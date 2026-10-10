package com.jaanik.integratedaiworkbench.backend.evaluation

import com.jaanik.integratedaiworkbench.shared.evaluation.EvaluationResult
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

interface EvaluationService {
    fun evaluate(response: ModelResponse): EvaluationResult
}
