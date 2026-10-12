package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.backend.model.InMemoryModelRegistry
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LlamaServerRealInferenceTest {
    @Test
    fun `generates a typed response through the configured managed local runtime`() {
        if (setting(ENABLED, ENV_ENABLED) != "true") return

        val modelPath = Path.of(requiredSetting(MODEL, ENV_MODEL)).toRealPath()
        val validator = ModelPathValidator.fromSelections(listOf(modelPath), emptyList())
        val registry = InMemoryModelRegistry(validator)
        val model = registry.register(modelPath, modelId = "real-qwen3")
        val profile = ModelProfile(
            id = "real-qwen3-profile",
            displayName = "Qwen3 local CPU",
            providerId = "llama-cpp-server",
            modelReference = model.canonicalPath,
            temperature = 0.0,
            topP = 1.0,
            maxOutputTokens = 16,
            timeout = Duration.ofSeconds(60),
        )
        registry.associate(profile, model.id)
        val manager = LifecycleRuntimeManager(LlamaServerAdapter(LlamaServerConfiguration(
            executablePath = Path.of(requiredSetting(EXECUTABLE, ENV_EXECUTABLE)),
            modelPath = modelPath,
            port = availableLocalPort(),
            startupTimeout = Duration.ofSeconds(120),
            releaseTag = requiredSetting(RELEASE_TAG, ENV_RELEASE_TAG),
            officialArtifactName = requiredSetting(ARTIFACT, ENV_ARTIFACT),
            expectedExecutableSha256 = requiredSetting(EXECUTABLE_SHA256, ENV_EXECUTABLE_SHA256),
        )))
        val service = LocalInferenceService(registry, validator, RuntimeCompatibilityMatcher(), JvmHardwareDetector(), manager)

        val response = runSuspend {
            service.execute(ModelRequest(profile.id, "Reply with a short greeting for the Workbench.", Duration.ofSeconds(60), temperature = 0.0, topP = 1.0, maxOutputTokens = 16))
        }

        assertEquals(ModelResponseStatus.SUCCESS, response.status)
        assertTrue(response.content.isNotBlank())
        assertEquals(profile.id, response.metadata["profileId"])
        assertEquals("llama-cpp-server", response.executionMetadata!!.runtimeId)
        assertFalse(response.metadata.values.any { it.contains(modelPath.toString(), ignoreCase = true) })
        assertEquals(RuntimeLifecycleState.STOPPED, manager.status().state)
    }

    private fun availableLocalPort(): Int =
        ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }

    private fun requiredSetting(propertyName: String, environmentName: String): String =
        assertNotNull(setting(propertyName, environmentName), "'$propertyName' or '$environmentName' is required.")

    private fun setting(propertyName: String, environmentName: String): String? =
        (System.getProperty(propertyName) ?: System.getenv(environmentName))?.takeIf(String::isNotBlank)

    private fun <T> runSuspend(block: suspend () -> T): T {
        val outcome = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome.set(result) }
        })
        return outcome.get().getOrThrow()
    }

    private companion object {
        const val ENABLED = "iwb.realInference"
        const val EXECUTABLE = "iwb.runtime.executable"
        const val MODEL = "iwb.runtime.model"
        const val RELEASE_TAG = "iwb.runtime.releaseTag"
        const val ARTIFACT = "iwb.runtime.artifact"
        const val EXECUTABLE_SHA256 = "iwb.runtime.executableSha256"
        const val ENV_ENABLED = "IWB_REAL_INFERENCE"
        const val ENV_EXECUTABLE = "IWB_RUNTIME_EXECUTABLE"
        const val ENV_MODEL = "IWB_RUNTIME_MODEL"
        const val ENV_RELEASE_TAG = "IWB_RUNTIME_RELEASE_TAG"
        const val ENV_ARTIFACT = "IWB_RUNTIME_ARTIFACT"
        const val ENV_EXECUTABLE_SHA256 = "IWB_RUNTIME_EXECUTABLE_SHA256"
    }
}
