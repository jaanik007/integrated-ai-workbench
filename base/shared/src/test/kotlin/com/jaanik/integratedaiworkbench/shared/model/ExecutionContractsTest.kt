package com.jaanik.integratedaiworkbench.shared.model

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionContractsTest {
    @Test
    fun `profile defaults preserve bootstrap construction and default thinking`() {
        val profile = ModelProfile("model-1", "Model", "local", "/models/model.gguf")

        assertTrue(profile.executionPolicy == ExecutionPolicy.LOCAL_ONLY)
        assertTrue(profile.thinkingLevel == ThinkingLevel.DEFAULT)
        assertTrue(profile.structuredOutputSchema == null)
    }

    @Test
    fun `profile rejects invalid execution settings`() {
        assertFailsWith<IllegalArgumentException> { ModelProfile("", "Model", "local", "/model.gguf") }
        assertFailsWith<IllegalArgumentException> { ModelProfile("id", "Model", "local", "/model.gguf", temperature = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { ModelProfile("id", "Model", "local", "/model.gguf", topP = 1.1) }
        assertFailsWith<IllegalArgumentException> { ModelProfile("id", "Model", "local", "/model.gguf", maxOutputTokens = 0) }
        assertFailsWith<IllegalArgumentException> { ModelProfile("id", "Model", "local", "/model.gguf", timeout = Duration.ZERO) }
    }

    @Test
    fun `runtime explicitly accepts or rejects thinking preferences`() {
        val capable = RuntimeCapabilities("local", true, true, true, supportedThinkingLevels = setOf(ThinkingLevel.LOW, ThinkingLevel.HIGH))
        val incapable = RuntimeCapabilities("basic", true, false, false)

        assertTrue(capable.supports(ThinkingLevel.HIGH))
        assertFalse(capable.supports(ThinkingLevel.MEDIUM))
        assertTrue(incapable.supports(ThinkingLevel.DEFAULT))
        assertFailsWith<IllegalArgumentException> { incapable.requireSupported(ThinkingLevel.LOW) }
    }

    @Test
    fun `request validates overrides without credentials or provider fields`() {
        val request = ModelRequest("profile", "Explain this", Duration.ofSeconds(5), thinkingLevel = ThinkingLevel.LOW)
        assertTrue(request.thinkingLevel == ThinkingLevel.LOW)
        assertFailsWith<IllegalArgumentException> { ModelRequest("profile", "Prompt", Duration.ofSeconds(-1)) }
        assertFailsWith<IllegalArgumentException> { ModelRequest("profile", "Prompt", Duration.ofSeconds(1), maxOutputTokens = 0) }
    }
}
