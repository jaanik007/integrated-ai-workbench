package com.jaanik.integratedaiworkbench.frontend.tabs

import com.jaanik.integratedaiworkbench.backend.mcp.JsonValue
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValueCodec
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Project-contained, inspectable workflow files. Never stored as session-only simulations. */
internal class WorkbenchWorkflowStore(projectPath: String?) {
    val projectRoot: Path? = projectPath?.let { Path.of(it).toAbsolutePath().normalize() }
    private val directory = projectRoot?.resolve(".ai-workbench/workflows")

    data class Step(
        var id: String = UUID.randomUUID().toString(),
        var title: String = "New operation",
        var type: String = "AI task",
        var roleId: String = "",
        var modelId: String = "",
        var providerId: String = "",
        var providerModelId: String = "",
        var instruction: String = "",
        var dependsOn: String = "",
        var contractId: String = "",
        var evidenceRequired: Boolean = false,
        var evidencePath: String = "",
        var evidenceTemplateId: String = "",
    )

    data class Workflow(
        var id: String = UUID.randomUUID().toString(),
        var name: String = "New workflow",
        val steps: MutableList<Step> = mutableListOf(),
    ) {
        override fun toString(): String = name
    }

    fun all(): List<Workflow> {
        val dir = directory ?: return emptyList()
        if (!Files.isDirectory(dir)) return emptyList()
        return Files.newDirectoryStream(dir, "*.json").use { stream ->
            stream.mapNotNull { path -> runCatching { read(Files.readString(path)) }.getOrNull() }
                .sortedBy { it.name.lowercase() }.toList()
        }
    }

    fun save(workflow: Workflow) {
        val dir = directory ?: error("Open a project to save a workflow.")
        require(workflow.name.isNotBlank()) { "Workflow name is required." }
        require(workflow.steps.map { it.id }.distinct().size == workflow.steps.size) { "Duplicate operation IDs." }
        val file = target(workflow.id)
        Files.createDirectories(dir)
        val temp = Files.createTempFile(dir, ".workflow-", ".tmp")
        try {
            Files.writeString(temp, serialize(workflow), StandardCharsets.UTF_8)
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    fun delete(workflow: Workflow) {
        Files.deleteIfExists(target(workflow.id))
    }

    private fun target(id: String): Path {
        val dir = directory ?: error("Open a project first.")
        require(runCatching { UUID.fromString(id) }.isSuccess) { "Invalid workflow ID." }
        return dir.resolve("$id.json")
    }

    fun serialize(w: Workflow): String = """{
  "format":"workbench-workflow-v1",
  "id":${q(w.id)},"name":${q(w.name)},
  "steps":[${
        w.steps.joinToString(",") { s ->
            """{
    "id":${q(s.id)},"title":${q(s.title)},"type":${q(s.type)},
    "roleId":${q(s.roleId)},"modelId":${q(s.modelId)},"providerId":${q(s.providerId)},
    "providerModelId":${q(s.providerModelId)},"instruction":${q(s.instruction)},
    "dependsOn":${q(s.dependsOn)},"contractId":${q(s.contractId)},
    "evidenceRequired":${q(s.evidenceRequired.toString())},
    "evidencePath":${q(s.evidencePath)},"evidenceTemplateId":${q(s.evidenceTemplateId)}
  }"""
        }
    }]
}"""

    fun read(text: String): Workflow {
        val obj = JsonValueCodec.parse(text) as? JsonValue.ObjectValue ?: error("Expected workflow JSON object.")
        val stepsArray = obj.values["steps"] as? JsonValue.ArrayValue ?: error("Workflow has no steps array.")
        require(stepsArray.values.size <= 200) { "Maximum 200 operations per workflow." }
        val steps = stepsArray.values.map { v ->
            val o = v as? JsonValue.ObjectValue ?: error("Invalid operation.")
            Step(
                id = o.string("id")?.takeIf { runCatching { UUID.fromString(it) }.isSuccess } ?: UUID.randomUUID()
                    .toString(),
                title = o.string("title").orEmpty(),
                type = o.string("type") ?: "AI task",
                roleId = o.string("roleId").orEmpty(),
                modelId = o.string("modelId").orEmpty(),
                providerId = o.string("providerId").orEmpty(),
                providerModelId = o.string("providerModelId").orEmpty(),
                instruction = o.string("instruction").orEmpty(),
                dependsOn = o.string("dependsOn").orEmpty(),
                contractId = o.string("contractId").orEmpty(),
                evidenceRequired = o.string("evidenceRequired") == "true",
                evidencePath = o.string("evidencePath").orEmpty(),
                evidenceTemplateId = o.string("evidenceTemplateId").orEmpty()
            )
        }.toMutableList()
        return Workflow(obj.string("id")?.takeIf { runCatching { UUID.fromString(it) }.isSuccess } ?: UUID.randomUUID()
            .toString(), obj.string("name") ?: "Imported workflow", steps)
    }

    fun importText(name: String, text: String): Workflow {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("{")) return read(text).copy(id = UUID.randomUUID().toString())
        val lines = text.lines()
        val parsed = mutableListOf<Step>()
        var current: Step? = null
        val header = Regex("^(?:#{1,4}\\s+|(?:\\d+[.)]|[-*])\\s+)(.+)$")
        for (line in lines) {
            val found = header.find(line.trim())
            if (found != null) {
                current = Step(title = found.groupValues[1].trim())
                parsed += current
            } else if (current != null) {
                val role =
                    Regex("^\\s*(?:Role|Responsible role|Vastutav roll)\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE).find(
                        line
                    )
                if (role != null) current.instruction += "\nRole: ${role.groupValues[1].trim()}"
                else if (line.isNotBlank()) current.instruction += (if (current.instruction.isBlank()) "" else "\n") + line.trim()
            }
        }
        require(parsed.isNotEmpty()) { "No recognizable headings or numbered/list operations found. Review the source document." }
        require(parsed.size <= 200) { "Too many operations (max 200)." }
        return Workflow(name = name.substringBeforeLast('.').ifBlank { "Imported workflow" }, steps = parsed)
    }

    fun safeEvidenceTarget(relative: String): Path {
        val root = projectRoot ?: error("Open a project to save evidence.")
        require(relative.isNotBlank()) { "Evidence output path required." }
        val path = Path.of(relative)
        require(!path.isAbsolute) { "Evidence path must be relative to the project." }
        val target = root.resolve(path).normalize()
        require(target.startsWith(root) && target != root) { "Evidence path escapes project." }
        // Prevent escaping through an existing symlink in parent directories.
        var cursor = target.parent
        while (cursor != null && cursor.startsWith(root)) {
            require(!Files.isSymbolicLink(cursor)) { "Evidence path may not traverse symbolic links." }
            if (cursor == root) break
            cursor = cursor.parent
        }
        require(!Files.exists(target)) { "Evidence file already exists: $relative (no overwrite)." }
        return target
    }

    private fun q(s: String): String = buildString {
        append('"'); for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
        else -> if (c.code < 32) append("\\u%04x".format(c.code)) else append(c)
    }; append('"')
    }
}
