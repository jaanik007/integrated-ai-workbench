package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities

/** Extension point implemented by a future, optional local inference runtime adapter. */
interface LocalInferenceAdapter {
    val runtimeId: String

    fun capabilities(): RuntimeCapabilities

    suspend fun generate(request: ModelRequest): ModelResponse
}
