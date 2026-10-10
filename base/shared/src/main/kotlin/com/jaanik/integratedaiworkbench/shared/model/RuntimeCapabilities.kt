package com.jaanik.integratedaiworkbench.shared.model

data class RuntimeCapabilities(
    val runtimeId: String,
    val supportsLocalInference: Boolean,
    val supportsStructuredOutput: Boolean,
    val supportsThinkingLevel: Boolean,
    val supportedModelFormats: Set<String> = emptySet(),
)
