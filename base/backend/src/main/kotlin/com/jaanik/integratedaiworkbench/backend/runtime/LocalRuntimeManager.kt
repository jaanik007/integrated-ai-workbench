package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities

/** Entry point for user-configured local runtimes. No runtime implementation is bundled at bootstrap. */
interface LocalRuntimeManager {
    fun availableRuntimes(): List<RuntimeCapabilities>

    suspend fun generate(request: ModelRequest): ModelResponse
}
