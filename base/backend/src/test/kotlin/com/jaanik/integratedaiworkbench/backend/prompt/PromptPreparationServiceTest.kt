package com.jaanik.integratedaiworkbench.backend.prompt

import com.jaanik.integratedaiworkbench.backend.context.FileSystemContextService
import com.jaanik.integratedaiworkbench.backend.context.GroundingRequest
import com.jaanik.integratedaiworkbench.backend.security.InputGuard
import com.jaanik.integratedaiworkbench.backend.security.OutputGuard
import com.jaanik.integratedaiworkbench.backend.security.OutputGuardPolicy
import com.jaanik.integratedaiworkbench.backend.security.ToolPermissionGuard
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PromptPreparationServiceTest {
    @Test fun `prepares separately typed sanitized prompt content`() {
        val root = Files.createTempDirectory("prompt")
        val source = Files.writeString(root.resolve("source.md"), "password=grounding-secret")
        val service = PromptPreparationService(InputGuard(), FileSystemContextService(setOf(root)))
        val result = service.prepare("System", "Task", "api_key=user-secret", GroundingRequest(setOf(source.toString()), 1, 100, 100), listOf(PromptExample("api_key=example", "answer")))

        val prepared = assertIs<PromptPreparationResult.Prepared>(result).parts
        assertTrue(prepared.userInput.contains("[REDACTED]"))
        assertTrue(prepared.grounding!!.files.single().content.contains("[REDACTED]"))
        assertTrue(prepared.examples.single().input.contains("[REDACTED]"))
        assertTrue(PromptComposer().compose(prepared).contains("## Example input"))
    }

    @Test fun `rejects invalid input examples oversized output and unauthorized tools`() {
        val root = Files.createTempDirectory("prompt")
        val service = PromptPreparationService(InputGuard(), FileSystemContextService(setOf(root)), policy = PromptPreparationPolicy(maximumExamples = 1, maximumExampleCharacters = 4))
        assertIs<PromptPreparationResult.Rejected>(service.prepare("System", "Task", " ", GroundingRequest(emptySet(), 1, 10)))
        assertIs<PromptPreparationResult.Rejected>(service.prepare("System", "Task", "ok", GroundingRequest(emptySet(), 1, 10), listOf(PromptExample("abc", "def"))))
        assertFalse(OutputGuard(OutputGuardPolicy(1)).validate(ModelResponse("too long")).accepted)
        assertFalse(ToolPermissionGuard().isAllowed("shell", emptySet()))
    }
}
