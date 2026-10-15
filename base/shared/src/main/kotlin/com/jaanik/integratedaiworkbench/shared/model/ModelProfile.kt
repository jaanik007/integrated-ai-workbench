package com.jaanik.integratedaiworkbench.shared.model

import java.time.Duration

/** User-owned configuration for a model; it never contains model binaries or credentials. */
data class ModelProfile(
    val id: String,
    val displayName: String,
    val providerId: String,
    val modelReference: String,
    val temperature: Double = 0.2,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 2_048,
    /** Optional llama.cpp context-window override. Null keeps the runtime's configured default. */
    val contextWindowTokens: Int? = null,
    val timeout: Duration = Duration.ofSeconds(60),
    val executionPolicy: ExecutionPolicy = ExecutionPolicy.LOCAL_ONLY,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.DEFAULT,
    val structuredOutputSchema: String? = null,
    val requiresNativeStructuredOutput: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "Model profile id must not be blank." }
        require(displayName.isNotBlank()) { "Model profile display name must not be blank." }
        require(providerId.isNotBlank()) { "Model provider id must not be blank." }
        require(modelReference.isNotBlank()) { "Model reference must not be blank." }
        require(temperature.isFinite() && temperature in 0.0..2.0) {
            "Temperature must be finite and between 0.0 and 2.0."
        }
        require(topP.isFinite() && topP in 0.0..1.0) { "Top-p must be finite and between 0.0 and 1.0." }
        require(maxOutputTokens > 0) { "Maximum output tokens must be positive." }
        require(contextWindowTokens == null || contextWindowTokens > 0) {
            "Context window tokens must be positive when configured."
        }
        require(!timeout.isZero && !timeout.isNegative) { "Execution timeout must be positive." }
        require(structuredOutputSchema?.isBlank() != true) { "Structured output schema must not be blank." }
        require(!requiresNativeStructuredOutput || structuredOutputSchema != null) {
            "Native structured output requires a structured output schema."
        }
    }
}

enum class ExecutionPolicy {
    LOCAL_ONLY,
    REMOTE_ALLOWED,
}

/** Provider-neutral execution preference; DEFAULT leaves the runtime in control. */
enum class ThinkingLevel {
    DEFAULT,
    LOW,
    MEDIUM,
    HIGH,
}
