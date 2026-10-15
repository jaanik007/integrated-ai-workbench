package com.jaanik.integratedaiworkbench.frontend.tabs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

/** Persistence for the Capabilities UI. Files are authoritative; SQLite indexes metadata and relations. */
internal class WorkbenchLibrary(
    private val workbenchHome: Path,
    private val projectRoot: Path?
) {
    enum class Scope { GLOBAL, PROJECT }

    data class Role(
        val id: String,
        val name: String,
        val purpose: String,
        val responsibility: String,
        val scope: Scope,
        val projectKey: String?,
        val documentIds: Set<String> = emptySet()
    )

    data class Document(
        val id: String,
        val name: String,
        val category: String,
        val scope: Scope,
        val projectKey: String?,
        val source: String?,
        val derivedFrom: String?,
        val sha256: String,
        val createdAt: String
    )

    data class Source(
        val id: String,
        val label: String,
        val uri: String,
        val status: String,
        val detail: String,
        val checkedAt: String
    )

    private val root = workbenchHome.toAbsolutePath().normalize()
    private val dbPath = root.resolve("library.sqlite")
    private val projectKey = projectRoot?.toAbsolutePath()?.normalize()?.toString()
    private val maxDocumentBytes = 2L * 1024 * 1024

    fun databaseExists(): Boolean = Files.isRegularFile(dbPath)

    /** First source-list use or first write creates the database; listing roles alone does not. */
    @Synchronized
    fun initializeIfNeeded() {
        if (databaseExists()) return
        Files.createDirectories(root)
        connection(create = true).use { /* migrations are applied when connecting */ }
    }

    private fun connection(create: Boolean = false): Connection {
        if (!create && !databaseExists()) error("Workbench Library has not been initialized.")
        Files.createDirectories(root)
        Class.forName("org.sqlite.JDBC")
        val c = DriverManager.getConnection("jdbc:sqlite:${dbPath.toAbsolutePath()}")
        c.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys=ON")
            statement.execute("PRAGMA busy_timeout=3000")
            statement.execute("PRAGMA journal_mode=WAL")
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS roles (
                    id TEXT PRIMARY KEY, name TEXT NOT NULL, purpose TEXT NOT NULL,
                    responsibility TEXT NOT NULL, scope TEXT NOT NULL, project_key TEXT,
                    created_at TEXT NOT NULL, updated_at TEXT NOT NULL
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL,
                    scope TEXT NOT NULL, project_key TEXT, source TEXT, derived_from TEXT,
                    sha256 TEXT NOT NULL, created_at TEXT NOT NULL
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS assignments (
                    role_id TEXT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                    document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
                    PRIMARY KEY (role_id, document_id)
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS model_role_assignments (
                    project_key TEXT NOT NULL,
                    model_id TEXT NOT NULL,
                    role_id TEXT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                    PRIMARY KEY (project_key, model_id, role_id)
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS sources (
                    id TEXT PRIMARY KEY, label TEXT NOT NULL, uri TEXT NOT NULL,
                    status TEXT NOT NULL, detail TEXT NOT NULL, checked_at TEXT NOT NULL
                )
            """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS activity (
                    id TEXT PRIMARY KEY, event TEXT NOT NULL, target_id TEXT,
                    recorded_at TEXT NOT NULL, detail TEXT NOT NULL
                )
            """.trimIndent()
            )
        }
        return c
    }

    private fun log(c: Connection, action: String, target: String?, detail: String) {
        c.prepareStatement("INSERT INTO activity VALUES (?,?,?,?,?)").use { p ->
            p.setString(1, UUID.randomUUID().toString()); p.setString(2, action)
            p.setString(3, target); p.setString(4, Instant.now().toString()); p.setString(5, detail)
            p.executeUpdate()
        }
    }

    private fun projectClause(alias: String = "") =
        "(${alias}scope='GLOBAL' OR (${alias}scope='PROJECT' AND ${alias}project_key=?))"

    fun roles(): List<Role> {
        if (!databaseExists()) return emptyList()
        connection().use { c ->
            val results = mutableListOf<Role>()
            c.prepareStatement("SELECT * FROM roles WHERE ${projectClause()} ORDER BY name COLLATE NOCASE").use { p ->
                p.setString(1, projectKey)
                p.executeQuery().use { rs ->
                    while (rs.next()) results.add(
                        Role(
                            rs.getString("id"), rs.getString("name"), rs.getString("purpose"),
                            rs.getString("responsibility"), Scope.valueOf(rs.getString("scope")),
                            rs.getString("project_key")
                        )
                    )
                }
            }
            return results.map { it.copy(documentIds = assignments(c, it.id)) }
        }
    }

    /** The active project has its own assignments; global roles remain selectable everywhere. */
    fun modelRoleAssignments(modelIds: Set<String>): Map<String, Set<String>> {
        if (modelIds.isEmpty() || !databaseExists()) return emptyMap()
        val result = linkedMapOf<String, MutableSet<String>>()
        connection().use { c ->
            c.prepareStatement(
                """
                SELECT m.model_id, m.role_id FROM model_role_assignments m
                JOIN roles r ON r.id = m.role_id
                WHERE m.project_key=? AND ${projectClause("r.")}
            """.trimIndent()
            ).use { p ->
                p.setString(1, projectKey.orEmpty())
                p.setString(2, projectKey)
                p.executeQuery().use { rs ->
                    while (rs.next()) {
                        val modelId = rs.getString(1)
                        if (modelId in modelIds) result.getOrPut(modelId) { linkedSetOf() }.add(rs.getString(2))
                    }
                }
            }
        }
        return result
    }

    /** Atomically replaces per-model role bindings. No role grants execution permissions. */
    fun saveModelRoleAssignments(assignments: Map<String, Set<String>>, removedModelIds: Set<String>) {
        if (!databaseExists() && assignments.values.all { it.isEmpty() }) return
        require(databaseExists()) { "Create a role before assigning it to a model." }
        val validRoleIds = roles().mapTo(hashSetOf()) { it.id }
        require(assignments.values.flatten().all { it in validRoleIds }) {
            "Assigned role was removed or is not available in this project; refresh the Models tab."
        }
        connection().use { c ->
            c.autoCommit = false
            try {
                val targets = assignments.keys + removedModelIds
                c.prepareStatement("DELETE FROM model_role_assignments WHERE project_key=? AND model_id=?").use { p ->
                    for (modelId in targets) {
                        p.setString(1, projectKey.orEmpty())
                        p.setString(2, modelId)
                        p.addBatch()
                    }
                    p.executeBatch()
                }
                c.prepareStatement("INSERT INTO model_role_assignments(project_key,model_id,role_id) VALUES(?,?,?)")
                    .use { p ->
                        assignments.forEach { (modelId, roleIds) ->
                            roleIds.forEach { roleId ->
                                p.setString(1, projectKey.orEmpty())
                                p.setString(2, modelId)
                                p.setString(3, roleId)
                                p.addBatch()
                            }
                        }
                        p.executeBatch()
                    }
                log(
                    c, "models.roles.saved", null,
                    "${assignments.size} models, ${assignments.values.sumOf { it.size }} role assignments"
                )
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    private fun assignments(c: Connection, roleId: String): Set<String> {
        val ids = linkedSetOf<String>()
        c.prepareStatement("SELECT document_id FROM assignments WHERE role_id=?").use { p ->
            p.setString(1, roleId)
            p.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getString(1)) }
        }
        return ids
    }

    fun createRole(name: String, purpose: String, responsibility: String, scope: Scope): Role {
        require(name.isNotBlank()) { "Role name is required." }
        require(scope != Scope.PROJECT || projectKey != null) { "No project is open." }
        initializeIfNeeded()
        val role = Role(
            UUID.randomUUID().toString(), name.trim(), purpose.trim(), responsibility.trim(), scope,
            if (scope == Scope.PROJECT) projectKey else null
        )
        connection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("INSERT INTO roles VALUES (?,?,?,?,?,?,?,?)").use { p ->
                    val time = Instant.now().toString()
                    listOf(
                        role.id, role.name, role.purpose, role.responsibility, role.scope.name,
                        role.projectKey, time, time
                    ).forEachIndexed { i, s -> p.setString(i + 1, s) }
                    p.executeUpdate()
                }
                log(c, "role.created", role.id, role.name)
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        mirrorProjectRole(role)
        return role
    }

    fun updateRole(role: Role) {
        require(role.name.isNotBlank()) { "Role name is required." }
        connection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("UPDATE roles SET name=?,purpose=?,responsibility=?,updated_at=? WHERE id=? AND ${projectClause()}")
                    .use { p ->
                        p.setString(1, role.name.trim()); p.setString(2, role.purpose.trim())
                        p.setString(3, role.responsibility.trim()); p.setString(4, Instant.now().toString())
                        p.setString(5, role.id); p.setString(6, projectKey)
                        require(p.executeUpdate() == 1) { "Role was not found in the active workspace." }
                    }
                log(c, "role.updated", role.id, role.name)
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        mirrorProjectRoleById(role.id)
    }

    fun cloneRole(source: Role): Role {
        val cloned = createRole("${source.name} (copy)", source.purpose, source.responsibility, source.scope)
        source.documentIds.forEach { assign(cloned.id, it, true) }
        return cloned
    }

    fun deleteRole(id: String) {
        if (!databaseExists()) return
        val projectScoped = roles().any { it.id == id && it.scope == Scope.PROJECT }
        connection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM roles WHERE id=? AND ${projectClause()}").use { p ->
                    p.setString(1, id); p.setString(2, projectKey)
                    require(p.executeUpdate() == 1) { "Role was not found." }
                }
                log(c, "role.deleted", id, "Role and its assignments removed; documents retained")
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        if (projectScoped) Files.deleteIfExists(projectRoleFile(id))
    }

    fun documents(): List<Document> {
        if (!databaseExists()) return emptyList()
        connection().use { c ->
            val result = mutableListOf<Document>()
            c.prepareStatement("SELECT * FROM documents WHERE ${projectClause()} ORDER BY category,name COLLATE NOCASE")
                .use { p ->
                    p.setString(1, projectKey)
                    p.executeQuery().use { rs -> while (rs.next()) result.add(rs.toDocument()) }
                }
            return result
        }
    }

    private fun ResultSet.toDocument() = Document(
        getString("id"), getString("name"), getString("category"),
        Scope.valueOf(getString("scope")), getString("project_key"),
        getString("source"), getString("derived_from"), getString("sha256"), getString("created_at")
    )

    private fun fileOf(doc: Document): Path {
        require(Regex("[0-9a-fA-F-]{36}").matches(doc.id)) { "Invalid document ID." }
        val base = if (doc.scope == Scope.GLOBAL) root.resolve("documents") else {
            require(doc.projectKey == projectKey && projectRoot != null) { "Document is from a different project." }
            projectRoot.resolve(".ai-workbench").resolve("library")
        }
        val path = base.resolve("${doc.id}.md").normalize()
        require(path.startsWith(base.normalize())) { "Document path escapes library." }
        return path
    }

    fun content(doc: Document): String {
        val file = fileOf(doc)
        require(Files.size(file) <= maxDocumentBytes) { "Document exceeds the 2 MB limit." }
        val bytes = Files.readAllBytes(file)
        require(sha256(bytes) == doc.sha256) { "Document was changed outside Workbench; import it as a new revision." }
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun createDocument(
        name: String, category: String, scope: Scope, content: String,
        source: String? = null, derivedFrom: String? = null
    ): Document {
        require(name.isNotBlank()) { "Document name is required." }
        require(category.isNotBlank() && category.length <= 64) { "Category must be 1–64 characters." }
        require(scope != Scope.PROJECT || projectKey != null) { "No project is open." }
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= maxDocumentBytes) { "Document exceeds the 2 MB limit." }
        initializeIfNeeded()
        val doc = Document(
            UUID.randomUUID().toString(), name.trim(), category.trim(), scope,
            if (scope == Scope.PROJECT) projectKey else null, source, derivedFrom,
            sha256(bytes), Instant.now().toString()
        )
        val file = fileOf(doc)
        Files.createDirectories(file.parent)
        val temp = Files.createTempFile(file.parent, "workbench-", ".tmp")
        try {
            Files.write(temp, bytes)
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp, file)
            }
            mirrorProjectDocument(doc)
            connection().use { c ->
                c.autoCommit = false
                try {
                    c.prepareStatement("INSERT INTO documents VALUES (?,?,?,?,?,?,?,?,?)").use { p ->
                        listOf(
                            doc.id, doc.name, doc.category, doc.scope.name, doc.projectKey,
                            doc.source, doc.derivedFrom, doc.sha256, doc.createdAt
                        )
                            .forEachIndexed { i, s -> p.setString(i + 1, s) }
                        p.executeUpdate()
                    }
                    log(c, "document.created", doc.id, "${doc.category}: ${doc.name}; source=${doc.source ?: "new"}")
                    c.commit()
                } catch (e: Exception) {
                    c.rollback(); throw e
                }
            }
        } catch (e: Exception) {
            Files.deleteIfExists(file)
            if (doc.scope == Scope.PROJECT) Files.deleteIfExists(documentMetadataFile(doc))
            throw e
        } finally {
            Files.deleteIfExists(temp)
        }
        return doc
    }

    fun assign(roleId: String, documentId: String, attached: Boolean) {
        connection().use { c ->
            c.autoCommit = false
            try {
                val role = c.prepareStatement("SELECT scope,project_key FROM roles WHERE id=?").use { p ->
                    p.setString(1, roleId); p.executeQuery().use { rs ->
                    require(rs.next()) { "Role was not found." }
                    rs.getString(1) to rs.getString(2)
                }
                }
                val doc = c.prepareStatement("SELECT scope,project_key FROM documents WHERE id=?").use { p ->
                    p.setString(1, documentId); p.executeQuery().use { rs ->
                    require(rs.next()) { "Document was not found." }
                    rs.getString(1) to rs.getString(2)
                }
                }
                require(role.first != "GLOBAL" || doc.first == "GLOBAL") { "Global roles cannot depend on project-local documents." }
                require(doc.first != "PROJECT" || role.second == doc.second) { "Cannot cross project boundaries." }
                if (attached) {
                    c.prepareStatement("INSERT OR IGNORE INTO assignments VALUES (?,?)").use { p ->
                        p.setString(1, roleId); p.setString(2, documentId); p.executeUpdate()
                    }
                } else {
                    c.prepareStatement("DELETE FROM assignments WHERE role_id=? AND document_id=?").use { p ->
                        p.setString(1, roleId); p.setString(2, documentId); p.executeUpdate()
                    }
                }
                log(c, if (attached) "document.attached" else "document.detached", documentId, roleId)
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        mirrorProjectRoleById(roleId)
    }

    fun deleteDocument(document: Document) {
        connection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM documents WHERE id=? AND ${projectClause()}").use { p ->
                    p.setString(1, document.id); p.setString(2, projectKey)
                    require(p.executeUpdate() == 1) { "Document is assigned to a role or is not available here." }
                }
                log(c, "document.deleted", document.id, document.name)
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        // If deletion fails, the orphan file remains recoverable; metadata never points to missing content.
        Files.deleteIfExists(fileOf(document))
        if (document.scope == Scope.PROJECT) Files.deleteIfExists(documentMetadataFile(document))
    }

    fun sources(): List<Source> {
        initializeIfNeeded()
        connection().use { c ->
            val result = mutableListOf<Source>()
            c.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM sources ORDER BY label").use { rs ->
                    while (rs.next()) result.add(
                        Source(
                            rs.getString("id"), rs.getString("label"), rs.getString("uri"),
                            rs.getString("status"), rs.getString("detail"), rs.getString("checked_at")
                        )
                    )
                }
            }
            return result
        }
    }

    /** Source adapters use known filesystem locations only. IntelliJ plugin settings are NOT scraped. */
    fun discoverFileSources(): List<Source> {
        initializeIfNeeded()
        val home = Path.of(System.getProperty("user.home"))
        val locations = mutableListOf<Pair<String, Path>>()
        for (relative in listOf(".gemini/skills", ".agents/skills", ".codex/skills", ".claude/skills")) {
            locations.add("User $relative" to home.resolve(relative))
            if (projectRoot != null) locations.add("Project $relative" to projectRoot.resolve(relative))
        }
        if (projectRoot != null) locations.add("Project AGENTS.md" to projectRoot.resolve("AGENTS.md"))
        val timestamp = Instant.now().toString()
        val results = locations.map { (label, path) ->
            val normalized = path.toAbsolutePath().normalize()
            Source(
                "file:${sha256(normalized.toString().toByteArray()).take(24)}", label,
                normalized.toUri().toString(), if (Files.exists(normalized)) "DETECTED" else "NOT_FOUND",
                if (Files.isDirectory(normalized)) "Directory found; individual files require explicit import"
                else if (Files.isRegularFile(normalized)) "File found; import requires user selection"
                else "Not found", timestamp
            )
        }
        connection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("INSERT OR REPLACE INTO sources VALUES (?,?,?,?,?,?)").use { p ->
                    results.forEach { s ->
                        p.setString(1, s.id); p.setString(2, s.label); p.setString(3, s.uri)
                        p.setString(4, s.status); p.setString(5, s.detail); p.setString(6, s.checkedAt)
                        p.addBatch()
                    }
                    p.executeBatch()
                }
                log(c, "sources.scanned", null, "${results.count { it.status == "DETECTED" }} locations detected")
                c.commit()
            } catch (e: Exception) {
                c.rollback(); throw e
            }
        }
        return results
    }

    private fun documentMetadataFile(document: Document): Path =
        fileOf(document).resolveSibling("${document.id}.properties")

    private fun mirrorProjectDocument(document: Document) {
        if (document.scope != Scope.PROJECT) return
        val properties = Properties().apply {
            setProperty("format", "integrated-ai-workbench.document.v1")
            setProperty("id", document.id)
            setProperty("name", document.name)
            setProperty("category", document.category)
            setProperty("sha256", document.sha256)
            setProperty("source", document.source.orEmpty())
            setProperty("derivedFrom", document.derivedFrom.orEmpty())
            setProperty("createdAt", document.createdAt)
        }
        val destination = documentMetadataFile(document)
        Files.newBufferedWriter(destination, StandardCharsets.UTF_8).use {
            properties.store(it, "Project-local Workbench document index")
        }
    }

    private fun projectRoleFile(id: String): Path {
        require(Regex("[0-9a-fA-F-]{36}").matches(id)) { "Invalid role ID." }
        val base = requireNotNull(projectRoot) { "No open project." }
        return base.resolve(".ai-workbench").resolve("roles").resolve("$id.properties")
    }

    /** Project-scoped roles have a human-inspectable file in the project tree. */
    private fun mirrorProjectRole(role: Role) {
        if (role.scope != Scope.PROJECT) return
        val file = projectRoleFile(role.id)
        val props = Properties().apply {
            setProperty("format", "integrated-ai-workbench.role.v1")
            setProperty("id", role.id)
            setProperty("name", role.name)
            setProperty("purpose", role.purpose)
            setProperty("responsibility", role.responsibility)
            setProperty("documentIds", role.documentIds.joinToString(","))
        }
        Files.createDirectories(file.parent)
        val temporary = Files.createTempFile(file.parent, "role-", ".tmp")
        try {
            Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)
                .use { props.store(it, "Project-local Workbench role") }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun mirrorProjectRoleById(id: String) {
        roles().firstOrNull { it.id == id }?.let(::mirrorProjectRole)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
