package com.jaanik.integratedaiworkbench.backend.model

import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ModelRegistryTest {
    @Test
    fun `registers approved files and preserves user file on removal`() {
        val directory = Files.createTempDirectory("registry")
        val modelPath = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val registry = InMemoryModelRegistry(ModelPathValidator.fromSelections(listOf(modelPath), emptyList()))

        val model = registry.register(modelPath, "Fixture", "model-1")
        registry.remove(model.id)

        assertTrue(Files.exists(modelPath))
        assertEquals(emptyList(), registry.models())
    }

    @Test
    fun `rejects duplicate ids unauthorized paths and unsupported types`() {
        val approvedDirectory = Files.createTempDirectory("approved")
        val approved = Files.writeString(approvedDirectory.resolve("model.gguf"), "fixture")
        val unapproved = Files.writeString(Files.createTempDirectory("outside").resolve("model.gguf"), "fixture")
        val textFile = Files.writeString(approvedDirectory.resolve("notes.txt"), "fixture")
        val registry = InMemoryModelRegistry(ModelPathValidator.fromSelections(listOf(approved), emptyList()))

        registry.register(approved, modelId = "model-1")
        assertFailsWith<IllegalStateException> { registry.register(approved, modelId = "model-1") }
        assertFailsWith<ModelRegistryException> { registry.register(unapproved) }
        assertFailsWith<ModelRegistryException> { registry.register(textFile) }
    }

    @Test
    fun `associations require canonical profile references and prevent removal`() {
        val directory = Files.createTempDirectory("profiles")
        val modelPath = Files.writeString(directory.resolve("model.gguf"), "fixture").toRealPath()
        val registry = InMemoryModelRegistry(ModelPathValidator.fromSelections(listOf(modelPath), emptyList()))
        val model = registry.register(modelPath, modelId = "model-1")
        val profile = ModelProfile("profile-1", "Profile", "local", model.canonicalPath)

        registry.associate(profile, model.id)

        assertEquals(profile, registry.find("profile-1"))
        assertFailsWith<ModelRegistryException> { registry.remove(model.id) }
    }

    @Test
    fun `discovers only approved directory files within bounds`() {
        val directory = Files.createTempDirectory("discovery")
        Files.writeString(directory.resolve("first.gguf"), "fixture")
        Files.createDirectories(directory.resolve("nested"))
        Files.writeString(directory.resolve("nested/second.onnx"), "fixture")
        Files.writeString(directory.resolve("notes.txt"), "fixture")
        val registry = InMemoryModelRegistry(ModelPathValidator.fromSelections(emptyList(), listOf(directory)))

        assertEquals(1, registry.discover(directory, maxDepth = 1).size)
        assertEquals(1, registry.discover(directory, maxDepth = 2, maxResults = 1).size)
        assertFailsWith<ModelRegistryException> { registry.discover(Path.of(System.getProperty("user.dir"))) }
    }

    @Test
    fun `restores only valid persisted entries and profile associations`() {
        val directory = Files.createTempDirectory("persistent")
        val modelPath = Files.writeString(directory.resolve("model.gguf"), "fixture").toRealPath()
        val profile = ModelProfile("profile-1", "Profile", "local", modelPath.toString())
        val component = PersistentModelRegistry()

        component.loadState(ModelRegistryState(
            mutableListOf(PersistedModel("model-1", "Model", modelPath.toString(), mutableListOf("profile-1"))),
            mutableListOf(PersistedProfile("model-1", PersistedModelProfile.from(profile))),
        ))

        assertEquals("model-1", component.registry().findModel("model-1")?.id)
        assertEquals(profile, component.registry().find("profile-1"))
        assertEquals("model-1", component.getState().models.single().id)
    }

    @Test
    fun `symbolic link escapes are rejected when supported by filesystem`() {
        val approved = Files.createTempDirectory("approved-link")
        val outside = Files.writeString(Files.createTempFile("outside-link", ".gguf"), "fixture")
        val link = approved.resolve("escape.gguf")
        val created = runCatching { Files.createSymbolicLink(link, outside); true }.getOrDefault(false)
        if (!created) return

        val registry = InMemoryModelRegistry(ModelPathValidator.fromSelections(emptyList(), listOf(approved)))
        assertFailsWith<ModelRegistryException> { registry.register(link) }
    }
}
