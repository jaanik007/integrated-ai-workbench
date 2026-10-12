package com.jaanik.integratedaiworkbench.shared.model

data class RuntimeCapabilities(
    val runtimeId: String,
    val supportsLocalInference: Boolean,
    val supportsStructuredOutput: Boolean,
    val supportsThinkingLevel: Boolean,
    val supportedModelFormats: Set<String> = emptySet(),
    val supportedThinkingLevels: Set<ThinkingLevel> = emptySet(),
    val supportedOperatingSystems: Set<String> = emptySet(),
    val supportedArchitectures: Set<String> = emptySet(),
    val supportedExecutionDevices: Set<ExecutionDevice> = emptySet(),
    val supportedAccelerationBackends: Set<AccelerationBackend> = emptySet(),
    val minimumSystemMemoryBytes: Long? = null,
    val supportsNativeStructuredOutput: Boolean = false,
) {
    init {
        require(runtimeId.isNotBlank()) { "Runtime id must not be blank." }
        require(!supportsThinkingLevel || supportedThinkingLevels.isNotEmpty()) {
            "A runtime that supports thinking levels must declare supported levels."
        }
        require(minimumSystemMemoryBytes == null || minimumSystemMemoryBytes > 0) {
            "Minimum system memory must be positive when declared."
        }
    }

    fun supports(thinkingLevel: ThinkingLevel): Boolean =
        thinkingLevel == ThinkingLevel.DEFAULT || (supportsThinkingLevel && thinkingLevel in supportedThinkingLevels)

    fun requireSupported(thinkingLevel: ThinkingLevel) {
        require(supports(thinkingLevel)) {
            "Runtime '$runtimeId' does not support thinking level $thinkingLevel."
        }
    }
}

enum class ExecutionDevice { CPU, GPU }

enum class AccelerationBackend { CUDA, VULKAN, METAL, DIRECT_ML }
