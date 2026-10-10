package com.jaanik.integratedaiworkbench.shared.evaluation

data class EvaluationResult(
    val passed: Boolean,
    val findings: List<String> = emptyList(),
    val evaluatorId: String,
)
