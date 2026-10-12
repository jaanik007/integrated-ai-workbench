package com.jaanik.integratedaiworkbench.backend.agent

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RuntimeMarkdownLoaderTest {
    @Test fun `loads curated runtime definitions without codex resources`() {
        val loader = RuntimeMarkdownLoader(Path.of(System.getProperty("user.dir"), ".ai"))
        val configuration = loader.reload("profile")
        assertTrue(setOf("default", "coding", "judge").all { it in configuration.agents })
        assertTrue("safe-local-assistance" in configuration.skills)
        assertTrue(configuration.agents.getValue("judge").definition.allowedToolIds.isEmpty())
    }

    @Test fun `failed reload retains previous valid configuration`() {
        val root = Files.createTempDirectory("runtime-definitions")
        createSkill(root, "safe", "safe")
        createAgent(root, "agent", "safe")
        val loader = RuntimeMarkdownLoader(root)
        val first = loader.reload()
        Files.writeString(root.resolve("agents/agent/AGENT.md"), "# Broken")

        assertFailsWith<RuntimeConfigurationException> { loader.reload() }
        assertEquals(first, loader.configuration())
    }

    @Test fun `missing skill metadata and paths outside root are rejected`() {
        val root = Files.createTempDirectory("runtime-definitions")
        Files.createDirectories(root.resolve("skills/bad"))
        Files.writeString(root.resolve("skills/bad/SKILL.md"), "# Bad")
        Files.createDirectories(root.resolve("agents"))
        assertFailsWith<RuntimeConfigurationException> { RuntimeMarkdownLoader(root).reload() }
    }

    private fun createSkill(root: Path, directory: String, id: String) {
        Files.createDirectories(root.resolve("skills/$directory"))
        Files.writeString(root.resolve("skills/$directory/SKILL.md"), "# Skill\n\n**Skill ID:** `$id`\n\nInstructions")
    }

    private fun createAgent(root: Path, id: String, skillId: String) {
        Files.createDirectories(root.resolve("agents/$id"))
        Files.writeString(root.resolve("agents/$id/AGENT.md"), "# Agent\n\nPermitted: `$skillId`")
    }
}
