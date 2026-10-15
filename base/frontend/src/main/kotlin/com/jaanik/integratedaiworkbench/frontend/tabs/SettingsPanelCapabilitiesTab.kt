package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel
import javax.swing.border.TitledBorder

/** Capabilities: persistent role composition and a local document library. No permission grants here. */
class SettingsPanelCapabilitiesTab(private val context: WorkbenchFrontendContext) :
    JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))), RefreshableWorkbenchTab {

    private val home = Path.of(PathManager.getConfigPath()).resolve("integrated-ai-workbench")
    private val repository = WorkbenchLibrary(home, context.project.basePath?.let(Path::of))
    private val lock = Any()

    private val status = JBLabel("Role and document definitions do not grant execution permissions.")
    private val agentSummary = JBLabel("Select a default model profile to discover runtime agents.")
    private val roleListModel = DefaultListModel<WorkbenchLibrary.Role>()
    private val roleList = JBList(roleListModel).apply { selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val documentListModel = DefaultListModel<WorkbenchLibrary.Document>()
    private val documentList = JBList(documentListModel).apply { selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val assignedListModel = DefaultListModel<WorkbenchLibrary.Document>()
    private val assignedList = JBList(assignedListModel).apply { selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val sourceListModel = DefaultListModel<WorkbenchLibrary.Source>()
    private val sourceList = JBList(sourceListModel).apply { selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val roleName = JBTextField()
    private val purpose = JBTextArea(4, 45).apply { lineWrap = true; wrapStyleWord = true }
    private val responsibility = JBTextArea(4, 45).apply { lineWrap = true; wrapStyleWord = true }
    private val search = JBTextField().apply { emptyText.text = "Search names, categories and provenance" }
    private val category = JComboBox<String>()
    private val tabs = JTabbedPane()
    private var roles = emptyList<WorkbenchLibrary.Role>()
    private var documents = emptyList<WorkbenchLibrary.Document>()
    private var sources = emptyList<WorkbenchLibrary.Source>()
    private var loadingEditor = false
    private var runtimeRequest = 0L

    init {
        border = JBUI.Borders.empty(6)
        roleList.cellRenderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value?.let { "[${it.scope.name}] ${it.name} (${it.documentIds.size})" }.orEmpty()
        }
        documentList.cellRenderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value?.let { "[${it.scope.name}] ${it.category} · ${it.name}" }.orEmpty()
        }
        assignedList.cellRenderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value?.let { "${it.category} · ${it.name}" }.orEmpty()
        }
        sourceList.cellRenderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value?.let { "${it.status} · ${it.label} — ${it.uri}" }.orEmpty()
        }
        roleList.addListSelectionListener { if (!it.valueIsAdjusting) showRole() }
        tabs.addTab("Studio", studioPanel())
        tabs.addTab("Repository Library", libraryPanel())
        tabs.addTab("Sources", sourcesPanel())
        tabs.addTab("Runtime", runtimePanel())
        tabs.addChangeListener {
            if (tabs.selectedIndex == 2) reload(initializeSources = true)
        }
        add(tabs, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        search.document.addDocumentListener(documentListener { updateDocuments() })
        category.addActionListener { updateDocuments() }
        reload()
    }

    private fun studioPanel() = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
        add(JBLabel("Role composition · definitions only; authorize execution in Workflow."), BorderLayout.NORTH)
        val stack = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        val selection = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            border = TitledBorder("Roles")
            add(JBScrollPane(roleList).apply { preferredSize = Dimension(0, JBUI.scale(125)) }, BorderLayout.CENTER)
            add(
                buttons(
                    "New role" to ::newRole,
                    "Copy role" to ::copyRole,
                    "Delete role" to ::deleteRole
                ), BorderLayout.SOUTH
            )
        }
        stack.add(selection)
        val editor = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = TitledBorder("Role details")
            val form = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
            form.add(JBLabel("Role name")); form.add(roleName)
            form.add(JBLabel("Purpose")); form.add(JBScrollPane(purpose).apply {
            preferredSize = Dimension(0, JBUI.scale(90))
        })
            form.add(JBLabel("Responsibilities / scope"))
            form.add(JBScrollPane(responsibility).apply { preferredSize = Dimension(0, JBUI.scale(90)) })
            add(form, BorderLayout.CENTER)
            add(buttons("Save role details" to ::saveRole), BorderLayout.SOUTH)
        }
        stack.add(editor)
        val assignments = JPanel(BorderLayout(0, JBUI.scale(5))).apply {
            border = TitledBorder("Assigned library documents")
            add(JBScrollPane(assignedList).apply { preferredSize = Dimension(0, JBUI.scale(110)) }, BorderLayout.CENTER)
            add(
                buttons(
                    "Browse library" to { tabs.selectedIndex = 1 },
                    "Detach selected" to ::detachDocument
                ), BorderLayout.SOUTH
            )
        }
        stack.add(assignments)
        add(JBScrollPane(stack).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
    }

    private fun libraryPanel() = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
        val searchBar = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            add(search, BorderLayout.CENTER)
            add(category, BorderLayout.EAST)
        }
        add(searchBar, BorderLayout.NORTH)
        add(JBScrollPane(documentList), BorderLayout.CENTER)
        val south = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        south.add(
            buttons(
                "Create document" to { createDocument() },
                "Copy & modify" to ::cloneDocument,
                "Import file" to ::importFile,
                "View" to ::viewDocument
            )
        )
        south.add(
            buttons(
            "Assign / detach from role" to ::toggleAssignment,
            "Delete unassigned" to ::deleteDocument,
            "Browse AGENTS.md" to ::browseAgents,
            "New AGENTS.md" to { createDocument("AGENTS.md", "AGENTS.md") }
        ))
        south.add(JBLabel("Published content is immutable. Copy to create a modified version; project documents remain in the project tree."))
        add(south, BorderLayout.SOUTH)
    }

    private fun sourcesPanel() = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
        val header = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        header.add(JBLabel("Filesystem sources are checked on request; detected does not mean imported or executable."))
        header.add(JBLabel("IDE integration sources (not directly exportable without dedicated adapters):"))
        header.add(JBLabel("AI Assistant > Skills · Index MCP Server > Exposed Tools · MCP Server > Exposed Tools · Database > AI Tools"))
        header.add(JBLabel("Global library: ${home.toAbsolutePath().normalize()}"))
        header.add(
            JBLabel(
                "Project library: ${
                    context.project.basePath?.let {
                        Path.of(it).resolve(".ai-workbench").toString()
                    } ?: "No open project"
                }"))
        add(header, BorderLayout.NORTH)
        add(JBScrollPane(sourceList), BorderLayout.CENTER)
        add(
            buttons("Scan known locations" to ::scanSources, "Refresh list" to { reload(initializeSources = true) }),
            BorderLayout.SOUTH
        )
    }

    private fun runtimePanel() = JPanel(BorderLayout()).apply {
        border = TitledBorder("Runtime-discovered definitions")
        add(JBScrollPane(JBTextArea().apply {
            text = "Agents and skills come from the configured runtime, not from guessed IDE settings paths."
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }), BorderLayout.CENTER)
        add(agentSummary, BorderLayout.SOUTH)
    }

    private fun buttons(vararg actions: Pair<String, () -> Unit>) =
        JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4))).apply {
            actions.forEach { (title, handler) -> add(JButton(title).apply { addActionListener { handler() } }) }
        }

    private fun <T> background(action: String, task: () -> T, complete: (T) -> Unit = {}) {
        status.text = "$action…"
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { synchronized(lock) { task() } }
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { complete(it); status.text = "$action completed." },
                    onFailure = { status.text = "$action failed: ${it.message ?: it.javaClass.simpleName}" }
                )
            }
        }
    }

    private data class Snapshot(
        val roles: List<WorkbenchLibrary.Role>,
        val documents: List<WorkbenchLibrary.Document>,
        val sources: List<WorkbenchLibrary.Source>
    )

    private fun snapshot() = Snapshot(
        repository.roles(), repository.documents(),
        if (repository.databaseExists()) repository.sources() else emptyList()
    )

    private fun reload(initializeSources: Boolean = false) {
        background("Refresh library", {
            if (initializeSources) repository.initializeIfNeeded()
            snapshot()
        }, ::render)
    }

    private fun render(snapshot: Snapshot) {
        val roleId = roleList.selectedValue?.id
        val documentId = documentList.selectedValue?.id
        roles = snapshot.roles; documents = snapshot.documents; sources = snapshot.sources
        roleListModel.clear(); roles.forEach(roleListModel::addElement)
        roleList.setSelectedValue(roles.firstOrNull { it.id == roleId } ?: roles.firstOrNull(), true)
        sourceListModel.clear(); sources.forEach(sourceListModel::addElement)
        val previousCategory = category.selectedItem as? String ?: "All categories"
        category.model = DefaultComboBoxModel(
            (listOf("All categories") + documents.map { it.category }.distinct().sorted()).toTypedArray()
        )
        category.selectedItem =
            previousCategory.takeIf { it != "All categories" && documents.any { d -> d.category == it } }
                ?: "All categories"
        updateDocuments()
        documentList.setSelectedValue(documents.firstOrNull { it.id == documentId }, true)
        showRole()
    }

    private fun updateDocuments() {
        val selected = documentList.selectedValue?.id
        val q = search.text.trim()
        val selectedCategory = category.selectedItem as? String ?: "All categories"
        documentListModel.clear()
        documents.filter { d ->
            (selectedCategory == "All categories" || d.category == selectedCategory) &&
                    (q.isBlank() || listOf(d.name, d.category, d.source ?: "").any {
                        it.contains(
                            q,
                            ignoreCase = true
                        )
                    })
        }.forEach(documentListModel::addElement)
        documentList.setSelectedValue(documents.firstOrNull { it.id == selected }, true)
    }

    private fun showRole() {
        val selected = roleList.selectedValue
        loadingEditor = true
        try {
            roleName.text = selected?.name.orEmpty()
            purpose.text = selected?.purpose.orEmpty()
            responsibility.text = selected?.responsibility.orEmpty()
            listOf(roleName, purpose, responsibility).forEach { it.isEnabled = selected != null }
            assignedListModel.clear()
            documents.filter { selected?.documentIds?.contains(it.id) == true }.forEach(assignedListModel::addElement)
        } finally {
            loadingEditor = false
        }
    }

    private fun chooseScope(default: WorkbenchLibrary.Scope = WorkbenchLibrary.Scope.GLOBAL): WorkbenchLibrary.Scope? {
        val options = if (context.project.basePath == null) arrayOf("Global") else arrayOf("Global", "Project")
        val choice = JOptionPane.showInputDialog(
            this,
            "Where should the item be stored?",
            "Storage scope",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            if (default == WorkbenchLibrary.Scope.PROJECT && options.size > 1) "Project" else "Global"
        )
        return when (choice) {
            "Global" -> WorkbenchLibrary.Scope.GLOBAL; "Project" -> WorkbenchLibrary.Scope.PROJECT; else -> null
        }
    }

    private fun newRole() {
        val name = Messages.showInputDialog(context.project, "Role name", "New role", null)?.trim().orEmpty()
        if (name.isBlank()) return
        val scope = chooseScope() ?: return
        background("Create role", {
            val created = repository.createRole(name, "", "", scope)
            created.id to snapshot()
        }) { (id, data) -> render(data); roleList.setSelectedValue(roles.firstOrNull { it.id == id }, true) }
    }

    private fun saveRole() {
        val role = roleList.selectedValue ?: return
        if (loadingEditor) return
        val edited = role.copy(
            name = roleName.text.trim(),
            purpose = purpose.text.trim(),
            responsibility = responsibility.text.trim()
        )
        background("Save role", { repository.updateRole(edited); snapshot() }, ::render)
    }

    private fun copyRole() {
        val selected = roleList.selectedValue ?: return
        background("Copy role", {
            val created = repository.cloneRole(selected)
            created.id to snapshot()
        }) { (id, data) -> render(data); roleList.setSelectedValue(roles.firstOrNull { it.id == id }, true) }
    }

    private fun deleteRole() {
        val selected = roleList.selectedValue ?: return
        if (Messages.showYesNoDialog(
                context.project,
                "Delete '${selected.name}'? Assigned library documents will be preserved.", "Delete role", null
            ) != Messages.YES
        ) return
        background("Delete role", { repository.deleteRole(selected.id); snapshot() }, ::render)
    }

    private data class DocumentInput(
        val name: String,
        val category: String,
        val scope: WorkbenchLibrary.Scope,
        val content: String
    )

    private fun documentDialog(
        initial: WorkbenchLibrary.Document? = null, seed: String? = null,
        suggestedName: String = "", suggestedCategory: String = "DOCUMENT"
    ): DocumentInput? {
        val name = JBTextField(initial?.let { "${it.name} (copy)" } ?: suggestedName)
        val type = JBTextField(initial?.category ?: suggestedCategory)
        val scope = JComboBox(arrayOf("Global", "Project")).apply {
            if (context.project.basePath == null) removeItem("Project")
            selectedItem =
                if (initial?.scope == WorkbenchLibrary.Scope.PROJECT && context.project.basePath != null) "Project" else "Global"
        }
        val content = JBTextArea(15, 60).apply {
            lineWrap = true; wrapStyleWord = true
            text = seed
                ?: "# ${suggestedName.ifBlank { "Document title" }}\n\n## Purpose\n\n## Scope\n\n## Inputs\n\n## Outputs\n\n## Validation\n"
        }
        val form = JPanel(BorderLayout(JBUI.scale(6), JBUI.scale(6))).apply {
            val meta = JPanel(GridLayout(3, 2, JBUI.scale(6), JBUI.scale(6))).apply {
                add(JBLabel("Name")); add(name)
                add(JBLabel("Category (custom allowed)")); add(type)
                add(JBLabel("Scope")); add(scope)
            }
            add(meta, BorderLayout.NORTH)
            add(
                JBScrollPane(content).apply { preferredSize = Dimension(JBUI.scale(570), JBUI.scale(300)) },
                BorderLayout.CENTER
            )
            add(
                JBLabel("This creates a new immutable document; existing versions are never overwritten."),
                BorderLayout.SOUTH
            )
        }
        if (JOptionPane.showConfirmDialog(
                this,
                form,
                "New library document",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            ) != JOptionPane.OK_OPTION
        ) return null
        if (name.text.isBlank() || type.text.isBlank()) {
            status.text = "Name and category are required."
            return null
        }
        return DocumentInput(
            name.text.trim(), type.text.trim(),
            if (scope.selectedItem == "Project") WorkbenchLibrary.Scope.PROJECT else WorkbenchLibrary.Scope.GLOBAL,
            content.text
        )
    }

    private fun createDocument(name: String = "", suggestedCategory: String = "DOCUMENT") {
        val input = documentDialog(suggestedName = name, suggestedCategory = suggestedCategory) ?: return
        background("Create document", {
            val created = repository.createDocument(input.name, input.category, input.scope, input.content)
            created.id to snapshot()
        }) { (id, data) ->
            search.text = ""; category.selectedItem = "All categories"
            render(data)
            documentList.setSelectedValue(documents.firstOrNull { it.id == id }, true)
        }
    }

    private fun cloneDocument() {
        val original = documentList.selectedValue ?: return
        background("Load original for copying", { repository.content(original) }) { originalText ->
            val input = documentDialog(original, originalText) ?: return@background
            background("Create independent copy", {
                repository.createDocument(
                    input.name, input.category, input.scope, input.content,
                    source = "workbench:${original.id}", derivedFrom = original.id
                )
                snapshot()
            }, ::render)
        }
    }

    private fun importFile() {
        val chooser = JFileChooser(context.project.basePath).apply {
            dialogTitle = "Select a document to copy into the Workbench Library"
            fileSelectionMode = JFileChooser.FILES_ONLY
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile.toPath().toAbsolutePath().normalize()
        if (!Files.isRegularFile(file) || Files.size(file) > 2L * 1024 * 1024) {
            status.text = "Select a regular file smaller than 2 MB."
            return
        }
        if (Messages.showYesNoDialog(
                context.project,
                "Copy this file to the Workbench library? Review it for credentials or private data first.\n$file",
                "Import document", null
            ) != Messages.YES
        ) return
        val documentScope = chooseScope() ?: return
        val category = Messages.showInputDialog(
            context.project, "Document category (for example SKILL or SPEC)",
            "Import document", null
        )?.trim().orEmpty()
        if (category.isBlank()) return
        background("Import document", {
            val content = Files.readString(file, StandardCharsets.UTF_8)
            repository.createDocument(
                file.fileName.toString(),
                category,
                documentScope,
                content,
                source = file.toUri().toString()
            )
            snapshot()
        }, ::render)
    }

    private fun viewDocument() {
        val doc = documentList.selectedValue ?: return
        background("Load document", { repository.content(doc) }) { body ->
            val area = JBTextArea(body, 20, 68).apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
            JOptionPane.showMessageDialog(
                this, JBScrollPane(area).apply { preferredSize = Dimension(650, 400) },
                "${doc.category} · ${doc.name} (read-only)", JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun toggleAssignment() {
        val role = roleList.selectedValue ?: return
        val doc = documentList.selectedValue ?: return
        val attach = doc.id !in role.documentIds
        background(if (attach) "Assign document" else "Detach document", {
            repository.assign(role.id, doc.id, attach); snapshot()
        }, ::render)
    }

    private fun detachDocument() {
        val role = roleList.selectedValue ?: return
        val doc = assignedList.selectedValue ?: return
        background("Detach document", { repository.assign(role.id, doc.id, false); snapshot() }, ::render)
    }

    private fun deleteDocument() {
        val doc = documentList.selectedValue ?: return
        if (Messages.showYesNoDialog(
                context.project,
                "Permanently delete '${doc.name}'? Assigned documents cannot be deleted.",
                "Delete library document", null
            ) != Messages.YES
        ) return
        background("Delete document", { repository.deleteDocument(doc); snapshot() }, ::render)
    }

    private fun browseAgents() {
        val projectAgents = context.project.basePath?.let { Path.of(it).resolve("AGENTS.md") }
        val chooser = JFileChooser(context.project.basePath).apply {
            selectedFile = projectAgents?.toFile()
            dialogTitle = "Locate an existing AGENTS.md; importing will not change the original"
            fileSelectionMode = JFileChooser.FILES_ONLY
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile.toPath()
        if (!file.fileName.toString().equals("AGENTS.md", ignoreCase = true)) {
            status.text = "Selected file is not named AGENTS.md."
            return
        }
        if (Files.size(file) > 2L * 1024 * 1024) {
            status.text = "File exceeds the 2 MB limit."; return
        }
        val where = chooseScope(WorkbenchLibrary.Scope.PROJECT) ?: return
        background("Import AGENTS.md snapshot", {
            repository.createDocument(
                "AGENTS.md", "AGENTS.md", where,
                Files.readString(file, StandardCharsets.UTF_8), file.toUri().toString()
            )
            snapshot()
        }, ::render)
    }

    private fun scanSources() {
        background("Discover known local sources", { repository.discoverFileSources(); snapshot() }, ::render)
    }

    override fun refreshFromServices() {
        val selectedProfileId = context.settings.snapshot().defaultModelProfileId
        val request = ++runtimeRequest
        if (selectedProfileId.isBlank()) {
            agentSummary.text = "No default model profile is selected."
            return
        }
        agentSummary.text = "Discovering runtime agent definitions…"
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { context.workbench.agents(selectedProfileId) }
            ApplicationManager.getApplication().invokeLater {
                if (request != runtimeRequest) return@invokeLater
                agentSummary.text = result.fold(
                    onSuccess = { agents ->
                        if (agents.isEmpty()) "No runtime agents reported by selected profile."
                        else agents.joinToString(" · ") { "${it.displayName}: ${it.allowedToolIds.size} tool identifiers" }
                    },
                    onFailure = { "Runtime discovery failed: ${it.message ?: "Unknown error"}" }
                )
            }
        }
    }

    private fun documentListener(onChange: () -> Unit) = object : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
    }
}
