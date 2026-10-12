package com.jaanik.integratedaiworkbench.backend.prompt

import com.jaanik.integratedaiworkbench.backend.context.ContextService
import com.jaanik.integratedaiworkbench.backend.context.GroundingRequest
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.backend.security.SecretSanitizer

data class PromptPreparationPolicy(
    val maximumExamples: Int = 5,
    val maximumExampleCharacters: Int = 4_000,
)

sealed interface PromptPreparationResult {
    data class Prepared(val parts: PromptParts) : PromptPreparationResult
    data class Rejected(val reason: String) : PromptPreparationResult
}

/** Applies input, grounding, and secret controls before any runtime receives prompt content. */
class PromptPreparationService(
    private val inputGuard: InputGuard,
    private val contextService: ContextService,
    private val sanitizer: SecretSanitizer = SecretSanitizer(),
    private val policy: PromptPreparationPolicy = PromptPreparationPolicy(),
) {
    fun prepare(
        systemInstructions: String,
        task: String,
        userInput: String,
        groundingRequest: GroundingRequest,
        examples: List<PromptExample> = emptyList(),
    ): PromptPreparationResult {
        val input = inputGuard.validate(userInput)
        if (!input.accepted) return PromptPreparationResult.Rejected(input.reason ?: "Input was rejected.")
        if (examples.size > policy.maximumExamples || examples.any { it.input.length + it.output.length > policy.maximumExampleCharacters }) {
            return PromptPreparationResult.Rejected("Prompt examples exceed the configured limit.")
        }
        val context = runCatching { contextService.collect(groundingRequest) }
            .getOrElse { return PromptPreparationResult.Rejected("Grounding request was rejected.") }
        return PromptPreparationResult.Prepared(
            PromptParts(
                systemInstructions = systemInstructions.trim(),
                task = task.trim(),
                userInput = sanitizer.sanitize(userInput),
                grounding = context,
                examples = examples.map { PromptExample(sanitizer.sanitize(it.input), sanitizer.sanitize(it.output)) },
            ),
        )
    }
}
