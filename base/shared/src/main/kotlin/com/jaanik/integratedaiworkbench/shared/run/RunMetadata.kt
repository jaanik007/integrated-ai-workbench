package com.jaanik.integratedaiworkbench.shared.run

import java.time.Instant

data class RunMetadata(
    val runId: String,
    val agentId: String,
    val modelProfileId: String,
    val startedAt: Instant,
    val executionPolicy: String,
)
