package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.backend.model.InMemoryModelRegistry
import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalInferenceServiceTest {
    @Test
    fun `resolves registered model maps generation settings and releases runtime`() {
        val fixture = fixture()
        val response = runSuspend { fixture.service.execute(ModelRequest("profile", "Say hello", Duration.ofSeconds(5))) }

        assertEquals(ModelResponseStatus.SUCCESS, response.status)
        assertEquals("fake response", response.content)
        assertEquals(0.3, fixture.adapter.lastRequest!!.temperature)
        assertEquals(0.8, fixture.adapter.lastRequest!!.topP)
        assertEquals(12, fixture.adapter.lastRequest!!.maxOutputTokens)
        assertEquals("profile", response.metadata["profileId"])
        assertEquals("fake", response.executionMetadata!!.runtimeId)
        assertEquals(RuntimeLifecycleState.STOPPED, fixture.manager.status().state)
    }

    @Test
    fun `rejects unsupported native structured output before startup`() {
        val fixture = fixture(requiresNativeStructuredOutput = true)
        val response = runSuspend { fixture.service.execute(ModelRequest("profile", "Return JSON", Duration.ofSeconds(5))) }

        assertEquals(ModelResponseStatus.FAILED, response.status)
        assertEquals("RUNTIME_INCOMPATIBLE", response.metadata["error"])
        assertEquals(RuntimeLifecycleState.NOT_STARTED, fixture.manager.status().state)
        assertEquals(null, fixture.adapter.lastRequest)
    }

    @Test
    fun `maps timeout and generation failure to typed safe outcomes`() {
        val timedOut = fixture(response = ModelResponse("", status = ModelResponseStatus.TIMED_OUT))
        val timeoutResponse = runSuspend { timedOut.service.execute(ModelRequest("profile", "Wait", Duration.ofSeconds(1))) }
        assertEquals(ModelResponseStatus.TIMED_OUT, timeoutResponse.status)
        assertEquals(RuntimeLifecycleState.STOPPED, timedOut.manager.status().state)

        val failed = fixture().also { it.adapter.failGenerate = true }
        val failedResponse = runSuspend { failed.service.execute(ModelRequest("profile", "Fail", Duration.ofSeconds(1))) }
        assertEquals(ModelResponseStatus.FAILED, failedResponse.status)
        assertEquals("LOCAL_RUNTIME_FAILURE", failedResponse.metadata["error"])
        assertEquals(RuntimeLifecycleState.STOPPED, failed.manager.status().state)
    }

    @Test
    fun `cancellation terminates the managed runtime and prevents a stale success`() {
        val fixture = fixture().also { it.adapter.generationDelay = Duration.ofMillis(200) }
        val cancellation = InferenceCancellation()
        val outcome = AtomicReference<ModelResponse>()
        val worker = thread(start = true) {
            outcome.set(runSuspend { fixture.service.execute(ModelRequest("profile", "Long request", Duration.ofSeconds(5)), cancellation) })
        }
        val deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos()
        while (fixture.adapter.lastRequest == null && System.nanoTime() < deadline) Thread.sleep(10)
        assertNotNull(fixture.adapter.lastRequest)
        cancellation.cancel()
        worker.join(2_000)

        assertFalse(worker.isAlive)
        assertEquals(ModelResponseStatus.CANCELLED, outcome.get().status)
        assertEquals(RuntimeLifecycleState.STOPPED, fixture.manager.status().state)
    }

    private fun fixture(
        requiresNativeStructuredOutput: Boolean = false,
        response: ModelResponse = ModelResponse("fake response"),
    ): Fixture {
        val modelPath = Files.writeString(Files.createTempFile("model", ".gguf"), "fixture").toRealPath()
        val validator = ModelPathValidator.fromSelections(listOf(modelPath), emptyList())
        val registry = InMemoryModelRegistry(validator)
        val model = registry.register(modelPath, modelId = "model")
        val profile = ModelProfile(
            id = "profile",
            displayName = "Profile",
            providerId = "local",
            modelReference = model.canonicalPath,
            temperature = 0.3,
            topP = 0.8,
            maxOutputTokens = 12,
            requiresNativeStructuredOutput = requiresNativeStructuredOutput,
            structuredOutputSchema = if (requiresNativeStructuredOutput) "{}" else null,
        )
        registry.associate(profile, model.id)
        val adapter = FakeLocalRuntimeAdapter(
            runtimeCapabilities = RuntimeCapabilities(
                runtimeId = "fake",
                supportsLocalInference = true,
                supportsStructuredOutput = false,
                supportsThinkingLevel = false,
                supportedModelFormats = setOf("gguf"),
                supportedOperatingSystems = setOf("Windows"),
                supportedArchitectures = setOf("amd64"),
                supportedExecutionDevices = setOf(ExecutionDevice.CPU),
            ),
            generatedResponse = response,
        )
        val manager = LifecycleRuntimeManager(adapter)
        return Fixture(
            LocalInferenceService(registry, validator, RuntimeCompatibilityMatcher(), fixedHardware(), manager),
            adapter,
            manager,
        )
    }

    private fun fixedHardware(): HardwareDetector = object : HardwareDetector {
        override fun detect(): HardwareSnapshot = HardwareSnapshot(
            operatingSystem = "Windows",
            operatingSystemVersion = "11",
            architecture = "amd64",
            availableProcessors = 8,
            memory = MemorySnapshot(maxJvmHeapBytes = 1_000_000),
        )
    }

    private data class Fixture(
        val service: LocalInferenceService,
        val adapter: FakeLocalRuntimeAdapter,
        val manager: LifecycleRuntimeManager,
    )

    private fun <T> runSuspend(block: suspend () -> T): T {
        val outcome = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome.set(result) }
        })
        return outcome.get().getOrThrow()
    }
}
