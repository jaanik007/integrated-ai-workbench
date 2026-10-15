package com.jaanik.integratedaiworkbench.frontend.tabs

import com.jaanik.integratedaiworkbench.backend.provider.PersistedProviderRecord
import com.jaanik.integratedaiworkbench.backend.provider.ProviderConnectionStatus
import com.jaanik.integratedaiworkbench.backend.provider.ProviderDiscoveryService
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile

/** No fictitious providers. Only verified catalog endpoints and usable native runtimes are published. */
internal object VerifiedExecutionSources {
    data class Provider(val record: PersistedProviderRecord, val models: List<String>, val origin: String) {
        override fun toString(): String = "${record.displayName} · ${models.size} models"
    }

    data class Snapshot(
        val localModels: List<ModelProfile>,
        val providers: List<Provider>,
        val diagnostics: List<String>,
        val unavailableLocalReasons: Map<String, String> = emptyMap(),
    )

    fun scan(context: WorkbenchFrontendContext): Snapshot {
        val diagnostics = mutableListOf<String>()
        val unavailableLocalReasons = linkedMapOf<String, String>()
        val local = context.workbench.profiles().filter { profile ->
            runCatching {
                val availability = context.workbench.runtimeAvailability(profile.id)
                val gpu = context.settings.snapshot().selectedProviderId == "llama-cpp-gpu"
                val available = if (gpu) availability.gpuAvailable else availability.cpuAvailable
                if (!available) unavailableLocalReasons[profile.id] = if (gpu) availability.gpuReason else availability.cpuReason
                true
            }.onFailure {
                unavailableLocalReasons[profile.id] = it.message ?: "unavailable"
            }.getOrDefault(true)
        }
        val providers = linkedMapOf<String, Provider>()
        val persisted = context.providerRegistry.registeredProviders().filter { it.source != "BUILT_IN" }
        val candidates = persisted.map { it to "Saved" } + listOf(
            PersistedProviderRecord(
                id = "auto-ollama-local",
                displayName = "Ollama (local)",
                type = "Ollama",
                source = "DISCOVERED",
                endpoint = "http://127.0.0.1:11434",
                protocol = "Ollama API"
            ) to "Discovered",
            PersistedProviderRecord(
                id = "auto-openai-local-1234",
                displayName = "Local OpenAI-compatible API (:1234)",
                type = "OpenAI-compatible",
                source = "DISCOVERED",
                endpoint = "http://127.0.0.1:1234",
                protocol = "OpenAI Chat Completions /v1"
            ) to "Discovered"
        )
        for ((record, origin) in candidates) {
            val key = record.endpoint.trimEnd('/').lowercase() + "|" + (if (record.protocol.contains(
                    "Ollama",
                    true
                )
            ) "ollama" else "openai")
            if (key in providers) continue
            val probe = runCatching {
                val secret = if (origin == "Saved") context.providerRegistry.getApiKey(record.id) else null
                ProviderDiscoveryService.probe(record.endpoint, record.protocol, secret)
            }.onFailure { diagnostics += "${record.displayName}: ${it.message ?: "probe failed"}" }.getOrNull()
            if (probe?.status == ProviderConnectionStatus.AVAILABLE && probe.models.isNotEmpty()) {
                providers[key] = Provider(record, probe.models.distinct(), origin)
            } else if (origin == "Saved") {
                diagnostics += "${record.displayName}: ${probe?.errorMessage ?: "No usable model catalog"}"
            }
        }
        unavailableLocalReasons.values.forEach { reason -> diagnostics += "Local runtime: $reason" }
        return Snapshot(local, providers.values.toList(), diagnostics, unavailableLocalReasons)
    }
}
