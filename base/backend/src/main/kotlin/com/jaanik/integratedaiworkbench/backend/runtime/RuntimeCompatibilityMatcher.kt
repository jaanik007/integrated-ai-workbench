package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.AccelerationBackend
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities

enum class CompatibilityState { COMPATIBLE, INCOMPATIBLE, UNKNOWN }

enum class CompatibilityReasonCode {
    UNSUPPORTED_LOCAL_EXECUTION,
    UNSUPPORTED_OPERATING_SYSTEM,
    UNSUPPORTED_ARCHITECTURE,
    UNSUPPORTED_MODEL_FORMAT,
    INSUFFICIENT_SYSTEM_MEMORY,
    UNKNOWN_SYSTEM_MEMORY,
    UNSUPPORTED_DEVICE,
    UNSUPPORTED_ACCELERATION_BACKEND,
    UNSUPPORTED_STRUCTURED_OUTPUT,
    UNSUPPORTED_THINKING_LEVEL,
    INCOMPLETE_RUNTIME_DESCRIPTOR,
}

data class ModelRequirements(
    val modelFormat: String?,
    val minimumSystemMemoryBytes: Long? = null,
    val requiredDevice: ExecutionDevice = ExecutionDevice.CPU,
    val requiredAccelerationBackend: AccelerationBackend? = null,
)

data class CompatibilityReason(val code: CompatibilityReasonCode, val message: String)

data class CompatibilityResult(
    val state: CompatibilityState,
    val reasons: List<CompatibilityReason>,
)

class RuntimeCompatibilityMatcher {
    fun evaluate(
        profile: ModelProfile,
        model: ModelRequirements,
        hardware: HardwareSnapshot,
        runtime: RuntimeCapabilities,
    ): CompatibilityResult {
        val incompatible = mutableListOf<CompatibilityReason>()
        val unknown = mutableListOf<CompatibilityReason>()

        fun unsupported(condition: Boolean, code: CompatibilityReasonCode, message: String) {
            if (condition) incompatible += CompatibilityReason(code, message)
        }
        fun missing(condition: Boolean, code: CompatibilityReasonCode, message: String) {
            if (condition) unknown += CompatibilityReason(code, message)
        }

        unsupported(!runtime.supportsLocalInference, CompatibilityReasonCode.UNSUPPORTED_LOCAL_EXECUTION, "Runtime does not support local inference.")
        unsupported(runtime.supportedOperatingSystems.isNotEmpty() && runtime.supportedOperatingSystems.none {
            normalizeOperatingSystem(it) == normalizeOperatingSystem(hardware.operatingSystem)
        },
            CompatibilityReasonCode.UNSUPPORTED_OPERATING_SYSTEM, "Runtime does not support this operating system.")
        unsupported(runtime.supportedArchitectures.isNotEmpty() && runtime.supportedArchitectures.none {
            normalizeArchitecture(it) == normalizeArchitecture(hardware.architecture)
        },
            CompatibilityReasonCode.UNSUPPORTED_ARCHITECTURE, "Runtime does not support this architecture.")
        unsupported(model.modelFormat != null && runtime.supportedModelFormats.isNotEmpty() && model.modelFormat !in runtime.supportedModelFormats,
            CompatibilityReasonCode.UNSUPPORTED_MODEL_FORMAT, "Runtime does not support the model format.")
        missing(model.modelFormat == null || runtime.supportedModelFormats.isEmpty(),
            CompatibilityReasonCode.INCOMPLETE_RUNTIME_DESCRIPTOR, "Model-format support cannot be established.")
        unsupported(runtime.supportedExecutionDevices.isNotEmpty() && model.requiredDevice !in runtime.supportedExecutionDevices,
            CompatibilityReasonCode.UNSUPPORTED_DEVICE, "Runtime does not support the requested execution device.")
        missing(runtime.supportedExecutionDevices.isEmpty(), CompatibilityReasonCode.INCOMPLETE_RUNTIME_DESCRIPTOR,
            "Runtime execution-device support is not declared.")
        if (model.requiredAccelerationBackend != null) {
            unsupported(model.requiredAccelerationBackend !in runtime.supportedAccelerationBackends,
                CompatibilityReasonCode.UNSUPPORTED_ACCELERATION_BACKEND, "Runtime does not support the requested acceleration backend.")
        }
        val memoryRequirement = listOfNotNull(model.minimumSystemMemoryBytes, runtime.minimumSystemMemoryBytes).maxOrNull()
        if (memoryRequirement != null) {
            val available = hardware.memory.availablePhysicalBytes
            unsupported(available != null && available < memoryRequirement, CompatibilityReasonCode.INSUFFICIENT_SYSTEM_MEMORY,
                "Available physical memory is insufficient.")
            missing(available == null, CompatibilityReasonCode.UNKNOWN_SYSTEM_MEMORY, "Available physical memory is unknown.")
        }
        unsupported(profile.requiresNativeStructuredOutput && !runtime.supportsNativeStructuredOutput,
            CompatibilityReasonCode.UNSUPPORTED_STRUCTURED_OUTPUT, "Runtime does not support native structured output.")
        unsupported(!runtime.supports(profile.thinkingLevel), CompatibilityReasonCode.UNSUPPORTED_THINKING_LEVEL,
            "Runtime does not support the requested thinking level.")

        return when {
            incompatible.isNotEmpty() -> CompatibilityResult(CompatibilityState.INCOMPATIBLE, incompatible + unknown)
            unknown.isNotEmpty() -> CompatibilityResult(CompatibilityState.UNKNOWN, unknown)
            else -> CompatibilityResult(CompatibilityState.COMPATIBLE, emptyList())
        }
    }

    private fun normalizeOperatingSystem(value: String): String = when {
        value.startsWith("windows", ignoreCase = true) -> "windows"
        value.startsWith("mac", ignoreCase = true) -> "macos"
        else -> value.lowercase()
    }

    private fun normalizeArchitecture(value: String): String = when (value.lowercase()) {
        "amd64", "x86_64", "x64" -> "x86_64"
        "aarch64", "arm64" -> "arm64"
        else -> value.lowercase()
    }
}
