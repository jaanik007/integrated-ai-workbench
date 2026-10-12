package com.jaanik.integratedaiworkbench.shared.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Application-level UI preferences. This service deliberately stores paths and
 * feature preferences only; model files, runtime binaries, and secrets stay out
 * of the IDE settings file.
 */
@State(
    name = "IntegratedAiWorkbenchSettings",
    storages = [Storage("integrated-ai-workbench.xml")],
)
@Service(Service.Level.APP)
class WorkbenchSettingsService : PersistentStateComponent<WorkbenchSettingsState> {
    private var state = WorkbenchSettingsState()

    @Synchronized
    fun snapshot(): WorkbenchSettingsState = state.copy()

    /** Replaces the complete, already validated preference snapshot atomically. */
    @Synchronized
    fun update(candidate: WorkbenchSettingsState) {
        validate(candidate)
        state = candidate.copy(version = CURRENT_VERSION)
    }

    override fun getState(): WorkbenchSettingsState = snapshot()

    override fun loadState(state: WorkbenchSettingsState) {
        val migrated = state.copy(
            version = CURRENT_VERSION,
            lmStudioEndpoint = state.lmStudioEndpoint.ifBlank { DEFAULT_LM_STUDIO_ENDPOINT },
        )
        this.state = if (runCatching { validate(migrated) }.isSuccess) {
            migrated
        } else {
            WorkbenchSettingsState()
        }
    }

    private fun validate(candidate: WorkbenchSettingsState) {
        if (candidate.defaultModelDirectory.isNotBlank()) {
            val directory = Path.of(candidate.defaultModelDirectory)
            require(directory.isAbsolute && Files.isDirectory(directory) && Files.isReadable(directory)) {
                "The default model directory must be an existing readable absolute directory."
            }
        }

        val endpoint = URI(candidate.lmStudioEndpoint)
        require(endpoint.scheme in setOf("http", "https") && !endpoint.host.isNullOrBlank()) {
            "LM Studio endpoint must be an absolute HTTP(S) URL."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val DEFAULT_LM_STUDIO_ENDPOINT = "http://127.0.0.1:1234"
    }
}

data class WorkbenchSettingsState(
    var version: Int = WorkbenchSettingsService.CURRENT_VERSION,
    var defaultModelDirectory: String = "",
    var defaultModelProfileId: String = "",
    var lmStudioEndpoint: String = WorkbenchSettingsService.DEFAULT_LM_STUDIO_ENDPOINT,
    var managedLlamaCppEnabled: Boolean = true,
    var runtimeAgentsEnabled: Boolean = true,
    var projectGroundingEnabled: Boolean = true,
    var readOnlyMcpToolsEnabled: Boolean = false,
    var showExecutionPanel: Boolean = true,
)
