package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.backend.provider.PersistedProviderRecord
import com.jaanik.integratedaiworkbench.backend.provider.ProviderConnectionStatus
import com.jaanik.integratedaiworkbench.backend.provider.ProviderDiscoveryService
import com.jaanik.integratedaiworkbench.backend.runtime.InferenceCancellation
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.TitledBorder

/** Real execution only. No simulated roles, workflow steps, outputs or permissions. */
class SettingsPanelExecuteTab(private val context: WorkbenchFrontendContext) :
    JPanel(BorderLayout(0, JBUI.scale(8))), RefreshableWorkbenchTab, Disposable {

    private enum class Route(val title: String) {
        ROLE("By role"), LOCAL("By local model"), PROVIDER("By external provider");

        override fun toString() = title
    }

    private data class RoleChoice(val role: WorkbenchLibrary.Role?) {
        override fun toString() = role?.name ?: "No role / direct execution"
    }

    private data class ProviderChoice(val provider: VerifiedExecutionSources.Provider?) {
        override fun toString() = provider?.record?.displayName ?: "Integrated llama.cpp (verified)"
    }

    private data class DocumentChoice(val document: WorkbenchLibrary.Document?) {
        override fun toString() = document?.let { "${it.category} · ${it.name}" } ?: "No library system document"
    }

    private sealed interface Destination {
        data class Local(val model: ModelProfile) : Destination
        data class Remote(val provider: PersistedProviderRecord, val modelId: String) : Destination
    }

    private data class Selection(val destination: Destination, val system: String, val roleName: String?)

    private val repository = WorkbenchLibrary(
        Path.of(PathManager.getConfigPath()).resolve("integrated-ai-workbench"),
        context.project.basePath?.let(Path::of)
    )
    private val worker = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val routeCombo = ComboBox(Route.entries.toTypedArray())
    private val roleCombo = ComboBox<RoleChoice>()
    private val modelCombo = ComboBox<ModelProfile>()
    private val providerCombo = ComboBox<ProviderChoice>()
    private val providerModelId = ComboBox<String>().apply { isEnabled = false }
    private val systemDocumentCombo = ComboBox<DocumentChoice>()
    private val systemArea = JBTextArea(3, 55).apply { lineWrap = true; wrapStyleWord = true }
    private val behaviorArea = JBTextArea(2, 55).apply { lineWrap = true; wrapStyleWord = true }
    private val promptArea = JBTextArea(4, 55).apply { lineWrap = true; wrapStyleWord = true }
    private val resultArea = JBTextArea(8, 55).apply { lineWrap = true; wrapStyleWord = true; isEditable = false }
    private val summary = JBLabel("Select a real execution target.")
    private val status = JBLabel("Ready")
    private val sendButton = JButton("Post")
    private val cancelButton = JButton("Cancel").apply { isEnabled = false }
    private val chatButton = JButton("Open floating chat")
    private val reloadButton = JButton("Refresh sources")
    private val generation = AtomicLong()
    private var busy = false
    @Volatile
    private var localCancellation: InferenceCancellation? = null
    @Volatile
    private var remoteCancellation: ProviderInferenceClient.Cancellation? = null
    private var refreshing = false
    private var sources = VerifiedExecutionSources.Snapshot(emptyList(), emptyList(), emptyList())
    private val workflows = WorkbenchWorkflowPanel(context, repository)
    private val sourceTimer = javax.swing.Timer(30_000) { if (isShowing && !busy && !refreshing) refreshFromServices() }
    private var providerCatalog: Map<String, List<String>> = emptyMap()
    private val statusUpdate: (String) -> Unit = { status.text = it; status.toolTipText = it }

    init {
        border = JBUI.Borders.empty(4)
        add(JBTabbedPane().apply {
            addTab("Single execution", JPanel(BorderLayout(0, JBUI.scale(8))).apply {
                add(configurationPanel(), BorderLayout.NORTH)
                add(executionPanel(), BorderLayout.CENTER)
                add(footer(), BorderLayout.SOUTH)
            })
            addTab("Workflow designer", workflows)
        }, BorderLayout.CENTER)
        routeCombo.addActionListener {
            if (!refreshing) {
                proposeRoleModel(); updateDisplay()
            }
        }
        roleCombo.addActionListener {
            if (!refreshing) {
                proposeRoleModel(); updateDisplay()
            }
        }
        modelCombo.addActionListener { if (!refreshing) updateDisplay() }
        providerCombo.addActionListener {
            if (!refreshing) {
                reloadProviderModels()
                updateDisplay()
            }
        }
        providerModelId.addActionListener { if (!refreshing) updateDisplay() }
        sendButton.addActionListener { submit() }
        cancelButton.addActionListener { localCancellation?.cancel(); remoteCancellation?.cancel(); statusUpdate("Cancellation requested…") }
        chatButton.addActionListener { openChat() }
        reloadButton.addActionListener { refreshFromServices() }
        // In single execution Ctrl+Enter submits; plain Enter remains available for multi-line prompts.
        promptArea.inputMap.put(javax.swing.KeyStroke.getKeyStroke("ctrl ENTER"), "execute-post")
        promptArea.actionMap.put("execute-post", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) = submit()
        })
        sourceTimer.start()
        refreshFromServices()
    }

    private fun configurationPanel(): JPanel = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
        border = TitledBorder("Execution target and behavior")
        add(JPanel(GridBagLayout()).apply {
            fun row(line: Int, label: String, component: Component) {
                val l = GridBagConstraints().apply {
                    gridx = 0; gridy = line; weightx = 0.0; anchor = GridBagConstraints.WEST
                    insets = JBUI.insets(3); fill = GridBagConstraints.NONE
                }
                add(JBLabel(label), l)
                val r = GridBagConstraints().apply {
                    gridx = 1; gridy = line; weightx = 1.0; anchor = GridBagConstraints.WEST
                    insets = JBUI.insets(3); fill = GridBagConstraints.HORIZONTAL
                }
                add(component, r)
            }
            row(0, "Project:", JBLabel(context.project.basePath ?: "No project selected"))
            row(1, "Role:", roleCombo)
            row(2, "Selection mode:", routeCombo)
            row(3, "Local model:", modelCombo)
            row(4, "External provider:", providerCombo)
            row(5, "Provider model:", providerModelId)
            row(6, "System document:", systemDocumentCombo)
            row(7, "Additional system instructions:", JBScrollPane(systemArea))
            row(8, "Behavior / response preferences:", JBScrollPane(behaviorArea))
        }, BorderLayout.CENTER)
        add(JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(3)
            add(summary, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                add(reloadButton)
            }, BorderLayout.EAST)
        }, BorderLayout.SOUTH)
    }

    private fun executionPanel(): JPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        border = TitledBorder("Single execution — actual inference")
        add(JPanel(BorderLayout(0, JBUI.scale(3))).apply {
            add(JBLabel("Prompt (Ctrl+Enter to post)"), BorderLayout.NORTH)
            add(JBScrollPane(promptArea), BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        add(JPanel(BorderLayout(0, JBUI.scale(3))).apply {
            add(JBLabel("Actual response / failure"), BorderLayout.NORTH)
            add(JBScrollPane(resultArea), BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    private fun footer(): JPanel = JPanel(BorderLayout(8, 0)).apply {
        border = BorderFactory.createEmptyBorder(4, 2, 4, 2)
        add(status, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
            add(chatButton); add(cancelButton); add(sendButton)
        }, BorderLayout.EAST)
    }

    override fun refreshFromServices() {
        if (busy || refreshing) return
        refreshing = true
        statusUpdate("Scanning available local models and providers…")
        // PasswordSafe, SQLite and provider HTTP probes run off the Swing EDT.
        worker.launch {
            val scan = runCatching { VerifiedExecutionSources.scan(context) }
            val roles = runCatching { repository.roles() }.getOrElse { emptyList() }
            val documents = runCatching { repository.documents() }.getOrElse { emptyList() }
            val registered = runCatching { context.modelRegistry.registeredModels() }.getOrElse { emptyList() }
            val assignments =
                runCatching { repository.modelRoleAssignments(registered.mapTo(linkedSetOf()) { it.modelId }) }.getOrElse { emptyMap() }
            SwingUtilities.invokeLater {
                refreshing = false
                scan.onFailure { statusUpdate("Source scan failed: ${it.message}") }
                val snapshot = scan.getOrNull() ?: return@invokeLater
                val previousRole = (roleCombo.selectedItem as? RoleChoice)?.role?.id
                val previousModel = (modelCombo.selectedItem as? ModelProfile)?.id
                val previousProvider = (providerCombo.selectedItem as? ProviderChoice)?.provider?.record?.id
                val previousSystem = (systemDocumentCombo.selectedItem as? DocumentChoice)?.document?.id
                val previousProviderModel = providerModelId.selectedItem as? String
                sources = snapshot
                providerCatalog = snapshot.providers.associate { it.record.id to it.models }
                refreshing = true
                try {
                    roleCombo.removeAllItems()
                    roleCombo.addItem(RoleChoice(null))
                    roles.forEach { roleCombo.addItem(RoleChoice(it)) }
                    if (previousRole != null) for (i in 0 until roleCombo.itemCount) {
                        if (roleCombo.getItemAt(i).role?.id == previousRole) roleCombo.selectedIndex = i
                    }
                    providerCombo.removeAllItems()
                    if (snapshot.localModels.isNotEmpty()) providerCombo.addItem(ProviderChoice(null))
                    snapshot.providers.forEach { providerCombo.addItem(ProviderChoice(it)) }
                    for (i in 0 until providerCombo.itemCount) {
                        if (providerCombo.getItemAt(i).provider?.record?.id == previousProvider) providerCombo.selectedIndex =
                            i
                    }
                    systemDocumentCombo.removeAllItems()
                    systemDocumentCombo.addItem(DocumentChoice(null))
                    documents.filter { it.category.equals("SYSTEM", true) || it.category.equals("TEMPLATE", true) }
                        .forEach { systemDocumentCombo.addItem(DocumentChoice(it)) }
                    for (i in 0 until systemDocumentCombo.itemCount) {
                        if (systemDocumentCombo.getItemAt(i).document?.id == previousSystem) systemDocumentCombo.selectedIndex =
                            i
                    }
                    reloadModelsForRole(registered, assignments, previousModel)
                    reloadProviderModels(previousProviderModel)
                    statusUpdate(
                        "Registered: ${snapshot.localModels.size} local model(s); ${snapshot.providers.size} external model catalog(s) verified." +
                                if (snapshot.diagnostics.isNotEmpty()) " ${snapshot.diagnostics.size} runtime/provider diagnostic(s)." else ""
                    )
                } finally {
                    refreshing = false
                }
                updateDisplay()
                workflows.updateAvailableSources(snapshot)
            }
        }
    }

    private fun reloadModelsForRole(
        registered: List<com.jaanik.integratedaiworkbench.backend.model.PersistentRegisteredModel>? = null,
        assignments: Map<String, Set<String>>? = null,
        previous: String? = (modelCombo.selectedItem as? ModelProfile)?.id,
    ) {
        val allRegistered = registered ?: context.modelRegistry.registeredModels()
        val bindings = assignments ?: repository.modelRoleAssignments(allRegistered.mapTo(linkedSetOf()) { it.modelId })
        val route = routeCombo.selectedItem as? Route ?: Route.ROLE
        val role = (roleCombo.selectedItem as? RoleChoice)?.role
        val eligible = if (route == Route.ROLE && role != null) {
            val ids =
                allRegistered.filter { role.id in bindings[it.modelId].orEmpty() }.mapNotNull { it.profile?.id }.toSet()
            sources.localModels.filter { it.id in ids }
        } else sources.localModels
        val wasRefreshing = refreshing
        refreshing = true
        modelCombo.removeAllItems()
        eligible.forEach(modelCombo::addItem)
        val match = (0 until modelCombo.itemCount).firstOrNull { modelCombo.getItemAt(it).id == previous }
        if (match != null) modelCombo.selectedIndex = match
        refreshing = wasRefreshing
    }

    private fun proposeRoleModel() {
        if (refreshing) return
        reloadModelsForRole()
        val role = (roleCombo.selectedItem as? RoleChoice)?.role
        if ((routeCombo.selectedItem as? Route) == Route.ROLE && role != null && modelCombo.itemCount == 0) {
            statusUpdate("No model assigned to '${role.name}'. Assign one in Models, or select a verified external provider.")
        }
    }

    private fun reloadProviderModels(preferred: String? = null) {
        val models = (providerCombo.selectedItem as? ProviderChoice)?.provider?.models.orEmpty()
        val before = refreshing
        refreshing = true
        try {
            providerModelId.removeAllItems()
            models.forEach(providerModelId::addItem)
            if (preferred != null && preferred in models) providerModelId.selectedItem = preferred
            providerModelId.isEnabled = models.isNotEmpty()
        } finally {
            refreshing = before
        }
    }

    private fun providerModelId(): String = providerModelId.selectedItem as? String ?: ""

    private fun updateDisplay() {
        val route = routeCombo.selectedItem as? Route ?: Route.ROLE
        val role = (roleCombo.selectedItem as? RoleChoice)?.role
        val profile = modelCombo.selectedItem as? ModelProfile
        val provider = (providerCombo.selectedItem as? ProviderChoice)?.provider
        val baseLabel = when (route) {
            Route.PROVIDER -> "External provider: ${provider?.record?.displayName ?: "not selected"} · Model: ${providerModelId().ifBlank { "not selected" }}"
            Route.LOCAL -> "Integrated local inference: ${profile?.displayName ?: "not selected"}"
            Route.ROLE -> "Role: ${role?.name ?: "not selected"} · Local model: ${profile?.displayName ?: "not selected"} · Optional provider override: ${provider?.record?.displayName ?: "none"}"
        }
        val localReason = profile?.let { sources.unavailableLocalReasons[it.id] }
        val label = if (localReason != null && provider == null) "$baseLabel · Runtime unavailable: $localReason" else baseLabel
        summary.text = label
        summary.toolTipText = label
        val chatAvailable = (provider != null && route != Route.LOCAL) || profile != null
        chatButton.isEnabled = chatAvailable && !busy
        sendButton.isEnabled = !busy && when (route) {
            Route.PROVIDER -> provider != null && providerModelId().isNotBlank()
            Route.LOCAL -> profile != null && localReason == null
            Route.ROLE -> provider != null || (profile != null && localReason == null)
        }
        chatButton.toolTipText = "Open a modeless multi-turn chat. Local chat replays bounded history in the prompt."
    }

    private fun selection(): Selection {
        val route = routeCombo.selectedItem as? Route ?: Route.ROLE
        val role = (roleCombo.selectedItem as? RoleChoice)?.role
        if (route == Route.ROLE) require(role != null) { "Choose a role or select 'By local model' / 'By external provider'." }
        val provider = (providerCombo.selectedItem as? ProviderChoice)?.provider
        val profile = modelCombo.selectedItem as? ModelProfile
        val destination = when (route) {
            Route.LOCAL -> Destination.Local(profile ?: error("Choose a local model."))
            Route.PROVIDER -> Destination.Remote(
                provider?.record ?: error("Choose an external provider."),
                providerModelId()
            )

            Route.ROLE -> {
                if (provider != null) Destination.Remote(provider.record, providerModelId())
                else {
                    val selected = profile ?: error("Choose a model assigned to this role or an external provider.")
                    val registered = context.modelRegistry.registeredModels()
                        .firstOrNull { it.profile?.id == selected.id && it.isAccessible }
                        ?: error("Selected local model is unavailable.")
                    val assigned =
                        repository.modelRoleAssignments(setOf(registered.modelId))[registered.modelId].orEmpty()
                    require(role!!.id in assigned) { "This model does not have the selected role. Assign it in Models, or choose an external provider." }
                    Destination.Local(selected)
                }
            }
        }
        if (destination is Destination.Remote) {
            require(destination.modelId.isNotBlank()) { "Choose a provider model ID." }
            require(destination.modelId in providerCatalog[destination.provider.id].orEmpty()) { "Model is not in the currently verified provider catalog. Refresh sources." }
        }
        val sections = mutableListOf<String>()
        if (role != null) {
            sections += "Role: ${role.name}\nPurpose: ${role.purpose}\nResponsibility / scope: ${role.responsibility}"
            val documents = repository.documents().associateBy { it.id }
            role.documentIds.forEach { id ->
                val doc = documents[id] ?: error("Role refers to unavailable document $id.")
                sections += "Attached ${doc.category} document '${doc.name}':\n${repository.content(doc)}"
            }
        }
        (systemDocumentCombo.selectedItem as? DocumentChoice)?.document?.let { doc ->
            sections += "Selected system document '${doc.name}':\n${repository.content(doc)}"
        }
        systemArea.text.trim().takeIf(String::isNotBlank)?.let { sections += it }
        behaviorArea.text.trim().takeIf(String::isNotBlank)?.let { sections += "Response behavior: $it" }
        val combined = sections.joinToString("\n\n")
        require(combined.length <= 64_000) { "Combined role and system instructions exceed 64,000 characters. Remove attachments or shorten instructions." }
        return Selection(destination, combined, role?.name)
    }

    private fun submit() {
        if (busy) return
        val text = promptArea.text.trim()
        if (text.isBlank()) {
            statusUpdate("Enter a prompt."); return
        }
        val configuration =
            runCatching { selection() }.getOrElse { statusUpdate(it.message ?: "Invalid selection"); return }
        busy = true; sendButton.isEnabled = false; cancelButton.isEnabled = true; chatButton.isEnabled = false
        resultArea.text = ""; statusUpdate("Sending actual inference request…")
        val jobId = generation.incrementAndGet()
        worker.launch {
            val result = runCatching {
                when (val target = configuration.destination) {
                    is Destination.Local -> {
                        val cancellation = InferenceCancellation().also { localCancellation = it }
                        val merged = if (configuration.system.isBlank()) text
                        else "SYSTEM INSTRUCTIONS:\n${configuration.system}\n\nUSER REQUEST:\n$text"
                        val response = context.workbench.executeLocal(target.model.id, merged, cancellation)
                        when (response.status) {
                            ModelResponseStatus.SUCCESS -> response.content.ifBlank { error("Local inference returned no text.") }
                            ModelResponseStatus.CANCELLED -> error("Local inference cancelled.")
                            ModelResponseStatus.TIMED_OUT -> error("Local inference timed out.")
                            ModelResponseStatus.FAILED -> error(
                                response.metadata["error"] ?: "Local inference failed without details."
                            )
                        }
                    }

                    is Destination.Remote -> {
                        val cancellation = ProviderInferenceClient.Cancellation().also { remoteCancellation = it }
                        val messages = buildList {
                            if (configuration.system.isNotBlank()) add(
                                ProviderInferenceClient.Turn(
                                    "system",
                                    configuration.system
                                )
                            )
                            add(ProviderInferenceClient.Turn("user", text))
                        }
                        val apiKey = context.providerRegistry.getApiKey(target.provider.id)
                        ProviderInferenceClient.complete(
                            target.provider,
                            target.modelId,
                            messages,
                            apiKey,
                            cancellation
                        ).content
                    }
                }
            }
            SwingUtilities.invokeLater {
                if (jobId != generation.get()) return@invokeLater
                result.onSuccess { resultArea.text = it; statusUpdate("Inference completed.") }
                    .onFailure {
                        resultArea.text =
                            it.message ?: "Unknown inference error"; statusUpdate("Inference failed. See output.")
                    }
                busy = false; sendButton.isEnabled = true; cancelButton.isEnabled = false
                localCancellation = null; remoteCancellation = null
                updateDisplay()
            }
        }
    }

    private fun openChat() {
        val selected = runCatching { selection() }.getOrElse { statusUpdate(it.message ?: "Invalid selection"); return }
        val target = when (val d = selected.destination) {
            is Destination.Local -> WorkbenchFloatingChat.Target.Local(d.model.displayName) { prompt, cancel ->
                val response = context.workbench.executeLocal(d.model.id, prompt, cancel)
                if (response.status != ModelResponseStatus.SUCCESS) error("Local inference ${response.status}: ${response.metadata["error"].orEmpty()}")
                response.content
            }

            is Destination.Remote -> WorkbenchFloatingChat.Target.Remote(d.provider, d.modelId) {
                context.providerRegistry.getApiKey(d.provider.id)
            }
        }
        WorkbenchFloatingChat.open(context.project, target, selected.system, selected.roleName)
        statusUpdate("Opened independent floating chat. Closing settings will not close the chat.")
    }

    override fun dispose() {
        generation.incrementAndGet()
        localCancellation?.cancel(); remoteCancellation?.cancel()
        sourceTimer.stop()
        workflows.dispose()
        worker.cancel()
    }
}
