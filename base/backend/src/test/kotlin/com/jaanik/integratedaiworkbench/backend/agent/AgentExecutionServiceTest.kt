package com.jaanik.integratedaiworkbench.backend.agent

import com.jaanik.integratedaiworkbench.backend.context.FileSystemContextService
import com.jaanik.integratedaiworkbench.backend.context.GroundingRequest
import com.jaanik.integratedaiworkbench.backend.model.InMemoryModelRegistry
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValue
import com.jaanik.integratedaiworkbench.backend.mcp.McpClient
import com.jaanik.integratedaiworkbench.backend.mcp.McpToolDefinition
import com.jaanik.integratedaiworkbench.backend.mcp.McpToolExecutionService
import com.jaanik.integratedaiworkbench.backend.mcp.McpToolResult
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.backend.prompt.PromptPreparationService
import com.jaanik.integratedaiworkbench.backend.runtime.FakeLocalRuntimeAdapter
import com.jaanik.integratedaiworkbench.backend.runtime.HardwareDetector
import com.jaanik.integratedaiworkbench.backend.runtime.HardwareSnapshot
import com.jaanik.integratedaiworkbench.backend.runtime.LifecycleRuntimeManager
import com.jaanik.integratedaiworkbench.backend.runtime.LocalInferenceService
import com.jaanik.integratedaiworkbench.backend.runtime.MemorySnapshot
import com.jaanik.integratedaiworkbench.backend.runtime.RuntimeCompatibilityMatcher
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import com.jaanik.integratedaiworkbench.shared.model.ToolCall
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionStatus
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentExecutionServiceTest {
    @Test fun `executes approved agent with bounded grounded prompt and structured result`() {
        val fixture = fixture(ModelResponse("{\"answer\":\"hello\"}"))
        val grounded = Files.writeString(fixture.contextRoot.resolve("context.md"), "api_key=context-secret")

        val outcome = runSuspend { fixture.service.execute(AgentExecutionRequest("agent", "password=user-secret", GroundingRequest(setOf(grounded.toString()), 1, 500))) }

        assertEquals(AgentExecutionStatus.SUCCESS, outcome.status)
        assertEquals(setOf("safe"), outcome.skillIds)
        assertEquals(listOf("context.md"), outcome.groundingReferences)
        assertTrue(fixture.adapter.lastRequest!!.prompt.contains("[REDACTED]"))
        assertFalse(fixture.adapter.lastRequest!!.prompt.contains("user-secret"))
        assertEquals("profile", outcome.metadata.modelProfileId)
        assertEquals(ModelResponseStatus.SUCCESS, outcome.metadata.outcome)
    }

    @Test fun `rejects missing agent before runtime and malformed structured response after inference`() {
        val fixture = fixture(ModelResponse("not JSON"))
        val missing = runSuspend { fixture.service.execute(AgentExecutionRequest("missing", "hello", GroundingRequest(emptySet(), 1, 100))) }
        assertEquals(AgentExecutionStatus.INVALID_CONFIGURATION, missing.status)
        assertEquals(null, fixture.adapter.lastRequest)

        val malformed = runSuspend { fixture.service.execute(AgentExecutionRequest("agent", "hello", GroundingRequest(emptySet(), 1, 100))) }
        assertEquals(AgentExecutionStatus.STRUCTURED_OUTPUT_INVALID, malformed.status)
        assertEquals(null, malformed.response)
    }

    @Test fun `rejects unauthorized model proposed tool without executing it`() {
        val fixture = fixture(ModelResponse("{\"answer\":\"hello\"}", toolCalls = listOf(ToolCall("shell"))))
        val outcome = runSuspend { fixture.service.execute(AgentExecutionRequest("agent", "hello", GroundingRequest(emptySet(), 1, 100))) }

        assertEquals(AgentExecutionStatus.UNAUTHORIZED_TOOL, outcome.status)
        assertEquals("Proposed tool operation is not authorized.", outcome.validationError)
    }

    @Test fun `maps runtime timeout and cancellation to typed outcomes`() {
        val timeout = fixture(ModelResponse("", status = ModelResponseStatus.TIMED_OUT))
        assertEquals(AgentExecutionStatus.TIMED_OUT, runSuspend { timeout.service.execute(AgentExecutionRequest("agent", "hello", GroundingRequest(emptySet(), 1, 100))) }.status)

        val cancellation = fixture(ModelResponse("", status = ModelResponseStatus.CANCELLED))
        assertEquals(AgentExecutionStatus.CANCELLED, runSuspend { cancellation.service.execute(AgentExecutionRequest("agent", "hello", GroundingRequest(emptySet(), 1, 100))) }.status)
    }

    @Test fun `continues existing agent execution after an authorized tool result`() {
        val first = ModelResponse("tool request", toolCalls = listOf(ToolCall("get_project_modules")))
        val final = ModelResponse("{\"answer\":\"The project module is integrated-ai-workbench\"}")
        val fixture = fixture(first, allowedTools = "get_project_modules", mcp = McpToolExecutionService({ ProjectModulesClient() }, expectedProjectModule = "integrated-ai-workbench"))
        fixture.adapter.enqueue(final)

        val outcome = runSuspend { fixture.service.execute(AgentExecutionRequest("agent", "Which module is open?", GroundingRequest(emptySet(), 1, 100), toolExecutionEnabled = true)) }

        assertEquals(AgentExecutionStatus.SUCCESS, outcome.status)
        assertEquals("{\"answer\":\"The project module is integrated-ai-workbench\"}", outcome.response?.content)
        assertEquals(listOf("get_project_modules"), outcome.toolExecutions.map { it.toolId })
        assertTrue(fixture.adapter.lastRequest!!.prompt.contains("Verified IDE tool result"))
        assertTrue(fixture.adapter.lastRequest!!.prompt.contains("integrated-ai-workbench"))
    }

    @Test fun `uses the selected agent output schema for the final response`() {
        val fixture = fixture(ModelResponse("{\"summary\":\"hello\"}"), profileSchema = null)

        val outcome = runSuspend {
            fixture.service.execute(
                AgentExecutionRequest(
                    "agent", "hello", GroundingRequest(emptySet(), 1, 100),
                    expectedOutputSchema = SUMMARY_SCHEMA,
                ),
            )
        }

        assertEquals(AgentExecutionStatus.SUCCESS, outcome.status)
        assertEquals(SUMMARY_SCHEMA, fixture.adapter.lastRequest!!.expectedSchema)
    }

    private fun fixture(
        response: ModelResponse,
        allowedTools: String? = null,
        mcp: McpToolExecutionService? = null,
        profileSchema: String? = SCHEMA,
    ): Fixture {
        val aiRoot = Files.createTempDirectory("agent-runtime")
        Files.createDirectories(aiRoot.resolve("skills/safe"))
        Files.writeString(aiRoot.resolve("skills/safe/SKILL.md"), "# Safe\n\n**Skill ID:** `safe`\n\nPolicy")
        Files.createDirectories(aiRoot.resolve("agents/agent"))
        Files.writeString(aiRoot.resolve("agents/agent/AGENT.md"), "# Agent\n\nPermitted: `safe`" + (allowedTools?.let { "\n\n**Permitted MCP tools:** `$it`" } ?: ""))
        val modelPath = Files.writeString(Files.createTempFile("agent-model", ".gguf"), "fixture").toRealPath()
        val validator = ModelPathValidator.fromSelections(listOf(modelPath), emptyList())
        val registry = InMemoryModelRegistry(validator)
        val model = registry.register(modelPath, modelId = "model")
        registry.associate(ModelProfile("profile", "Profile", "local", model.canonicalPath, timeout = Duration.ofSeconds(2), structuredOutputSchema = profileSchema), model.id)
        val adapter = FakeLocalRuntimeAdapter(
            runtimeCapabilities = RuntimeCapabilities(
                runtimeId = "fake",
                supportsLocalInference = true,
                supportsStructuredOutput = false,
                supportsThinkingLevel = false,
                supportedModelFormats = setOf("gguf"),
                supportedOperatingSystems = setOf("Windows"),
                supportedArchitectures = setOf("amd64"),
                supportedExecutionDevices = setOf(ExecutionDevice.CPU),
            ),
            generatedResponse = response,
        )
        val manager = LifecycleRuntimeManager(adapter)
        val inference = LocalInferenceService(registry, validator, RuntimeCompatibilityMatcher(), hardware(), manager)
        val contextRoot = Files.createTempDirectory("agent-context")
        val promptPreparation = PromptPreparationService(InputGuard(), FileSystemContextService(setOf(contextRoot)))
        return Fixture(AgentExecutionService(RuntimeMarkdownLoader(aiRoot).also { it.reload("profile") }, registry, promptPreparation, inference, mcpToolExecution = mcp), adapter, contextRoot)
    }

    private fun hardware(): HardwareDetector = object : HardwareDetector {
        override fun detect() = HardwareSnapshot("Windows", "11", "amd64", 8, MemorySnapshot(maxJvmHeapBytes = 1_000_000))
    }

    private data class Fixture(val service: AgentExecutionService, val adapter: FakeLocalRuntimeAdapter, val contextRoot: Path)

    private class ProjectModulesClient : McpClient {
        override fun discoverTools(): List<McpToolDefinition> = listOf(McpToolDefinition("get_project_modules", "Lists modules", objectSchema()))
        override fun call(toolId: String, arguments: Map<String, String>): McpToolResult = McpToolResult("integrated-ai-workbench", false)
        override fun close() = Unit
        private fun objectSchema() = JsonValue.ObjectValue(mapOf("type" to JsonValue.StringValue("object"), "properties" to JsonValue.ObjectValue(emptyMap())))
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        val outcome = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome.set(result) }
        })
        return outcome.get().getOrThrow()
    }

    private companion object {
        const val SCHEMA = "{\"type\":\"object\",\"required\":[\"answer\"],\"properties\":{\"answer\":{\"type\":\"string\"}},\"additionalProperties\":false}"
        const val SUMMARY_SCHEMA = "{\"type\":\"object\",\"required\":[\"summary\"],\"properties\":{\"summary\":{\"type\":\"string\"}},\"additionalProperties\":false}"
    }
}
