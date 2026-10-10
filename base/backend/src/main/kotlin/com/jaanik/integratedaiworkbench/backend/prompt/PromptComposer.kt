package com.jaanik.integratedaiworkbench.backend.prompt

import com.jaanik.integratedaiworkbench.backend.context.GroundingContext

data class PromptParts(
    val systemInstructions: String,
    val task: String,
    val userInput: String,
    val grounding: GroundingContext? = null,
)

class PromptComposer {
    fun compose(parts: PromptParts): String = buildString {
        appendLine(parts.systemInstructions.trim())
        appendLine()
        appendLine(parts.task.trim())
        parts.grounding?.files?.forEach { file ->
            appendLine()
            appendLine("## ${file.relativePath}")
            appendLine(file.content)
        }
        appendLine()
        append(parts.userInput.trim())
    }
}
