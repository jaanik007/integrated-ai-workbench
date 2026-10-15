package com.jaanik.integratedaiworkbench.shared.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-level UI preferences. Model registrations and presets remain in
 * [PersistentModelRegistry][com.jaanik.integratedaiworkbench.backend.model.PersistentModelRegistry];
 * runtime binaries and secrets stay out of the IDE settings file.
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
        // Version 1 exposed a default directory and an LM Studio endpoint. Keep
        // their serialized fields for a non-destructive migration, but the UI
        // now uses the per-file model registry and managed llama.cpp provider.
        this.state = state.copy(
            version = CURRENT_VERSION,
            selectedProviderId = state.selectedProviderId.ifBlank { DEFAULT_PROVIDER_ID },
        )
    }

    private fun validate(candidate: WorkbenchSettingsState) {
        require(candidate.selectedProviderId.isNotBlank()) { "An inference provider id is required." }
    }

    companion object {
        const val CURRENT_VERSION = 2
        const val DEFAULT_PROVIDER_ID = "llama-cpp-cpu"
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
    var selectedProviderId: String = WorkbenchSettingsService.DEFAULT_PROVIDER_ID,
)
