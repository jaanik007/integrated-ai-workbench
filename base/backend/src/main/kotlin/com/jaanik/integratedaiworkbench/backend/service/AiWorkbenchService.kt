package com.jaanik.integratedaiworkbench.backend.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jaanik.integratedaiworkbench.backend.agent.AgentExecutionRequest
import com.jaanik.integratedaiworkbench.backend.agent.AgentExecutionService
import com.jaanik.integratedaiworkbench.backend.agent.RuntimeMarkdownLoader
import com.jaanik.integratedaiworkbench.backend.context.FileSystemContextService
import com.jaanik.integratedaiworkbench.backend.mcp.IntelliJMcpStdioConfiguration
import com.jaanik.integratedaiworkbench.backend.mcp.McpToolExecutionService
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.backend.model.ModelRegistry
import com.jaanik.integratedaiworkbench.backend.model.PersistentModelRegistry
import com.jaanik.integratedaiworkbench.backend.prompt.PromptPreparationService
import com.jaanik.integratedaiworkbench.backend.runtime.InferenceCancellation
import com.jaanik.integratedaiworkbench.backend.runtime.JvmHardwareDetector
import com.jaanik.integratedaiworkbench.backend.runtime.LifecycleRuntimeManager
import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerAdapter
import com.jaanik.integratedaiworkbench.backend.runtime.LocalInferenceService
import com.jaanik.integratedaiworkbench.backend.runtime.RuntimeCompatibilityMatcher
import com.jaanik.integratedaiworkbench.backend.runtime.loadLlamaServerConfiguration
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionOutcome
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class AiWorkbenchService(
    private val project: Project,
) {
    private val projectRoot: Path = Path.of(requireNotNull(project.basePath) { "Project path is unavailable." }).toRealPath()
    private val registry: ModelRegistry get() = ApplicationManager.getApplication().getService(PersistentModelRegistry::class.java).registry()
    private val settings get() = ApplicationManager.getApplication().getService(WorkbenchSettingsService::class.java).snapshot()

    fun profiles(): List<ModelProfile> = registry.profiles()

    fun registerLocalModel(path: Path): ModelProfile =
        ApplicationManager.getApplication().getService(PersistentModelRegistry::class.java).registerLocalModel(path)

    fun agents(profileId: String): List<WorkbenchAgentChoice> = runCatching {
        RuntimeMarkdownLoader(projectRoot.resolve(".ai")).reload(profileId).agents.values
            .map { WorkbenchAgentChoice(it.definition.id, it.definition.displayName, it.definition.allowedToolIds) }
            .sortedBy(WorkbenchAgentChoice::displayName)
    }.getOrDefault(emptyList())

    fun unavailableReason(profileId: String?, toolExecutionEnabled: Boolean = false): String? = when {
        !settings.managedLlamaCppEnabled -> "Workbench-managed llama.cpp is disabled in AI Workbench settings."
        !settings.runtimeAgentsEnabled -> "Runtime agents are disabled in AI Workbench settings."
        profileId.isNullOrBlank() -> "No registered local model profile is selected."
        registry.find(profileId) == null -> "The selected model profile is unavailable."
        !Files.isRegularFile(projectRoot.resolve(".aim/runtime/llama-server.properties")) -> "Workbench-managed llama.cpp configuration is unavailable."
        toolExecutionEnabled && !settings.readOnlyMcpToolsEnabled -> "Permitted read-only IntelliJ MCP tools are disabled in AI Workbench settings."
        toolExecutionEnabled && !Files.isRegularFile(projectRoot.resolve(".aim/mcp/intellij-stdio.properties")) -> "IntelliJ MCP STDIO configuration is unavailable."
        else -> null
    }

    suspend fun execute(profileId: String, request: AgentExecutionRequest, cancellation: InferenceCancellation): AgentExecutionOutcome {
        check(unavailableReason(profileId, request.toolExecutionEnabled) == null) {
            unavailableReason(profileId, request.toolExecutionEnabled)!!
        }
        check(settings.projectGroundingEnabled || request.groundingRequest.requestedPaths.isEmpty()) {
            "Project grounding is disabled in AI Workbench settings."
        }
        val profile = requireNotNull(registry.find(profileId))
        val validator = ModelPathValidator.fromSelections(registry.models().map { Path.of(it.canonicalPath) }, emptyList())
        val runtime = LocalInferenceService(
            registry, validator, RuntimeCompatibilityMatcher(), JvmHardwareDetector(),
            LifecycleRuntimeManager(LlamaServerAdapter(loadLlamaServerConfiguration(
                projectRoot.resolve(".aim/runtime/llama-server.properties"), Path.of(profile.modelReference), localPort(),
            ))),
        )
        val loader = RuntimeMarkdownLoader(projectRoot.resolve(".ai")).also { it.reload(profileId) }
        return AgentExecutionService(
            markdownLoader = loader, modelRegistry = registry,
            promptPreparation = PromptPreparationService(InputGuard(), FileSystemContextService(setOf(projectRoot))),
            inference = runtime,
            mcpToolExecution = if (request.toolExecutionEnabled) {
                McpToolExecutionService.fromConfiguration(
                    IntelliJMcpStdioConfiguration.load(projectRoot.resolve(".aim/mcp/intellij-stdio.properties")),
                )
            } else null,
        ).execute(request.copy(expectedOutputSchema = outputSchemaFor(request.agentId)), cancellation)
    }

    private fun outputSchemaFor(agentId: String): String? = when (agentId) {
        "coding" -> CODING_RESPONSE_SCHEMA
        "judge" -> JUDGE_RESPONSE_SCHEMA
        else -> null
    }

    private fun localPort(): Int = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
}

private const val CODING_RESPONSE_SCHEMA = """{"type":"object","required":["summary","affectedFiles","proposedChanges","risks","verificationPlan"],"properties":{"summary":{"type":"string"},"affectedFiles":{"type":"string"},"proposedChanges":{"type":"string"},"risks":{"type":"string"},"verificationPlan":{"type":"string"}},"additionalProperties":false}"""
private const val JUDGE_RESPONSE_SCHEMA = """{"type":"object","required":["status","scores"],"properties":{"status":{"type":"string","enum":["PASS","FAIL"]},"scores":{"type":"object"}},"additionalProperties":false}"""

data class WorkbenchAgentChoice(val id: String, val displayName: String, val allowedToolIds: Set<String>) {
    override fun toString(): String = displayName
}
