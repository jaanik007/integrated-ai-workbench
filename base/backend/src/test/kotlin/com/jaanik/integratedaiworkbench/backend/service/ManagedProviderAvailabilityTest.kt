package com.jaanik.integratedaiworkbench.backend.service

import com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerConfiguration
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManagedProviderAvailabilityTest {
    @Test
    fun `CPU selection is available only for a configured CPU runtime`() {
        val availability = managedProviderAvailability(MANAGED_CPU_PROVIDER, configuration(cudaEnabled = false))

        assertTrue(availability.cpuAvailable)
        assertFalse(availability.gpuAvailable)
    }

    @Test
    fun `GPU selection is available only for a configured CUDA runtime`() {
        val availability = managedProviderAvailability(MANAGED_GPU_PROVIDER, configuration(cudaEnabled = true))

        assertTrue(availability.gpuAvailable)
        assertFalse(availability.cpuAvailable)
    }

    @Test
    fun `external provider selection cannot route a local GGUF through llama cpp`() {
        val availability = managedProviderAvailability("external-provider", configuration(cudaEnabled = false))

        assertFalse(availability.cpuAvailable)
        assertTrue(availability.cpuReason.contains("CPU or GPU provider"))
    }

    private fun configuration(cudaEnabled: Boolean): LlamaServerConfiguration {
        val directory = Files.createTempDirectory("managed-provider")
        return LlamaServerConfiguration(
            executablePath = Files.writeString(directory.resolve("llama-server.exe"), "fixture"),
            modelPath = Files.writeString(directory.resolve("model.gguf"), "fixture"),
            releaseTag = "fixture",
            officialArtifactName = "fixture",
            expectedExecutableSha256 = "a".repeat(64),
            cudaEnabled = cudaEnabled,
            gpuLayers = if (cudaEnabled) 20 else null,
        )
    }
}
