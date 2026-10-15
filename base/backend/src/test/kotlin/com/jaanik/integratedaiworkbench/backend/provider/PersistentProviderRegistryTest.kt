package com.jaanik.integratedaiworkbench.backend.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentProviderRegistryTest {

    @Test
    fun `test provider registry save and retrieve`() {
        val registry = PersistentProviderRegistry()
        val record = PersistedProviderRecord(
            id = "test-provider",
            displayName = "Test Provider",
            type = "Ollama",
            source = "MANUAL",
            endpoint = "http://127.0.0.1:11434",
            protocol = "Ollama API",
        )
        registry.saveProvider(record, "secret-api-key")

        val providers = registry.registeredProviders()
        assertEquals(1, providers.size)
        assertEquals("test-provider", providers[0].id)
        assertTrue(providers[0].hasApiKey)
    }
}
