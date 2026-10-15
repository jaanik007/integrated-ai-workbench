package com.jaanik.integratedaiworkbench.frontend

import java.util.UUID

/**
 * Session-only UI state for the unfinished role, contract, provider-discovery,
 * and workflow features shown in the design prototype. It is intentionally not
 * persisted and never participates in backend authorization or inference.
 */
class WorkbenchSimulationState {
    val roles = mutableListOf(
        SimulatedRole("analyst", "Analyst", "Plans and examines approved context.", linkedSetOf("project.read")),
        SimulatedRole("developer", "Developer", "Prepares implementation proposals.", linkedSetOf("project.read", "project.write")),
        SimulatedRole("reviewer", "Reviewer", "Reviews work and reports risks.", linkedSetOf("project.read")),
    )
    val contracts = mutableListOf(
        SimulatedContract("safe-defaults", "Safe defaults", "SYSTEM.md", "General safety guidance"),
        SimulatedContract("research-planning", "Research planning", "SKILL.md", "Structured planning workflow"),
        SimulatedContract("code-review", "Code review agent", "AGENT.md", "Read-only review instructions"),
        SimulatedContract("project-inspection", "Project inspection", "TOOL.md", "Read-only tool contract"),
    )
    val workflow = SimulatedWorkflow(
        name = "Plan → Build → Review",
        steps = mutableListOf(
            SimulatedWorkflowStep("Plan", "analyst", "Inspect the request and prepare a plan."),
            SimulatedWorkflowStep("Build", "developer", "Prepare an implementation response from the plan."),
            SimulatedWorkflowStep("Review", "reviewer", "Review the simulated outputs and list risks."),
        ),
    )
    val modelRoleAssignments = mutableMapOf<String, MutableSet<String>>()
    var selectedDemoProviderId: String? = null
    var workflowLog: String = "No workflow simulation has run in this IDE session."

    fun newId(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(8)}"
}

data class SimulatedRole(
    val id: String,
    var name: String,
    var description: String,
    val permissions: MutableSet<String>,
    val contractIds: MutableSet<String> = linkedSetOf(),
)

data class SimulatedContract(
    val id: String,
    var name: String,
    var type: String,
    var description: String,
)

data class SimulatedWorkflow(
    var name: String,
    var mode: String = "Sequential",
    var maxConcurrentSteps: Int = 1,
    val steps: MutableList<SimulatedWorkflowStep>,
)

data class SimulatedWorkflowStep(
    var name: String,
    var roleId: String,
    var prompt: String,
)
