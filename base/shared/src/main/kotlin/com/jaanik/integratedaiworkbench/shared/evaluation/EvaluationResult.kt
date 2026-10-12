package com.jaanik.integratedaiworkbench.shared.evaluation

data class EvaluationResult(
    val passed: Boolean,
    val findings: List<String> = emptyList(),
    val evaluatorId: String,
    val status: EvaluationStatus = if (passed) EvaluationStatus.PASSED else EvaluationStatus.FAILED,
    val deterministicPassed: Boolean = passed,
    val judgeStatus: JudgeStatus = JudgeStatus.NOT_REQUESTED,
    val rubricFingerprint: String? = null,
    val provenance: Map<String, String> = emptyMap(),
)

enum class EvaluationStatus { PASSED, FAILED, UNKNOWN }
enum class JudgeStatus { NOT_REQUESTED, PASSED, FAILED, UNKNOWN }
