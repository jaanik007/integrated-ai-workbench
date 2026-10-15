package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.AccelerationBackend
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlamaServerAdapterTest {
    @Test fun `builds localhost-only command with explicit local resources`() {
        val directory = Files.createTempDirectory("llama-server")
        val executable = Files.writeString(directory.resolve("llama-server.exe"), "fixture")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(executable, model, port = 19080, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = hash(executable), cudaEnabled = true, gpuLayers = 20))

        assertEquals(listOf(executable.toString(), "--host", "127.0.0.1", "--port", "19080", "--model", model.toString(), "--reasoning", "off", "--gpu-layers", "20", "--log-verbosity", "4"), adapter.command())
        assertTrue(ExecutionDevice.GPU in adapter.capabilities().supportedExecutionDevices)
        assertTrue(AccelerationBackend.CUDA in adapter.capabilities().supportedAccelerationBackends)
    }

    @Test fun `passes a configured profile context window to llama server`() {
        val directory = Files.createTempDirectory("llama-server")
        val executable = Files.writeString(directory.resolve("llama-server.exe"), "fixture")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(
            executable, model, releaseTag = "b11541", officialArtifactName = "fixture",
            expectedExecutableSha256 = hash(executable), contextWindowTokens = 8_192,
        ))

        assertTrue(adapter.command().windowed(2).any { it == listOf("--ctx-size", "8192") })
    }

    @Test fun `CPU command explicitly disables GPU layer offload`() {
        val directory = Files.createTempDirectory("llama-server")
        val executable = Files.writeString(directory.resolve("llama-server.exe"), "fixture")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val adapter = LlamaServerAdapter(LlamaServerConfiguration(executable, model, releaseTag = "b11541", officialArtifactName = "fixture", expectedExecutableSha256 = hash(executable)))

        assertTrue(adapter.command().windowed(2).any { it == listOf("--gpu-layers", "0") })
    }

    @Test fun `loads a scoped GPU configuration without using the CPU runtime`() {
        val directory = Files.createTempDirectory("llama-server")
        val cpu = Files.writeString(directory.resolve("cpu-llama-server.exe"), "cpu")
        val gpu = Files.writeString(directory.resolve("llama-server.exe"), "gpu")
        val model = Files.writeString(directory.resolve("model.gguf"), "fixture")
        val cpuPath = cpu.toString().replace('\\', '/')
        val gpuPath = gpu.toString().replace('\\', '/')
        val configuration = Files.writeString(directory.resolve("runtime.properties"), """
            executablePath=$cpuPath
            releaseTag=cpu
            officialArtifactName=CPU
            expectedExecutableSha256=${hash(cpu)}
            llama-cpp-gpu.executablePath=$gpuPath
            llama-cpp-gpu.releaseTag=gpu
            llama-cpp-gpu.officialArtifactName=CUDA
            llama-cpp-gpu.expectedExecutableSha256=${hash(gpu)}
            llama-cpp-gpu.cudaEnabled=true
            llama-cpp-gpu.gpuLayers=999
        """.trimIndent())

        val loaded = loadLlamaServerConfiguration(configuration, model, 19080, providerId = "llama-cpp-gpu")
        assertTrue(Files.isSameFile(gpu, loaded.executablePath))
        assertTrue(loaded.cudaEnabled)
        assertEquals(999, loaded.gpuLayers)
        assertFalse(Files.isSameFile(cpu, loaded.executablePath))
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
