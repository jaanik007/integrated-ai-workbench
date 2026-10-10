package com.jaanik.integratedaiworkbench.backend.security

import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

data class OutputGuardPolicy(val maximumCharacters: Int = 24_000)

class OutputGuard(private val policy: OutputGuardPolicy = OutputGuardPolicy()) {
    fun validate(response: ModelResponse): GuardResult =
        if (response.content.length <= policy.maximumCharacters) GuardResult(true)
        else GuardResult(false, "Output exceeds the configured character limit.")
}
