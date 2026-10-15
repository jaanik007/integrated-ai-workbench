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
import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
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

    fun runtimeAvailability(profileId: String?): WorkbenchRuntimeAvailability {
        val profile = profileId?.let(registry::find)
        if (!settings.managedLlamaCppEnabled) return WorkbenchRuntimeAvailability.disabled("Workbench-managed llama.cpp is disabled in AI Workbench settings.")
        if (profile == null) return WorkbenchRuntimeAvailability.unavailable("Select an accessible registered GGUF model first.")
        if (profile.providerId != MANAGED_LLAMA_CPP_PROVIDER) {
            return WorkbenchRuntimeAvailability.unavailable("The selected model is not registered for the Workbench-managed llama.cpp provider.")
        }
        val configurationPath = projectRoot.resolve(".aim/runtime/llama-server.properties")
        if (!Files.isRegularFile(configurationPath)) return WorkbenchRuntimeAvailability.unavailable("Workbench-managed llama.cpp configuration is unavailable.")
        val configuration = runCatching {
            loadLlamaServerConfiguration(
                configurationPath,
                Path.of(profile.modelReference),
                18080,
                profile.contextWindowTokens,
                settings.selectedProviderId,
            )
        }.getOrElse { return WorkbenchRuntimeAvailability.unavailable("The managed llama.cpp configuration is invalid.") }
        if (!Files.isRegularFile(configuration.executablePath) || !Files.isReadable(configuration.executablePath)) {
            return WorkbenchRuntimeAvailability.unavailable("Configured llama-server.exe is missing or unreadable.")
        }
        return managedProviderAvailability(settings.selectedProviderId, configuration)
    }

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
        registry.find(profileId)?.providerId != MANAGED_LLAMA_CPP_PROVIDER -> "The selected model does not use the Workbench-managed llama.cpp provider."
        settings.selectedProviderId !in MANAGED_PROVIDER_IDS -> "Select the Workbench-managed CPU or GPU provider before running a local model."
        selectedRuntimeUnavailableReason(profileId) != null -> selectedRuntimeUnavailableReason(profileId)
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
        val runtime = localInference(requireNotNull(registry.find(profileId)))
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

    /** Executes the selected registered GGUF directly, without requiring an MCP-enabled runtime agent. */
    suspend fun executeLocal(profileId: String, prompt: String, cancellation: InferenceCancellation): ModelResponse {
        check(settings.managedLlamaCppEnabled) { "Workbench-managed llama.cpp is disabled in AI Workbench settings." }
        val profile = requireNotNull(registry.find(profileId)) { "The selected model profile is unavailable." }
        check(profile.providerId == MANAGED_LLAMA_CPP_PROVIDER) {
            "The selected model does not use the Workbench-managed llama.cpp provider."
        }
        check(selectedRuntimeUnavailableReason(profile.id) == null) {
            selectedRuntimeUnavailableReason(profile.id)!!
        }
        return localInference(profile).execute(ModelRequest(profile.id, prompt, profile.timeout), cancellation)
    }

    private fun selectedRuntimeUnavailableReason(profileId: String): String? {
        val availability = runtimeAvailability(profileId)
        return when (settings.selectedProviderId) {
            MANAGED_CPU_PROVIDER -> availability.cpuReason.takeUnless { availability.cpuAvailable }
            MANAGED_GPU_PROVIDER -> availability.gpuReason.takeUnless { availability.gpuAvailable }
            else -> "Select the Workbench-managed CPU or GPU provider before running a local model."
        }
    }

    private fun localInference(profile: ModelProfile): LocalInferenceService {
        val validator = ModelPathValidator.fromSelections(registry.models().map { Path.of(it.canonicalPath) }, emptyList())
        return LocalInferenceService(
            registry, validator, RuntimeCompatibilityMatcher(), JvmHardwareDetector(),
            LifecycleRuntimeManager(LlamaServerAdapter(loadLlamaServerConfiguration(
                projectRoot.resolve(".aim/runtime/llama-server.properties"),
                Path.of(profile.modelReference), localPort(), profile.contextWindowTokens, settings.selectedProviderId,
            ))),
        )
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

data class WorkbenchRuntimeAvailability(
    val cpuAvailable: Boolean,
    val gpuAvailable: Boolean,
    val cpuReason: String,
    val gpuReason: String,
) {
    companion object {
        fun unavailable(reason: String) = WorkbenchRuntimeAvailability(false, false, reason, reason)
        fun disabled(reason: String) = unavailable(reason)
    }
}

/** Maps the persisted provider choice to the backend the configured binary can actually run. */
internal fun managedProviderAvailability(
    selectedProviderId: String,
    configuration: com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerConfiguration,
): WorkbenchRuntimeAvailability = when {
    selectedProviderId !in MANAGED_PROVIDER_IDS -> WorkbenchRuntimeAvailability.unavailable(
        "Select the Workbench-managed CPU or GPU provider before running a local model.",
    )
    configuration.cudaEnabled -> WorkbenchRuntimeAvailability(
        cpuAvailable = false,
        gpuAvailable = true,
        cpuReason = "Configured llama-server.exe is CUDA-enabled. Select the managed GPU provider or configure a CPU runtime.",
        gpuReason = "CUDA build configured; executable hash and runtime readiness are checked when execution starts.",
    )
    else -> WorkbenchRuntimeAvailability(
        cpuAvailable = true,
        gpuAvailable = false,
        cpuReason = "CPU build configured; executable hash and runtime readiness are checked when execution starts.",
        gpuReason = "No compatible GPU llama.cpp build is configured.",
    )
}

internal const val MANAGED_LLAMA_CPP_PROVIDER = "llama-cpp-server"
internal const val MANAGED_CPU_PROVIDER = "llama-cpp-cpu"
internal const val MANAGED_GPU_PROVIDER = "llama-cpp-gpu"
internal val MANAGED_PROVIDER_IDS = setOf(MANAGED_CPU_PROVIDER, MANAGED_GPU_PROVIDER)
