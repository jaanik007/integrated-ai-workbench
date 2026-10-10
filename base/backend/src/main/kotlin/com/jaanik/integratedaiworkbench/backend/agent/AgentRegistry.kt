package com.jaanik.integratedaiworkbench.backend.agent

import com.jaanik.integratedaiworkbench.shared.agent.AgentDefinition

interface AgentRegistry {
    fun agents(): List<AgentDefinition>
    fun find(agentId: String): AgentDefinition?
}

class InMemoryAgentRegistry : AgentRegistry {
    private val entries = mutableMapOf<String, AgentDefinition>()

    override fun agents(): List<AgentDefinition> = entries.values.sortedBy(AgentDefinition::displayName)

    override fun find(agentId: String): AgentDefinition? = entries[agentId]

    fun register(agent: AgentDefinition) {
        require(agent.id.isNotBlank()) { "Agent id must not be blank." }
        entries[agent.id] = agent
    }
}
