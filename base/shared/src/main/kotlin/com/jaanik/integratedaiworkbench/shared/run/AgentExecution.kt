package com.jaanik.integratedaiworkbench.shared.run

import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

enum class AgentExecutionStatus {
    SUCCESS,
    INVALID_CONFIGURATION,
    INPUT_REJECTED,
    GROUNDING_REJECTED,
    RUNTIME_FAILURE,
    TIMED_OUT,
    CANCELLED,
    OUTPUT_REJECTED,
    STRUCTURED_OUTPUT_INVALID,
    UNAUTHORIZED_TOOL,
    TOOL_UNAVAILABLE,
    TOOL_ARGUMENTS_INVALID,
    TOOL_APPROVAL_REQUIRED,
    TOOL_EXECUTION_FAILED,
    TOOL_CALL_LIMIT_REACHED,
}

/** Sanitised provenance for one Workbench-dispatched tool request. */
data class ToolExecutionProvenance(
    val toolId: String,
    val approved: Boolean,
    val completed: Boolean,
    val detail: String,
)

/** Safe, user-facing result of one policy-enforced agent invocation. */
data class AgentExecutionOutcome(
    val status: AgentExecutionStatus,
    val response: ModelResponse? = null,
    val metadata: RunMetadata,
    val skillIds: Set<String> = emptySet(),
    val groundingReferences: List<String> = emptyList(),
    val toolExecutions: List<ToolExecutionProvenance> = emptyList(),
    val validationError: String? = null,
) {
    init {
        require(status == AgentExecutionStatus.SUCCESS || response == null) {
            "Failed execution outcomes must not expose an unvalidated response."
        }
    }
}
