package com.jaanik.integratedaiworkbench.backend.evaluation

import com.jaanik.integratedaiworkbench.shared.evaluation.EvaluationResult
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

/** Optional model-based evaluation boundary. A judge runtime is not configured during bootstrap. */
interface JudgeService {
    suspend fun judge(response: ModelResponse): EvaluationResult
}
