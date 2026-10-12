package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeCompatibilityMatcherTest {
    private val matcher = RuntimeCompatibilityMatcher()
    private val hardware = HardwareSnapshot("Windows", "11", "amd64", 8, MemorySnapshot(32_000, 16_000, 8_000))
    private val profile = ModelProfile("p", "Profile", "local", "/model.gguf")
    private val compatibleRuntime = RuntimeCapabilities(
        "runtime", true, true, true, setOf("gguf"), setOf(ThinkingLevel.LOW),
        setOf("Windows"), setOf("amd64"), setOf(ExecutionDevice.CPU), minimumSystemMemoryBytes = 8_000,
    )

    @Test fun `matches compatible cpu fixtures`() {
        assertEquals(CompatibilityState.COMPATIBLE, matcher.evaluate(profile, ModelRequirements("gguf", 8_000), hardware, compatibleRuntime).state)
    }

    @Test fun `normalizes Windows and x64 platform aliases before matching`() {
        val result = matcher.evaluate(
            profile,
            ModelRequirements("gguf"),
            hardware.copy(operatingSystem = "Windows 11", architecture = "x86_64"),
            compatibleRuntime,
        )
        assertEquals(CompatibilityState.COMPATIBLE, result.state)
    }

    @Test fun `known incompatibility wins over unknown information`() {
        val result = matcher.evaluate(profile, ModelRequirements("onnx", 64_000), hardware.copy(memory = hardware.memory.copy(availablePhysicalBytes = null)), compatibleRuntime)
        assertEquals(CompatibilityState.INCOMPATIBLE, result.state)
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_MODEL_FORMAT })
    }

    @Test fun `unknown memory remains unknown`() {
        val result = matcher.evaluate(profile, ModelRequirements("gguf", 8_000), hardware.copy(memory = hardware.memory.copy(availablePhysicalBytes = null)), compatibleRuntime)
        assertEquals(CompatibilityState.UNKNOWN, result.state)
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNKNOWN_SYSTEM_MEMORY })
    }

    @Test fun `unsupported native structured output and thinking are rejected`() {
        val strict = profile.copy(thinkingLevel = ThinkingLevel.HIGH, structuredOutputSchema = "{}", requiresNativeStructuredOutput = true)
        val result = matcher.evaluate(strict, ModelRequirements("gguf"), hardware, compatibleRuntime)
        assertEquals(CompatibilityState.INCOMPATIBLE, result.state)
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_THINKING_LEVEL })
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_STRUCTURED_OUTPUT })
    }

    @Test fun `unsupported operating system device and local execution are rejected`() {
        val unavailable = compatibleRuntime.copy(
            supportsLocalInference = false,
            supportedOperatingSystems = setOf("Linux"),
            supportedExecutionDevices = setOf(ExecutionDevice.GPU),
        )
        val result = matcher.evaluate(profile, ModelRequirements("gguf"), hardware, unavailable)

        assertEquals(CompatibilityState.INCOMPATIBLE, result.state)
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_LOCAL_EXECUTION })
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_OPERATING_SYSTEM })
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.UNSUPPORTED_DEVICE })
    }

    @Test fun `incomplete format and device descriptors remain unknown`() {
        val incomplete = compatibleRuntime.copy(supportedModelFormats = emptySet(), supportedExecutionDevices = emptySet())
        val result = matcher.evaluate(profile, ModelRequirements(null), hardware, incomplete)

        assertEquals(CompatibilityState.UNKNOWN, result.state)
        assertTrue(result.reasons.any { it.code == CompatibilityReasonCode.INCOMPLETE_RUNTIME_DESCRIPTOR })
    }
}
