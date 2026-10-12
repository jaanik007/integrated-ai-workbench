package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import java.time.Duration
import java.util.ArrayDeque

class FakeLocalRuntimeAdapter(
    override val runtimeId: String = "fake-local",
    private val runtimeCapabilities: RuntimeCapabilities,
    var failStart: Boolean = false,
    var failLoad: Boolean = false,
    var failShutdown: Boolean = false,
    var failGenerate: Boolean = false,
    var generatedResponse: ModelResponse = ModelResponse("fake response"),
    var generationDelay: Duration = Duration.ZERO,
) : ManagedLocalRuntimeAdapter {
    private val queuedResponses = ArrayDeque<ModelResponse>()
    private var generatedDefaultOnce = false
    private var started = false
    @Volatile var lastRequest: ModelRequest? = null
        private set
    override fun capabilities(): RuntimeCapabilities = runtimeCapabilities
    override fun start() { check(!failStart) { "Configured startup failure." }; started = true }
    override fun isReady(): Boolean = started
    override fun loadModel() { check(started && !failLoad) { "Configured model loading failure." } }
    override fun shutdown(force: Boolean) { check(!failShutdown) { "Configured shutdown failure." }; started = false }
    override suspend fun generate(request: ModelRequest): ModelResponse {
        check(started && !failGenerate) { "Configured generation failure." }
        lastRequest = request
        if (!generationDelay.isZero) Thread.sleep(generationDelay.toMillis())
        return when {
            !generatedDefaultOnce -> generatedResponse.also { generatedDefaultOnce = true }
            queuedResponses.isNotEmpty() -> queuedResponses.removeFirst()
            else -> generatedResponse
        }
    }

    fun enqueue(response: ModelResponse) { queuedResponses += response }
}
