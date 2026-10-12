package com.jaanik.integratedaiworkbench.backend.agent

import com.jaanik.integratedaiworkbench.backend.context.FileSystemContextService
import com.jaanik.integratedaiworkbench.backend.context.GroundingRequest
import com.jaanik.integratedaiworkbench.backend.mcp.IntelliJMcpStdioConfiguration
import com.jaanik.integratedaiworkbench.backend.mcp.McpToolExecutionService
import com.jaanik.integratedaiworkbench.backend.model.InMemoryModelRegistry
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.backend.prompt.PromptPreparationService
import com.jaanik.integratedaiworkbench.backend.runtime.JvmHardwareDetector
import com.jaanik.integratedaiworkbench.backend.runtime.LifecycleRuntimeManager
import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerAdapter
import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerConfiguration
import com.jaanik.integratedaiworkbench.backend.runtime.LocalInferenceService
import com.jaanik.integratedaiworkbench.backend.runtime.RuntimeCompatibilityMatcher
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionStatus
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Opt-in proof of the actual Workbench local-model -> IntelliJ MCP -> local-model cycle. */
class AgentMcpRealIntegrationTest {
    @Test fun `continues managed local inference after an IntelliJ project-module call`() {
        if (setting("iwb.realMcp", "IWB_REAL_MCP") != "true") return
        val executable = Path.of(required("iwb.runtime.executable", "IWB_RUNTIME_EXECUTABLE"))
        val modelPath = Path.of(required("iwb.runtime.model", "IWB_RUNTIME_MODEL")).toRealPath()
        val validator = ModelPathValidator.fromSelections(listOf(modelPath), emptyList())
        val registry = InMemoryModelRegistry(validator)
        val model = registry.register(modelPath, modelId = "real-mcp-model")
        val profile = ModelProfile(
            id = "real-mcp-profile", displayName = "Real MCP profile", providerId = "llama-cpp-server",
            modelReference = model.canonicalPath, timeout = Duration.ofSeconds(180), temperature = 0.0,
            topP = 1.0, maxOutputTokens = 640, structuredOutputSchema = FINAL_SCHEMA,
        )
        registry.associate(profile, model.id)
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(
            executablePath = executable, modelPath = modelPath, port = localPort(), startupTimeout = Duration.ofSeconds(120),
            releaseTag = required("iwb.runtime.releaseTag", "IWB_RUNTIME_RELEASE_TAG"),
            officialArtifactName = required("iwb.runtime.artifact", "IWB_RUNTIME_ARTIFACT"),
            expectedExecutableSha256 = required("iwb.runtime.executableSha256", "IWB_RUNTIME_EXECUTABLE_SHA256"),
        ))
        val inference = LocalInferenceService(registry, validator, RuntimeCompatibilityMatcher(), JvmHardwareDetector(), LifecycleRuntimeManager(adapter))
        val projectRoot = Path.of(System.getProperty("user.dir")).toRealPath()
        val agent = AgentExecutionService(
            RuntimeMarkdownLoader(projectRoot.resolve(".ai")).also { it.reload(profile.id) }, registry,
            PromptPreparationService(InputGuard(), FileSystemContextService(setOf(projectRoot))), inference,
            mcpToolExecution = McpToolExecutionService.fromConfiguration(
                IntelliJMcpStdioConfiguration.load(projectRoot.resolve(".aim/mcp/intellij-stdio.properties")),
            ),
        )

        val outcome = runSuspend {
            agent.execute(AgentExecutionRequest(
                agentId = "coding",
                userInput = "Use get_project_modules exactly once to identify the open project module. First return only the prescribed tool-call JSON: {\"tool_calls\":[{\"toolId\":\"get_project_modules\",\"arguments\":{}}]}. After receiving the result, return exactly one JSON object with the string keys summary, affectedFiles, proposedChanges, risks, and verificationPlan; mention the module in summary. Do not add Markdown or any other keys.",
                groundingRequest = GroundingRequest(emptySet(), 1, 1_000), toolExecutionEnabled = true,
            ))
        }

        assertTrue(
            outcome.toolExecutions.any { it.toolId == "get_project_modules" && it.completed },
            "Expected a completed IntelliJ project-module call; status=${outcome.status}, validation=${outcome.validationError}",
        )
        assertEquals(AgentExecutionStatus.SUCCESS, outcome.status, outcome.validationError)
        assertTrue(outcome.response!!.content.contains("integrated-ai-workbench", ignoreCase = true))
    }

    private fun localPort(): Int = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
    private fun required(property: String, environment: String): String = setting(property, environment)
        ?: error("'$property' or '$environment' is required for real MCP integration.")
    private fun setting(property: String, environment: String): String? =
        (System.getProperty(property) ?: System.getenv(environment))?.takeIf(String::isNotBlank)
    private fun <T> runSuspend(block: suspend () -> T): T {
        val result = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(outcome: Result<T>) { result.set(outcome) }
        })
        return result.get().getOrThrow()
    }

    private companion object {
        const val FINAL_SCHEMA = """{"type":"object","required":["summary","affectedFiles","proposedChanges","risks","verificationPlan"],"properties":{"summary":{"type":"string"},"affectedFiles":{"type":"string"},"proposedChanges":{"type":"string"},"risks":{"type":"string"},"verificationPlan":{"type":"string"}},"additionalProperties":false}"""
    }
}
