package com.jaanik.integratedaiworkbench.backend.model

import com.intellij.openapi.components.service
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel

/**
 * Validates the editable, provider-neutral subset of a local model preset and
 * delegates persistence to the one existing model registry.
 */
class ModelPresetService(
    private val registry: PersistentModelRegistry = service(),
) {
    fun save(
        profile: ModelProfile,
        displayName: String,
        temperature: Double,
        topP: Double,
        maxOutputTokens: Int,
        thinkingLevel: ThinkingLevel,
    ): ModelProfile {
        require(displayName.isNotBlank()) { "Model display name must not be blank." }
        require(temperature.isFinite() && temperature in 0.0..2.0) {
            "Temperature must be between 0.0 and 2.0."
        }
        require(topP.isFinite() && topP in 0.0..1.0) { "Top-p must be between 0.0 and 1.0." }
        require(maxOutputTokens in 1..131_072) {
            "Maximum output tokens must be between 1 and 131072."
        }

        return registry.updateLocalProfile(
            profile.copy(
                displayName = displayName.trim(),
                temperature = temperature,
                topP = topP,
                maxOutputTokens = maxOutputTokens,
                thinkingLevel = thinkingLevel,
            ),
        )
    }
}
