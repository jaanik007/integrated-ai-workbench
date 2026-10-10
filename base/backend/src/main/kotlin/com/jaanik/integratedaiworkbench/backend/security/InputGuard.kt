package com.jaanik.integratedaiworkbench.backend.security

data class InputGuardPolicy(val maximumCharacters: Int = 12_000)

data class GuardResult(val accepted: Boolean, val reason: String? = null)

class InputGuard(private val policy: InputGuardPolicy = InputGuardPolicy()) {
    fun validate(input: String): GuardResult = when {
        input.isBlank() -> GuardResult(false, "Input must not be blank.")
        input.length > policy.maximumCharacters -> GuardResult(false, "Input exceeds the configured character limit.")
        else -> GuardResult(true)
    }
}
