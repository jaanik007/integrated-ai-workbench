package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.backend.model.ModelPathValidator
import com.jaanik.integratedaiworkbench.backend.model.ModelRegistry
import com.jaanik.integratedaiworkbench.shared.model.ExecutionMetadata
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class InferenceCancellation {
    @Volatile private var cancelled = false
    private val action = AtomicReference<(() -> Unit)?>(null)

    fun cancel() {
        cancelled = true
        action.get()?.invoke()
    }

    fun isCancelled(): Boolean = cancelled

    internal fun attach(onCancel: () -> Unit) {
        action.set(onCancel)
        if (cancelled) onCancel()
    }

    internal fun detach() {
        action.set(null)
    }
}

/** Executes one validated local request and releases the managed runtime before returning. */
class LocalInferenceService(
    private val registry: ModelRegistry,
    private val pathValidator: ModelPathValidator,
    private val compatibilityMatcher: RuntimeCompatibilityMatcher,
    private val hardwareDetector: HardwareDetector,
    private val runtimeManager: LifecycleRuntimeManager,
) {
    suspend fun execute(request: ModelRequest, cancellation: InferenceCancellation = InferenceCancellation()): ModelResponse {
        val startedAt = System.nanoTime()
        val profile = registry.find(request.profileId)
            ?: return failure("PROFILE_NOT_FOUND", request.profileId, durationMillis(startedAt))
        if (profile.executionPolicy != ExecutionPolicy.LOCAL_ONLY) {
            return failure("LOCAL_POLICY_REQUIRED", profile.id, durationMillis(startedAt))
        }
        val model = registry.models().singleOrNull {
            profile.id in it.profileIds && it.canonicalPath == profile.modelReference
        } ?: return failure("REGISTERED_MODEL_NOT_FOUND", profile.id, durationMillis(startedAt))
        val validatedPath = try {
            pathValidator.validatePersistedFile(Path.of(model.canonicalPath))
        } catch (_: Exception) {
            return failure("MODEL_PATH_INVALID", profile.id, durationMillis(startedAt))
        }
        if (validatedPath.toString() != profile.modelReference) {
            return failure("MODEL_PROFILE_MISMATCH", profile.id, durationMillis(startedAt))
        }
        val effectiveThinkingLevel = requestedThinkingLevel(request, profile.thinkingLevel)
        val capabilities = runtimeManager.availableRuntimes().single()
        val compatibility = compatibilityMatcher.evaluate(
            profile.copy(thinkingLevel = effectiveThinkingLevel),
            ModelRequirements(model.candidateFormat),
            hardwareDetector.detect(),
            capabilities,
        )
        if (compatibility.state != CompatibilityState.COMPATIBLE) {
            return failure("RUNTIME_INCOMPATIBLE", profile.id, durationMillis(startedAt), compatibility.state.name)
        }
        if (cancellation.isCancelled()) return cancelled(profile.id, effectiveThinkingLevel, startedAt)

        val effectiveRequest = request.copy(
            expectedSchema = request.expectedSchema ?: if (request.useProfileStructuredOutput) profile.structuredOutputSchema else null,
            thinkingLevel = effectiveThinkingLevel,
            temperature = request.temperature ?: profile.temperature,
            topP = request.topP ?: profile.topP,
            maxOutputTokens = request.maxOutputTokens ?: profile.maxOutputTokens,
        )
        return try {
            ensureModelLoaded()
            cancellation.attach { runtimeManager.shutdown(force = true) }
            if (cancellation.isCancelled()) return cancelled(profile.id, effectiveThinkingLevel, startedAt)
            val response = runtimeManager.generate(effectiveRequest)
            if (cancellation.isCancelled()) cancelled(profile.id, effectiveThinkingLevel, startedAt)
            else response.withProvenance(profile.id, effectiveThinkingLevel, durationMillis(startedAt), effectiveRequest)
        } catch (_: Exception) {
            if (cancellation.isCancelled()) cancelled(profile.id, effectiveThinkingLevel, startedAt)
            else failure("LOCAL_RUNTIME_FAILURE", profile.id, durationMillis(startedAt))
        } finally {
            cancellation.detach()
            runtimeManager.shutdown()
        }
    }

    private fun ensureModelLoaded() {
        when (runtimeManager.status().state) {
            RuntimeLifecycleState.NOT_STARTED, RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.FAILED -> runtimeManager.start()
            RuntimeLifecycleState.STARTING, RuntimeLifecycleState.STOPPING -> error("Runtime lifecycle operation is in progress.")
            RuntimeLifecycleState.READY, RuntimeLifecycleState.LOADING_MODEL, RuntimeLifecycleState.MODEL_LOADED -> Unit
        }
        if (runtimeManager.status().state == RuntimeLifecycleState.READY) runtimeManager.loadModel()
        check(runtimeManager.status().state == RuntimeLifecycleState.MODEL_LOADED) { "Runtime model is not loaded." }
    }

    private fun ModelResponse.withProvenance(
        profileId: String,
        effectiveThinkingLevel: ThinkingLevel,
        durationMillis: Long,
        effectiveRequest: ModelRequest,
    ): ModelResponse {
        val safeMetadata = metadata.filterKeys { it in SAFE_RUNTIME_METADATA_KEYS }.toMutableMap()
        safeMetadata += mapOf(
            "runtimeId" to runtimeManager.availableRuntimes().single().runtimeId,
            "profileId" to profileId,
            "durationMillis" to durationMillis.toString(),
            "maxOutputTokens" to effectiveRequest.maxOutputTokens.toString(),
            "temperature" to effectiveRequest.temperature.toString(),
            "topP" to effectiveRequest.topP.toString(),
        )
        if (effectiveRequest.expectedSchema != null) safeMetadata["structuredOutput"] = "post-generation-validation-required"
        return copy(
            metadata = safeMetadata,
            executionMetadata = ExecutionMetadata(
                runtimeId = runtimeManager.availableRuntimes().single().runtimeId,
                requestedThinkingLevel = effectiveRequest.thinkingLevel,
                effectiveThinkingLevel = effectiveThinkingLevel,
                executionPolicy = ExecutionPolicy.LOCAL_ONLY,
            ),
        )
    }

    private fun failure(code: String, profileId: String, durationMillis: Long, detail: String? = null): ModelResponse =
        ModelResponse("", status = ModelResponseStatus.FAILED, metadata = buildMap {
            put("error", code)
            put("profileId", profileId)
            put("durationMillis", durationMillis.toString())
            detail?.let { put("compatibility", it) }
        })

    private fun cancelled(profileId: String, thinkingLevel: ThinkingLevel, startedAt: Long): ModelResponse =
        ModelResponse("", status = ModelResponseStatus.CANCELLED, metadata = mapOf(
            "error" to "CANCELLED",
            "profileId" to profileId,
            "durationMillis" to durationMillis(startedAt).toString(),
        ), executionMetadata = ExecutionMetadata(
            runtimeId = runtimeManager.availableRuntimes().single().runtimeId,
            requestedThinkingLevel = thinkingLevel,
            effectiveThinkingLevel = thinkingLevel,
            executionPolicy = ExecutionPolicy.LOCAL_ONLY,
        ))

    private fun requestedThinkingLevel(request: ModelRequest, profileLevel: ThinkingLevel): ThinkingLevel =
        if (request.thinkingLevel == ThinkingLevel.DEFAULT) profileLevel else request.thinkingLevel

    private fun durationMillis(startedAt: Long): Long = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()

    private companion object {
        // GPU offload is deliberately retained: it is execution provenance, not
        // a model path, prompt, or other sensitive runtime detail.
        val SAFE_RUNTIME_METADATA_KEYS = setOf("runtime", "error", "executionDevice", "gpuOffloadEvidence")
    }
}
