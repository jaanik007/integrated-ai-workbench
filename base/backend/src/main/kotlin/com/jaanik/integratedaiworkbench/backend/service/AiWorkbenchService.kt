package com.jaanik.integratedaiworkbench.backend.service

import com.intellij.openapi.components.Service
import com.jaanik.integratedaiworkbench.backend.agent.AgentRegistry
import com.jaanik.integratedaiworkbench.backend.model.ModelRegistry

@Service(Service.Level.PROJECT)
class AiWorkbenchService(
    private val modelRegistry: ModelRegistry = EmptyModelRegistry,
    private val agentRegistry: AgentRegistry = EmptyAgentRegistry,
) {
    fun state(): WorkbenchState = WorkbenchState(
        registeredModelProfiles = modelRegistry.profiles().size,
        registeredAgents = agentRegistry.agents().size,
        localInferenceConfigured = false,
    )
}

data class WorkbenchState(
    val registeredModelProfiles: Int,
    val registeredAgents: Int,
    val localInferenceConfigured: Boolean,
)

private object EmptyModelRegistry : ModelRegistry {
    override fun profiles() = emptyList<com.jaanik.integratedaiworkbench.shared.model.ModelProfile>()
    override fun find(profileId: String) = null
}

private object EmptyAgentRegistry : AgentRegistry {
    override fun agents() = emptyList<com.jaanik.integratedaiworkbench.shared.agent.AgentDefinition>()
    override fun find(agentId: String) = null
}
