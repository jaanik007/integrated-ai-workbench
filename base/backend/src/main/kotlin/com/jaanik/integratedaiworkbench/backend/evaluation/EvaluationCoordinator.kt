package com.jaanik.integratedaiworkbench.backend.evaluation

import com.jaanik.integratedaiworkbench.backend.agent.AgentExecutionRequest
import com.jaanik.integratedaiworkbench.backend.agent.AgentExecutionService
import com.jaanik.integratedaiworkbench.backend.context.GroundingRequest
import com.jaanik.integratedaiworkbench.backend.security.SecretSanitizer
import com.jaanik.integratedaiworkbench.shared.evaluation.*
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionOutcome
import com.jaanik.integratedaiworkbench.shared.run.AgentExecutionStatus
import java.security.MessageDigest

data class EvaluationCriterion(val id: String, val minimumScore: Int, val maximumScore: Int) { init { require(id.isNotBlank() && minimumScore in 0..maximumScore) } }
data class EvaluationRubric(val id: String, val version: String, val criteria: List<EvaluationCriterion>) { init { require(id.isNotBlank() && version.isNotBlank() && criteria.isNotEmpty() && criteria.map { it.id }.distinct().size == criteria.size) }; val fingerprint get() = MessageDigest.getInstance("SHA-256").digest("$id|$version|$criteria".toByteArray()).joinToString("") { "%02x".format(it) } }
fun interface JudgeExecutor { suspend fun judge(candidate: String, rubric: EvaluationRubric): AgentExecutionOutcome }

/** Uses the approved `judge` runtime agent and therefore the same managed local lifecycle as normal execution. */
class AgentExecutionJudgeExecutor(private val execution: AgentExecutionService) : JudgeExecutor {
    override suspend fun judge(candidate: String, rubric: EvaluationRubric): AgentExecutionOutcome {
        val scoreRequirements = rubric.criteria.joinToString { "${it.id}: integer ${it.minimumScore}-${it.maximumScore}" }
        return execution.execute(
            AgentExecutionRequest(
                agentId = "judge",
                userInput = "Evaluate this sanitized candidate only. Rubric: ${rubric.id}@${rubric.version}; criteria: ${rubric.criteria}. Candidate: $candidate. Return only JSON with status exactly PASS or FAIL and scores containing exactly $scoreRequirements.",
                groundingRequest = GroundingRequest(emptySet(), maximumFiles = 1, maximumCharacters = 1),
            ),
        )
    }
}

class EvaluationCoordinator(private val judge: JudgeExecutor? = null, private val sanitizer: SecretSanitizer = SecretSanitizer()) {
    suspend fun evaluate(candidate: AgentExecutionOutcome, rubric: EvaluationRubric): EvaluationResult {
        val provenance = mapOf("runId" to candidate.metadata.runId, "rubricId" to rubric.id, "rubricVersion" to rubric.version)
        if (candidate.status != AgentExecutionStatus.SUCCESS || candidate.response == null) return EvaluationResult(false, listOf("MANDATORY_EXECUTION_VALIDATION_FAILED"), "deterministic", EvaluationStatus.FAILED, false, JudgeStatus.NOT_REQUESTED, rubric.fingerprint, provenance)
        val executor = judge ?: return EvaluationResult(false, listOf("JUDGE_UNAVAILABLE"), "deterministic", EvaluationStatus.UNKNOWN, true, JudgeStatus.UNKNOWN, rubric.fingerprint, provenance)
        val result = executor.judge(sanitizer.sanitize(candidate.response.content).take(12_000), rubric)
        if (result.status != AgentExecutionStatus.SUCCESS || result.response == null) return EvaluationResult(false, listOf("JUDGE_UNAVAILABLE"), "judge", EvaluationStatus.UNKNOWN, true, JudgeStatus.UNKNOWN, rubric.fingerprint, provenance)
        val parsed = parse(result.response.content, rubric) ?: return EvaluationResult(false, listOf("INVALID_JUDGE_RESULT"), "judge", EvaluationStatus.UNKNOWN, true, JudgeStatus.UNKNOWN, rubric.fingerprint, provenance)
        return EvaluationResult(parsed.first, emptyList(), "judge", if (parsed.first) EvaluationStatus.PASSED else EvaluationStatus.FAILED, true, if (parsed.first) JudgeStatus.PASSED else JudgeStatus.FAILED, rubric.fingerprint, provenance + mapOf("judgeAgentId" to result.metadata.agentId, "judgeProfileId" to result.metadata.modelProfileId))
    }
    private fun parse(text: String, rubric: EvaluationRubric): Pair<Boolean, Map<String, Int>>? {
        val status = Regex("\\\"status\\\"\\s*:\\s*\\\"(PASS|FAIL)\\\"").find(text)?.groupValues?.get(1) ?: return null
        val scores = Regex("\\\"scores\\\"\\s*:\\s*\\{([^{}]*)}").find(text)?.groupValues?.get(1) ?: return null
        val found = Regex("\\\"([^\\\"]+)\\\"\\s*:\\s*(\\d+)").findAll(scores).associate { it.groupValues[1] to it.groupValues[2].toInt() }
        if (found.keys != rubric.criteria.map { it.id }.toSet() || rubric.criteria.any { found.getValue(it.id) !in it.minimumScore..it.maximumScore }) return null
        return (status == "PASS") to found
    }
}
