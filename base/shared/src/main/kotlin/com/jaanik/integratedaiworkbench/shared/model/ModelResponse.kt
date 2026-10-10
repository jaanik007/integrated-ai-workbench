package com.jaanik.integratedaiworkbench.shared.model

data class ModelResponse(
    val content: String,
    val reasoningSummary: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

data class ToolCall(
    val toolId: String,
    val arguments: Map<String, String> = emptyMap(),
)
