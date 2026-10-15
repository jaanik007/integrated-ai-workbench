package com.jaanik.integratedaiworkbench.backend.model

import java.nio.file.Files
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersistentModelRegistryTest {
    @Test
    fun `converts model profiles to XML-friendly persistent data without losing execution settings`() {
        val profile = com.jaanik.integratedaiworkbench.shared.model.ModelProfile(
            id = "local-profile",
            displayName = "Local profile",
            providerId = "llama-cpp-server",
            modelReference = "C:/models/local.gguf",
            temperature = 0.7,
            topP = 0.8,
            maxOutputTokens = 512,
            contextWindowTokens = 4_096,
            timeout = Duration.ofSeconds(45),
            thinkingLevel = com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel.HIGH,
        )

        assertEquals(profile, PersistedModelProfile.from(profile).toModelProfile())
    }

    @Test
    fun `registers a selected GGUF once and rebuilds a usable local profile`() {
        val model = Files.writeString(Files.createTempFile("workbench-model", ".gguf"), "fixture")
        val registry = PersistentModelRegistry()

        val profile = registry.registerLocalModel(model)

        assertEquals("llama-cpp-server", profile.providerId)
        assertEquals(model.toRealPath().toString(), profile.modelReference)
        assertEquals(profile, registry.registry().find(profile.id))
        assertEquals(1, registry.registry().models().size)
        assertFailsWith<ModelRegistryException> { registry.registerLocalModel(model) }
    }

    @Test
    fun `retains an unavailable model entry and can reconnect it without deleting files`() {
        val original = Files.writeString(Files.createTempFile("workbench-model", ".gguf"), "fixture")
        val registry = PersistentModelRegistry()
        val profile = registry.registerLocalModel(original)
        val modelId = registry.registeredModels().single().modelId

        Files.delete(original)
        registry.loadState(registry.getState())

        assertEquals(1, registry.registeredModels().size)
        assertEquals(false, registry.registeredModels().single().isAccessible)
        assertEquals(null, registry.registry().find(profile.id))

        val replacement = Files.writeString(Files.createTempFile("workbench-model-reconnected", ".gguf"), "fixture")
        registry.reconnectLocalModel(modelId, replacement)

        assertEquals(replacement.toRealPath().toString(), registry.registry().find(profile.id)?.modelReference)
        assertEquals(true, registry.registeredModels().single().isAccessible)
    }

    @Test
    fun `recovers an editable profile when an older XML state restored it empty`() {
        val modelFile = Files.writeString(Files.createTempFile("workbench-model", ".gguf"), "fixture")
        val modelId = "saved-model"
        val profileId = "local-saved-profile"
        val registry = PersistentModelRegistry()

        registry.loadState(
            ModelRegistryState(
                models = mutableListOf(
                    PersistedModel(
                        id = modelId,
                        displayName = "Recovered GGUF",
                        canonicalPath = modelFile.toRealPath().toString(),
                        profileIds = mutableListOf(profileId),
                    ),
                ),
                profiles = mutableListOf(PersistedProfile(modelId, PersistedModelProfile())),
            ),
        )

        val registered = registry.registeredModels().single()
        assertEquals(profileId, registered.profile?.id)
        assertEquals(modelFile.toRealPath().toString(), registered.profile?.modelReference)
        assertEquals(registered.profile, registry.registry().find(profileId))
    }
}
