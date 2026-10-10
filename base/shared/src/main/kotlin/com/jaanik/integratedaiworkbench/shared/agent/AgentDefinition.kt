package com.jaanik.integratedaiworkbench.shared.agent

data class AgentDefinition(
    val id: String,
    val displayName: String,
    val modelProfileId: String,
    val agentInstructionPath: String,
    val allowedSkillPaths: Set<String> = emptySet(),
    val allowedToolIds: Set<String> = emptySet(),
    val groundingPolicyId: String? = null,
    val securityPolicyId: String? = null,
)
