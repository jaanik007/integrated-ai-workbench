package com.jaanik.integratedaiworkbench.shared.run

import java.time.Instant
import com.jaanik.integratedaiworkbench.shared.model.ExecutionPolicy
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel

data class RunMetadata(
    val runId: String,
    val agentId: String,
    val modelProfileId: String,
    val startedAt: Instant,
    val executionPolicy: ExecutionPolicy,
    val requestedThinkingLevel: ThinkingLevel = ThinkingLevel.DEFAULT,
    val effectiveThinkingLevel: ThinkingLevel = ThinkingLevel.DEFAULT,
    val outcome: ModelResponseStatus? = null,
    val completedAt: Instant? = null,
) {
    init {
        require(runId.isNotBlank()) { "Run id must not be blank." }
        require(agentId.isNotBlank()) { "Agent id must not be blank." }
        require(modelProfileId.isNotBlank()) { "Model profile id must not be blank." }
        require(completedAt == null || !completedAt.isBefore(startedAt)) {
            "Completion time must not precede start time."
        }
    }
}
