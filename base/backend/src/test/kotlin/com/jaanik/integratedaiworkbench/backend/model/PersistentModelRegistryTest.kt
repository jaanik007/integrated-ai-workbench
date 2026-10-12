package com.jaanik.integratedaiworkbench.backend.model

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersistentModelRegistryTest {
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
}
