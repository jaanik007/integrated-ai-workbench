package com.jaanik.integratedaiworkbench.backend.security

enum class ToolOperationClass { READ_ONLY, CONSEQUENTIAL, UNKNOWN }
data class ToolPermissionDecision(val allowed: Boolean, val reason: String)

class ToolPermissionGuard {
    fun isAllowed(toolId: String, allowedToolIds: Set<String>): Boolean =
        toolId.isNotBlank() && toolId in allowedToolIds

    fun classify(toolId: String): ToolOperationClass = when {
        toolId in setOf(
        "get_project_modules", "get_project_dependencies", "get_all_open_file_paths", "list_directory_tree",
        "read_file", "search_file", "search_regex", "search_symbol", "search_text", "get_file_problems",
        "get_repositories", "git_status"
        ) -> ToolOperationClass.READ_ONLY
        toolId.startsWith("create_") || toolId.startsWith("write_") || toolId.startsWith("edit_") ||
            toolId.startsWith("delete_") || toolId.startsWith("execute_") || toolId.startsWith("run_") ||
            toolId.startsWith("terminal_") || toolId.startsWith("git_") -> ToolOperationClass.CONSEQUENTIAL
        else -> ToolOperationClass.UNKNOWN
    }

    fun authorize(
        toolId: String,
        allowedToolIds: Set<String>,
        operationApprovalKey: String,
        approvedConsequentialCalls: Set<String>,
    ): ToolPermissionDecision {
        if (!isAllowed(toolId, allowedToolIds)) return ToolPermissionDecision(false, "The selected agent is not permitted to use this tool.")
        return when (classify(toolId)) {
            ToolOperationClass.READ_ONLY -> ToolPermissionDecision(true, "Read-only operation is permitted.")
            ToolOperationClass.CONSEQUENTIAL -> if (operationApprovalKey in approvedConsequentialCalls) {
                ToolPermissionDecision(true, "Operation-specific approval is present.")
            } else ToolPermissionDecision(false, "This consequential operation needs explicit approval.")
            ToolOperationClass.UNKNOWN -> ToolPermissionDecision(false, "Unknown MCP operations are denied by policy.")
        }
    }
}
