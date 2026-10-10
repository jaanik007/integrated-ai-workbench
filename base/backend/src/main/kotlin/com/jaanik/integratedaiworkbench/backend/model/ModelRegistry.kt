package com.jaanik.integratedaiworkbench.backend.model

import com.jaanik.integratedaiworkbench.shared.model.ModelProfile

interface ModelRegistry {
    fun profiles(): List<ModelProfile>
    fun find(profileId: String): ModelProfile?
}

class InMemoryModelRegistry : ModelRegistry {
    private val entries = mutableMapOf<String, ModelProfile>()

    override fun profiles(): List<ModelProfile> = entries.values.sortedBy(ModelProfile::displayName)

    override fun find(profileId: String): ModelProfile? = entries[profileId]

    fun register(profile: ModelProfile) {
        require(profile.id.isNotBlank()) { "Model profile id must not be blank." }
        entries[profile.id] = profile
    }
}
