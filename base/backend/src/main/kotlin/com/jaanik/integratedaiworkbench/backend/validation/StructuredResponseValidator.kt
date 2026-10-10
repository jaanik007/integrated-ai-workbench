package com.jaanik.integratedaiworkbench.backend.validation

import com.jaanik.integratedaiworkbench.backend.security.GuardResult
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

/** Performs deterministic shape checks; schema-specific parsing belongs to a later implementation. */
class StructuredResponseValidator {
    fun validate(response: ModelResponse): GuardResult = when {
        response.content.isBlank() -> GuardResult(false, "Response content must not be blank.")
        response.toolCalls.any { it.toolId.isBlank() } -> GuardResult(false, "Tool calls must have an id.")
        else -> GuardResult(true)
    }
}
