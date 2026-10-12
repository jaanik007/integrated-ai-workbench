package com.jaanik.integratedaiworkbench.backend.agent

import com.jaanik.integratedaiworkbench.shared.agent.AgentDefinition
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

data class RuntimeSkillDefinition(val id: String, val instructions: String, val source: String, val fingerprint: String)
data class RuntimeAgentDefinition(
    val definition: AgentDefinition,
    val instructions: String,
    val fingerprint: String,
)
data class RuntimeConfiguration(val agents: Map<String, RuntimeAgentDefinition>, val skills: Map<String, RuntimeSkillDefinition>)
class RuntimeConfigurationException(message: String) : IllegalArgumentException(message)

/** Loads only curated, declarative `.ai` Markdown resources; it never executes their content. */
class RuntimeMarkdownLoader(private val aiRoot: Path) {
    private var activeConfiguration = RuntimeConfiguration(emptyMap(), emptyMap())

    fun configuration(): RuntimeConfiguration = activeConfiguration

    fun reload(modelProfileId: String = "unassigned"): RuntimeConfiguration {
        require(modelProfileId.isNotBlank()) { "Model profile id must not be blank." }
        val root = canonicalDirectory(aiRoot)
        val skills = loadSkills(root.resolve("skills"), root)
        val agents = loadAgents(root.resolve("agents"), root, skills, modelProfileId)
        return RuntimeConfiguration(agents, skills).also { activeConfiguration = it }
    }

    private fun loadSkills(directory: Path, root: Path): Map<String, RuntimeSkillDefinition> {
        val canonicalDirectory = canonicalInside(directory, root)
        val loaded = linkedMapOf<String, RuntimeSkillDefinition>()
        Files.list(canonicalDirectory).use { entries ->
            entries.filter { Files.isDirectory(it) }.forEach { skillDirectory ->
                val file = canonicalInside(skillDirectory.resolve("SKILL.md"), root)
                val text = readMarkdown(file)
                val id = Regex("\\*\\*Skill ID:\\*\\* `([a-z0-9-]+)`").find(text)?.groupValues?.get(1)
                    ?: throw RuntimeConfigurationException("Runtime skill metadata is missing.")
                if (loaded.containsKey(id)) throw RuntimeConfigurationException("Duplicate runtime skill id '$id'.")
                loaded[id] = RuntimeSkillDefinition(id, text, root.relativize(file).toString(), fingerprint(text))
            }
        }
        return loaded
    }

    private fun loadAgents(
        directory: Path,
        root: Path,
        skills: Map<String, RuntimeSkillDefinition>,
        modelProfileId: String,
    ): Map<String, RuntimeAgentDefinition> {
        val canonicalDirectory = canonicalInside(directory, root)
        val loaded = linkedMapOf<String, RuntimeAgentDefinition>()
        Files.list(canonicalDirectory).use { entries ->
            entries.filter { Files.isDirectory(it) }.forEach { agentDirectory ->
                val file = canonicalInside(agentDirectory.resolve("AGENT.md"), root)
                val text = readMarkdown(file)
                val id = agentDirectory.fileName.toString()
                if (id.isBlank() || loaded.containsKey(id)) throw RuntimeConfigurationException("Duplicate or blank agent id.")
                val skillIds = Regex("`([a-z0-9-]+)`").findAll(text).map { it.groupValues[1] }.filter { it in skills }.toSet()
                if (skillIds.isEmpty()) throw RuntimeConfigurationException("Agent '$id' has no approved runtime skill.")
                val allowedToolIds = Regex("(?m)^\\*\\*Permitted MCP tools:\\*\\* (.+)$").find(text)?.groupValues?.get(1)
                    ?.let { declaration -> Regex("`([a-z0-9_-]+)`").findAll(declaration).map { it.groupValues[1] }.toSet() }
                    ?: emptySet()
                val definition = AgentDefinition(
                    id = id,
                    displayName = text.lineSequence().firstOrNull { it.startsWith("# ") }?.removePrefix("# ") ?: id,
                    modelProfileId = modelProfileId,
                    agentInstructionPath = root.relativize(file).toString(),
                    allowedSkillPaths = skillIds.map { skills.getValue(it).source }.toSet(),
                    allowedToolIds = allowedToolIds,
                )
                loaded[id] = RuntimeAgentDefinition(definition, text, fingerprint(text))
            }
        }
        return loaded
    }

    private fun canonicalDirectory(path: Path): Path = try {
        path.toRealPath().also { require(Files.isDirectory(it)) { "Runtime configuration root is not a directory." } }
    } catch (_: Exception) { throw RuntimeConfigurationException("Runtime configuration root is missing or inaccessible.") }

    private fun canonicalInside(path: Path, root: Path): Path = try {
        path.toRealPath().also {
            if (!it.startsWith(root) || !Files.isRegularFile(it) && !Files.isDirectory(it)) {
                throw RuntimeConfigurationException("Runtime configuration resource is not approved.")
            }
        }
    } catch (error: RuntimeConfigurationException) { throw error
    } catch (_: Exception) { throw RuntimeConfigurationException("Runtime configuration resource is missing or inaccessible.") }

    private fun readMarkdown(file: Path): String {
        if (!file.fileName.toString().endsWith(".md") || Files.size(file) > 256 * 1024) {
            throw RuntimeConfigurationException("Runtime configuration resource is not valid Markdown.")
        }
        return Files.readString(file, StandardCharsets.UTF_8).takeIf { it.isNotBlank() }
            ?: throw RuntimeConfigurationException("Runtime configuration instructions are blank.")
    }

    private fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
