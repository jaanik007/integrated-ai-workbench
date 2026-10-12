package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities

enum class RuntimeLifecycleState { NOT_STARTED, STARTING, READY, LOADING_MODEL, MODEL_LOADED, STOPPING, STOPPED, FAILED }

data class RuntimeStatus(val state: RuntimeLifecycleState, val diagnostic: String? = null)

interface ManagedLocalRuntimeAdapter : LocalInferenceAdapter {
    fun start()
    fun isReady(): Boolean
    fun loadModel()
    fun shutdown(force: Boolean = false)
}

/** Entry point for user-configured local runtimes. No runtime implementation is bundled at bootstrap. */
interface LocalRuntimeManager {
    fun availableRuntimes(): List<RuntimeCapabilities>

    suspend fun generate(request: ModelRequest): ModelResponse
}

/** Owns lifecycle transitions for a selected adapter; selection remains external and compatibility-gated. */
class LifecycleRuntimeManager(private val adapter: ManagedLocalRuntimeAdapter) : LocalRuntimeManager {
    private var status = RuntimeStatus(RuntimeLifecycleState.NOT_STARTED)

    fun status(): RuntimeStatus = status

    fun start() {
        if (status.state in setOf(RuntimeLifecycleState.READY, RuntimeLifecycleState.MODEL_LOADED)) return
        require(status.state in setOf(RuntimeLifecycleState.NOT_STARTED, RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.FAILED)) { "Runtime cannot start from ${status.state}." }
        status = RuntimeStatus(RuntimeLifecycleState.STARTING)
        try {
            adapter.start()
            check(adapter.isReady()) { "Runtime readiness check failed." }
            status = RuntimeStatus(RuntimeLifecycleState.READY)
        } catch (error: Exception) {
            status = RuntimeStatus(RuntimeLifecycleState.FAILED, "Runtime startup failed.")
            throw IllegalStateException("Runtime startup failed.", error)
        }
    }

    fun loadModel() {
        require(status.state == RuntimeLifecycleState.READY) { "Runtime must be ready before loading a model." }
        status = RuntimeStatus(RuntimeLifecycleState.LOADING_MODEL)
        try { adapter.loadModel(); status = RuntimeStatus(RuntimeLifecycleState.MODEL_LOADED) }
        catch (error: Exception) { status = RuntimeStatus(RuntimeLifecycleState.FAILED, "Model loading failed."); throw IllegalStateException("Model loading failed.", error) }
    }

    fun shutdown(force: Boolean = false) {
        if (status.state in setOf(RuntimeLifecycleState.NOT_STARTED, RuntimeLifecycleState.STOPPED)) return
        status = RuntimeStatus(RuntimeLifecycleState.STOPPING)
        try { adapter.shutdown(force); status = RuntimeStatus(RuntimeLifecycleState.STOPPED) }
        catch (_: Exception) { status = RuntimeStatus(RuntimeLifecycleState.FAILED, "Runtime shutdown failed.") }
    }

    override fun availableRuntimes(): List<RuntimeCapabilities> = listOf(adapter.capabilities())

    override suspend fun generate(request: ModelRequest): ModelResponse {
        require(status.state == RuntimeLifecycleState.MODEL_LOADED) { "Runtime must have a loaded model before generation." }
        return adapter.generate(request)
    }
}
