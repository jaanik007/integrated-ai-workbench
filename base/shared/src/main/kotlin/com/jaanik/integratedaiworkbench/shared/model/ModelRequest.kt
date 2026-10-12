package com.jaanik.integratedaiworkbench.shared.model

import java.time.Duration

data class ModelRequest(
    val profileId: String,
    val prompt: String,
    val timeout: Duration,
    val expectedSchema: String? = null,
    val useProfileStructuredOutput: Boolean = true,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.DEFAULT,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
) {
    init {
        require(profileId.isNotBlank()) { "Model profile id must not be blank." }
        require(prompt.isNotBlank()) { "Prompt must not be blank." }
        require(!timeout.isZero && !timeout.isNegative) { "Execution timeout must be positive." }
        require(expectedSchema?.isBlank() != true) { "Expected schema must not be blank." }
        require(temperature == null || (temperature.isFinite() && temperature in 0.0..2.0)) {
            "Temperature must be finite and between 0.0 and 2.0."
        }
        require(topP == null || (topP.isFinite() && topP in 0.0..1.0)) {
            "Top-p must be finite and between 0.0 and 1.0."
        }
        require(maxOutputTokens == null || maxOutputTokens > 0) { "Maximum output tokens must be positive." }
    }
}
