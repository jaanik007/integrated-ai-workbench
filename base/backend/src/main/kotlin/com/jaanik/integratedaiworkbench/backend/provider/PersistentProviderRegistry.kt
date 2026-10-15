package com.jaanik.integratedaiworkbench.backend.provider

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.net.URI

data class PersistedProviderRecord(
    var id: String = "",
    var displayName: String = "",
    var type: String = "",
    var source: String = "MANUAL",
    var endpoint: String = "",
    var protocol: String = "",
    var hasApiKey: Boolean = false,
)

data class ProviderRegistryState(
    var providers: MutableList<PersistedProviderRecord> = mutableListOf(),
)

/**
 * Stores connection metadata in IDE configuration and credentials in IntelliJ PasswordSafe.
 * No plaintext secret fallback: a PasswordSafe failure is surfaced to the caller.
 *
 * Credential update contract:
 *   apiKey == null, clearApiKey == false -> retain existing key
 *   apiKey != null                    -> replace the existing key
 *   clearApiKey == true               -> remove the existing key
 *
 * Call blocking methods on a worker thread, not Swing's EDT.
 */
@State(name = "IntegratedAiWorkbenchProviderRegistry", storages = [Storage("integrated-ai-workbench-providers.xml")])
@Service(Service.Level.APP)
class PersistentProviderRegistry : PersistentStateComponent<ProviderRegistryState> {
    private var state = ProviderRegistryState()

    @Synchronized
    fun registeredProviders(): List<PersistedProviderRecord> = state.providers.map { it.copy() }

    @Synchronized
    fun saveProvider(record: PersistedProviderRecord, apiKey: String?, clearApiKey: Boolean = false) {
        validateRecord(record)
        require(!(clearApiKey && apiKey != null)) { "Cannot replace and remove the API key simultaneously." }
        require(apiKey == null || apiKey.isNotBlank()) { "A replacement API key must not be blank." }

        val existing = state.providers.firstOrNull { it.id == record.id }
        val endpoint = record.endpoint.trim().trimEnd('/')
        val attributes = credentialAttributes(record.id)
        // Also reconcile older records whose hasApiKey flag was incorrectly cleared
        // by the previous implementation while the PasswordSafe entry remained.
        val savedKeyExists = PasswordSafe.instance.getPassword(attributes) != null
        if (savedKeyExists && existing != null && existing.endpoint.trim().trimEnd('/') != endpoint &&
            apiKey == null && !clearApiKey
        ) {
            error("Endpoint changed: replace or remove the previously stored API key explicitly.")
        }

        // Secrets must never be written to a remote, unencrypted HTTP endpoint.
        if (apiKey != null) {
            val uri = URI(endpoint)
            require(uri.scheme.equals("https", true) || isLoopback(uri.host.orEmpty())) {
                "Cannot save an API key for a remote HTTP endpoint; use HTTPS."
            }
        }

        val hasKey = when {
            clearApiKey -> {
                PasswordSafe.instance[attributes] = null
                false
            }
            apiKey != null -> {
                PasswordSafe.instance[attributes] = Credentials(record.id, apiKey)
                true
            }
            else -> savedKeyExists
        }

        // Do not mutate metadata until PasswordSafe successfully stores/deletes the key.
        val updated = record.copy(endpoint = endpoint, hasApiKey = hasKey)
        val index = state.providers.indexOfFirst { it.id == updated.id }
        if (index >= 0) state.providers[index] = updated else state.providers.add(updated)
    }

    @Synchronized
    fun removeProvider(id: String) {
        if (id.isBlank()) return
        // If deleting the key fails, preserve the record so the user can retry.
        PasswordSafe.instance[credentialAttributes(id)] = null
        state.providers.removeIf { it.id == id }
    }

    /** For authentication and connection probes only. Never bind its return value to a UI password field. */
    @Synchronized
    fun getApiKey(id: String): String? = PasswordSafe.instance.getPassword(credentialAttributes(id))

    @Synchronized
    override fun getState(): ProviderRegistryState = ProviderRegistryState(
        state.providers.map { it.copy() }.toMutableList()
    )

    @Synchronized
    override fun loadState(state: ProviderRegistryState) {
        val deduplicated = linkedMapOf<String, PersistedProviderRecord>()
        state.providers.forEach { record ->
            if (record.id.isNotBlank() && record.displayName.isNotBlank() && record.endpoint.isNotBlank()) {
                deduplicated[record.id] = record.copy()
            }
        }
        this.state = ProviderRegistryState(deduplicated.values.toMutableList())
    }

    private fun validateRecord(record: PersistedProviderRecord) {
        require(record.id.isNotBlank()) { "Provider ID is required." }
        require(record.displayName.isNotBlank()) { "Provider display name is required." }
        require(record.protocol.isNotBlank()) { "Provider protocol is required." }
        val uri = runCatching { URI(record.endpoint.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("Provider endpoint must be a valid URL.")
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "Provider endpoint must use HTTP or HTTPS."
        }
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Provider endpoint needs a hostname and must not contain credentials, a query or a fragment."
        }
    }

    private fun credentialAttributes(id: String): CredentialAttributes = CredentialAttributes(
        generateServiceName("Integrated AI Workbench", id)
    )

    private fun isLoopback(host: String): Boolean = host.equals("localhost", true) ||
            host == "127.0.0.1" || host == "::1" || host == "[::1]"
}
