package com.jaanik.integratedaiworkbench.backend.model

import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities

/** Describes a runtime integration without binding the Base Plugin to a concrete inference engine. */
interface ModelProvider {
    val id: String
    val displayName: String

    fun capabilities(): RuntimeCapabilities
}
