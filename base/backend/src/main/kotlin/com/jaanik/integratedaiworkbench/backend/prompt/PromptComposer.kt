package com.jaanik.integratedaiworkbench.backend.prompt

import com.jaanik.integratedaiworkbench.backend.context.GroundingContext

data class PromptParts(
    val systemInstructions: String,
    val task: String,
    val userInput: String,
    val grounding: GroundingContext? = null,
    val examples: List<PromptExample> = emptyList(),
)

data class PromptExample(val input: String, val output: String)

class PromptComposer {
    fun compose(parts: PromptParts): String = buildString {
        appendLine(parts.systemInstructions.trim())
        appendLine()
        appendLine(parts.task.trim())
        parts.examples.forEach { example ->
            appendLine()
            appendLine("## Example input")
            appendLine(example.input)
            appendLine("## Example output")
            appendLine(example.output)
        }
        parts.grounding?.files?.forEach { file ->
            appendLine()
            appendLine("## ${file.relativePath}")
            appendLine(file.content)
        }
        appendLine()
        append(parts.userInput.trim())
    }
}
