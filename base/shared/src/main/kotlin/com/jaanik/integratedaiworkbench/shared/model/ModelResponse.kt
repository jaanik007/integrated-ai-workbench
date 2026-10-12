package com.jaanik.integratedaiworkbench.shared.model

data class ModelResponse(
    val content: String,
    val reasoningSummary: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val status: ModelResponseStatus = ModelResponseStatus.SUCCESS,
    val executionMetadata: ExecutionMetadata? = null,
) {
    init {
        require(status != ModelResponseStatus.SUCCESS || content.isNotBlank()) {
            "Successful model responses must contain content."
        }
    }
}

enum class ModelResponseStatus {
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMED_OUT,
}

/** Safe, typed execution settings retained for result provenance. */
data class ExecutionMetadata(
    val runtimeId: String,
    val requestedThinkingLevel: ThinkingLevel,
    val effectiveThinkingLevel: ThinkingLevel,
    val executionPolicy: ExecutionPolicy,
) {
    init {
        require(runtimeId.isNotBlank()) { "Runtime id must not be blank." }
    }
}

data class ToolCall(
    val toolId: String,
    val arguments: Map<String, String> = emptyMap(),
)
