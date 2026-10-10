package com.jaanik.integratedaiworkbench.backend.security

class ToolPermissionGuard {
    fun isAllowed(toolId: String, allowedToolIds: Set<String>): Boolean =
        toolId.isNotBlank() && toolId in allowedToolIds
}
