package com.jaanik.integratedaiworkbench.backend.runtime

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class LlamaServerRealSmokeTest {
    @Test
    fun `starts checks health loads model and stops an explicitly configured local runtime`() {
        if (setting(ENABLED, ENV_ENABLED) != "true") return

        val configuration = LlamaServerConfiguration(
            executablePath = Path.of(requiredSetting(EXECUTABLE, ENV_EXECUTABLE)),
            modelPath = Path.of(requiredSetting(MODEL, ENV_MODEL)),
            port = availableLocalPort(),
            startupTimeout = Duration.ofSeconds(120),
            releaseTag = requiredSetting(RELEASE_TAG, ENV_RELEASE_TAG),
            officialArtifactName = requiredSetting(ARTIFACT, ENV_ARTIFACT),
            expectedExecutableSha256 = requiredSetting(EXECUTABLE_SHA256, ENV_EXECUTABLE_SHA256),
        )
        val manager = LifecycleRuntimeManager(LlamaServerAdapter(configuration))

        try {
            manager.start()
            assertEquals(RuntimeLifecycleState.READY, manager.status().state)
            assertEquals(200, healthStatus(configuration.port))

            manager.loadModel()
            assertEquals(RuntimeLifecycleState.MODEL_LOADED, manager.status().state)
            assertEquals(200, healthStatus(configuration.port))
        } finally {
            manager.shutdown()
            assertEquals(RuntimeLifecycleState.STOPPED, manager.status().state)
            assertFalse(healthStatus(configuration.port) in 200..299, "Managed runtime still responds after shutdown.")
        }
    }

    private fun availableLocalPort(): Int =
        ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }

    private fun healthStatus(port: Int): Int? = runCatching {
        val connection = URI("http://127.0.0.1:$port/health").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        connection.requestMethod = "GET"
        connection.responseCode
    }.getOrNull()

    private fun requiredSetting(propertyName: String, environmentName: String): String =
        assertNotNull(setting(propertyName, environmentName), "'$propertyName' or '$environmentName' is required.")

    private fun setting(propertyName: String, environmentName: String): String? =
        (System.getProperty(propertyName) ?: System.getenv(environmentName))?.takeIf(String::isNotBlank)

    private companion object {
        const val ENABLED = "iwb.realRuntimeSmoke"
        const val EXECUTABLE = "iwb.runtime.executable"
        const val MODEL = "iwb.runtime.model"
        const val RELEASE_TAG = "iwb.runtime.releaseTag"
        const val ARTIFACT = "iwb.runtime.artifact"
        const val EXECUTABLE_SHA256 = "iwb.runtime.executableSha256"
        const val ENV_ENABLED = "IWB_REAL_RUNTIME_SMOKE"
        const val ENV_EXECUTABLE = "IWB_RUNTIME_EXECUTABLE"
        const val ENV_MODEL = "IWB_RUNTIME_MODEL"
        const val ENV_RELEASE_TAG = "IWB_RUNTIME_RELEASE_TAG"
        const val ENV_ARTIFACT = "IWB_RUNTIME_ARTIFACT"
        const val ENV_EXECUTABLE_SHA256 = "IWB_RUNTIME_EXECUTABLE_SHA256"
    }
}
