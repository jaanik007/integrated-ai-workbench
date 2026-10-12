package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.AccelerationBackend
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LlamaServerAdapterTest {
    @Test fun `builds localhost-only command with explicit local resources`() {
        val directory = Files.createTempDirectory("llama-server")
        val executable = Files.writeString(directory.resolve("llama-server.exe"), "fixture")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(executable, model, port = 19080, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = hash(executable), cudaEnabled = true, gpuLayers = 20))

        assertEquals(listOf(executable.toString(), "--host", "127.0.0.1", "--port", "19080", "--model", model.toString(), "--reasoning", "off", "--gpu-layers", "20"), adapter.command())
        assertTrue(ExecutionDevice.GPU in adapter.capabilities().supportedExecutionDevices)
        assertTrue(AccelerationBackend.CUDA in adapter.capabilities().supportedAccelerationBackends)
    }

    @Test fun `rejects nonlocal unsupported or inconsistent configuration`() {
        val directory = Files.createTempDirectory("llama-server")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        assertFailsWith<IllegalArgumentException> { LlamaServerConfiguration(directory.resolve("other.exe"), model, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = "a".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { LlamaServerConfiguration(directory.resolve("llama-server.exe"), directory.resolve("model.bin"), releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = "a".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { LlamaServerConfiguration(directory.resolve("llama-server.exe"), model, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = "a".repeat(64), cudaEnabled = true) }
    }

    @Test fun `uses chat completions and schema only when requested`() {
        val directory = Files.createTempDirectory("llama-server")
        val executable = Files.writeString(directory.resolve("llama-server.exe"), "fixture")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(executable, model, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = hash(executable)))

        val request = com.jaanik.integratedaiworkbench.shared.model.ModelRequest("profile", "Return JSON.", java.time.Duration.ofSeconds(1), expectedSchema = """{"type":"object"}""")

        assertTrue(adapter.requestBody(request).contains("\"messages\""))
        assertTrue(adapter.requestBody(request).contains("\"response_format\""))
        assertTrue(!adapter.requestBody(request.copy(expectedSchema = null)).contains("\"response_format\""))
    }

    private fun hash(path: java.nio.file.Path): String = Files.newInputStream(path).use { stream ->
        MessageDigest.getInstance("SHA-256").digest(stream.readBytes()).joinToString("") { "%02x".format(it) }
    }
}
