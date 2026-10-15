package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.backend.model.GgufModelMetadata
import com.jaanik.integratedaiworkbench.backend.model.PersistentRegisteredModel
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import com.jaanik.integratedaiworkbench.shared.model.ModelProfile
import com.jaanik.integratedaiworkbench.shared.model.ThinkingLevel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.HierarchyEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Native editor for the persistent GGUF registry.
 *
 * File references, default selection, removal, reconnection and display names use
 * PersistentModelRegistry. Model-to-role assignments are stored in the shared WorkbenchLibrary
 * SQLite catalog, and role definitions are shared with the Capabilities tab.
 */
class SettingsPanelModelsTab(private val context: WorkbenchFrontendContext) : JPanel(BorderLayout(0, JBUI.scale(10))),
    RefreshableWorkbenchTab {
    /**
     * Six settings have an existing ModelProfile contract. The other fields are
     * editable previews until the actual inference request/runtime supports them.
     */
    private data class DraftInferenceSettings(
        var temperature: String = "0.2",
        var topP: String = "0.95",
        var topK: String = "",
        var maxOutputTokens: String = "2048",
        var contextWindowTokens: String = "",
        var timeoutSeconds: String = "60",
        var seed: String = "",
        var repeatPenalty: String = "",
        var presencePenalty: String = "",
        var frequencyPenalty: String = "",
        var mirostat: String = "AUTO",
        var stream: String = "AUTO",
        var reasoning: String = "AUTO",
        var thinkingLevel: ThinkingLevel = ThinkingLevel.DEFAULT,
        var reasoningTokenBudget: String = "",
    ) {
        fun simulated(): SimulatedInferenceSettings = SimulatedInferenceSettings(
            topK = topK,
            seed = seed,
            repeatPenalty = repeatPenalty,
            presencePenalty = presencePenalty,
            frequencyPenalty = frequencyPenalty,
            mirostat = mirostat,
            stream = stream,
            reasoning = reasoning,
            reasoningTokenBudget = reasoningTokenBudget,
        )

        fun applySimulated(value: SimulatedInferenceSettings) {
            topK = value.topK
            seed = value.seed
            repeatPenalty = value.repeatPenalty
            presencePenalty = value.presencePenalty
            frequencyPenalty = value.frequencyPenalty
            mirostat = value.mirostat
            stream = value.stream
            reasoning = value.reasoning
            reasoningTokenBudget = value.reasoningTokenBudget
        }
    }

    /** Session-only values; never passed off as live llama.cpp parameters. */
    private data class SimulatedInferenceSettings(
        val topK: String = "",
        val seed: String = "",
        val repeatPenalty: String = "",
        val presencePenalty: String = "",
        val frequencyPenalty: String = "",
        val mirostat: String = "AUTO",
        val stream: String = "AUTO",
        val reasoning: String = "AUTO",
        val reasoningTokenBudget: String = "",
    )

    companion object {
        // A session cache only. An IDE restart does NOT preserve these settings.
        // Use project + model ID to prevent cross-project collisions.
        private val simulatedSettingsByModel = mutableMapOf<Pair<String, String>, SimulatedInferenceSettings>()
    }

    private data class DraftModel(
        val key: String,
        val modelId: String?,
        val profileId: String?,
        val originalDisplayName: String,
        var displayName: String,
        val canonicalPath: String,
        val accessible: Boolean,
        var reconnectPath: String? = null,
        var inferenceSettings: DraftInferenceSettings = DraftInferenceSettings(),
    ) {
        val effectivePath: String get() = reconnectPath ?: canonicalPath
        val isNew: Boolean get() = modelId == null
    }

    private data class SavePlan(
        val models: List<DraftModel>,
        val removedModelIds: Set<String>,
        val defaultKey: String?,
        val rolesByKey: Map<String, Set<String>>,
    )

    private data class SaveResult(
        val registeredModels: List<PersistentRegisteredModel>,
        val modelIdByKey: Map<String, String>,
        val assignments: Map<String, Set<String>>,
        val roleCatalog: List<WorkbenchLibrary.Role>,
    )

    private val cards = object : JPanel(), Scrollable {
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int =
            JBUI.scale(24)

        override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int =
            JBUI.scale(240)

        override fun getScrollableTracksViewportWidth(): Boolean = true
        override fun getScrollableTracksViewportHeight(): Boolean = false
    }.apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    /** Responsive groups are reflowed without recreating editors or losing focus/drafts. */
    private data class ResponsiveGroup(val panel: JPanel, val ribbon: Boolean)

    private val responsiveGroups = mutableListOf<ResponsiveGroup>()
    private val cardScrollPane = JBScrollPane(cards).apply {
        border = JBUI.Borders.emptyTop(0)
        viewportBorder = JBUI.Borders.empty()
        viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) = updateResponsiveLayouts()
        })
    }

    private val search = JBTextField().apply { emptyText.text = "Search model name or filename…" }
    private val count = JBLabel()
    private val savedState = JBLabel()
    private val saveButton = JButton("Save Current List")
    private val discardButton = JButton("Discard changes")
    private val status = JBLabel().apply { horizontalAlignment = SwingConstants.RIGHT }

    /** The complete draft stays intact while [search] only changes the rendered subset. */
    private var draftModels = mutableListOf<DraftModel>()
    private val removedModelIds = linkedSetOf<String>()
    private var draftDefaultKey: String? = null
    private val repository = WorkbenchLibrary(
        Path.of(PathManager.getConfigPath()).resolve("integrated-ai-workbench"),
        context.project.basePath?.let(Path::of),
    )
    private var availableRoles: List<WorkbenchLibrary.Role> = emptyList()
    private val draftRoleAssignments = mutableMapOf<String, MutableSet<String>>()
    private val expandedModelKeys = linkedSetOf<String>()
    private val metadataByModelKey = mutableMapOf<String, GgufModelMetadata>()
    private val metadataErrorsByModelKey = mutableMapOf<String, String>()
    private var dirty = false
    private var saving = false
    private var reloadGeneration = 0L

    private val projectKey: String get() = context.project.basePath ?: context.project.name

    init {
        border = JBUI.Borders.empty(4)
        add(header(), BorderLayout.NORTH)
        add(cardScrollPane, BorderLayout.CENTER)
        add(saveBar(), BorderLayout.SOUTH)

        // Refresh role catalog when the Models view becomes visible after editing Capabilities.
        addHierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && isShowing) {
                refreshFromServices()
            }
        }

        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = renderCards()
            override fun removeUpdate(event: DocumentEvent) = renderCards()
            override fun changedUpdate(event: DocumentEvent) = renderCards()
        })
        updateSaveState()
    }

    private fun header(): JPanel = JPanel(BorderLayout()).apply {
        add(
            sectionPanel(
                title = "Model catalog",
                description = "Register local GGUF files without moving or modifying them.",
                trailing = count,
            ).apply {
                add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                    add(search, BorderLayout.CENTER)
                    add(JButton("Add").apply {
                        toolTipText = "Add a local GGUF reference to the draft list."
                        addActionListener { chooseModel() }
                    }, BorderLayout.EAST)
                }, BorderLayout.SOUTH)
            })
    }

    private fun sectionPanel(title: String, description: String, trailing: Component? = null): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(10),
            )
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(JBLabel(title).apply { font = font.deriveFont(font.style or java.awt.Font.BOLD) })
                    add(JBLabel(description).apply { toolTipText = description })
                }, BorderLayout.CENTER)
                trailing?.let { add(it, BorderLayout.EAST) }
            }, BorderLayout.NORTH)
        }

    private fun saveBar(): JPanel = JPanel(BorderLayout(JBUI.scale(12), 0)).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            JBUI.Borders.empty(10),
        )
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(savedState)
            add(JBLabel("Model profiles and assigned roles are saved; [SIM] inference fields remain session-only."))
        }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0)).apply {
            discardButton.addActionListener { discardChanges() }
            saveButton.addActionListener { saveDraft() }
            add(discardButton)
            add(saveButton)
            add(status)
        }, BorderLayout.EAST)
    }

    override fun refreshFromServices() {
        val generation = ++reloadGeneration
        ApplicationManager.getApplication().executeOnPooledThread {
            val state = runCatching {
                val registered = context.modelRegistry.registeredModels()
                val roles = repository.roles()
                val assignments = repository.modelRoleAssignments(registered.mapTo(linkedSetOf()) { it.modelId })
                val inspections = if (dirty || saving) emptyMap() else
                    registered.filter(PersistentRegisteredModel::isAccessible).associate { model ->
                        model.modelId to runCatching { context.modelRegistry.inspectGguf(Path.of(model.canonicalPath)) }
                    }
                LoadedCatalog(registered, roles, assignments, inspections)
            }
            ApplicationManager.getApplication().invokeLater {
                if (generation != reloadGeneration) return@invokeLater
                state.fold(onSuccess = { catalog ->
                    availableRoles = catalog.roles
                    if (dirty || saving) {
                        // Preserve unsaved selections, but expose roles created in Capabilities.
                        normalizeRoleAssignments()
                        renderCards()
                    } else {
                        catalog.inspections.forEach { (modelId, result) ->
                            result.onSuccess { metadata ->
                                metadataByModelKey[modelId] = metadata
                                metadataErrorsByModelKey.remove(modelId)
                            }.onFailure { error ->
                                metadataByModelKey.remove(modelId)
                                metadataErrorsByModelKey[modelId] = error.message ?: "Unable to read GGUF metadata."
                            }
                        }
                        loadSavedDraft(catalog.registered, catalog.assignments)
                    }
                }, onFailure = { error ->
                    setStatus("Failed loading model/role catalog: ${error.message ?: "unknown error"}")
                })
            }
        }
    }

    private data class LoadedCatalog(
        val registered: List<PersistentRegisteredModel>,
        val roles: List<WorkbenchLibrary.Role>,
        val assignments: Map<String, Set<String>>,
        val inspections: Map<String, Result<GgufModelMetadata>>,
    )

    private fun chooseModel() {
        if (saving) return
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("gguf").apply {
            title = "Add GGUF Model to List"
            description = "Select one local GGUF file. It stays in its current location."
        }
        FileChooser.chooseFile(descriptor, context.project, null) { file ->
            if (!file.name.endsWith(".gguf", ignoreCase = true)) {
                setStatus("Select a GGUF file.")
                return@chooseFile
            }
            canonicalize(Path.of(file.path)) { canonical ->
                if (draftModels.any {
                        sameCanonicalPath(
                            it.effectivePath,
                            canonical
                        ) && it.modelId !in removedModelIds
                    }) {
                    setStatus("This model is already registered or already in the draft.")
                    return@canonicalize
                }
                val key = "draft-${UUID.randomUUID()}"
                val draft = DraftModel(
                    key = key,
                    modelId = null,
                    profileId = null,
                    originalDisplayName = file.name,
                    displayName = file.name,
                    canonicalPath = canonical,
                    accessible = true,
                )
                draftModels.add(draft)
                metadataByModelKey.remove(key)
                metadataErrorsByModelKey.remove(key)
                draftRoleAssignments[key] = linkedSetOf()
                if (draftDefaultKey == null) draftDefaultKey = key
                expandedModelKeys.add(key)
                markDirty("Pending save: ${file.name}")
                inspectMetadata(draft)
            }
        }
    }

    private fun canonicalize(path: Path, onSuccess: (String) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { path.toRealPath().toString() }
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { if (!saving) onSuccess(it) },
                    onFailure = { setStatus("Unable to use this GGUF file: ${it.message ?: "unknown error"}") },
                )
            }
        }
    }

    private fun toggleExpanded(key: String) {
        if (saving) return
        if (!expandedModelKeys.add(key)) expandedModelKeys.remove(key)
        renderCards()
    }

    private fun inspectMetadata(model: DraftModel) {
        if (saving) return
        setStatus("Inspecting GGUF header…")
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { context.modelRegistry.inspectGguf(Path.of(model.effectivePath)) }
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { metadata ->
                        metadataByModelKey[model.key] = metadata
                        metadataErrorsByModelKey.remove(model.key)
                        setStatus("Read GGUF metadata: ${model.displayName}")
                    },
                    onFailure = { error ->
                        metadataByModelKey.remove(model.key)
                        metadataErrorsByModelKey[model.key] = error.message ?: "Unable to read GGUF metadata."
                        setStatus("Could not inspect GGUF metadata: ${error.message ?: "unknown error"}")
                    },
                )
                renderCards()
            }
        }
    }

    private fun renameModel(model: DraftModel) {
        if (saving) return
        if (model.modelId != null && model.profileId == null) {
            setStatus("This registry entry has no editable profile.")
            return
        }
        val renamed = Messages.showInputDialog(
            this,
            "Display name for ${fileName(model.effectivePath)}:",
            "Rename model",
            Messages.getQuestionIcon(),
            model.displayName,
            null,
        )?.trim().orEmpty()
        if (renamed.isBlank()) return
        if (draftModels.any { it.key != model.key && it.displayName.equals(renamed, ignoreCase = true) }) {
            setStatus("Model display names must be unique in this list.")
            return
        }
        model.displayName = renamed
        markDirty("Model rename pending save: $renamed")
    }

    private fun confirmRemove(model: DraftModel) {
        if (saving) return
        val result = Messages.showYesNoDialog(
            context.project,
            "Remove \"${model.displayName}\" from the Workbench model list? The GGUF file is not deleted.",
            "Remove model",
            "Remove from list",
            "Cancel",
            Messages.getWarningIcon(),
        )
        if (result != Messages.YES) return
        if (model.isNew) {
            draftModels.removeIf { it.key == model.key }
            draftRoleAssignments.remove(model.key)
        } else {
            removedModelIds.add(requireNotNull(model.modelId))
        }
        if (draftDefaultKey == model.key) draftDefaultKey = firstEligibleModelKey()
        markDirty("Removal pending save: ${model.displayName}")
    }

    private fun stageReconnect(model: DraftModel) {
        if (saving || model.modelId == null) return
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("gguf").apply {
            title = "Reconnect GGUF Model"
            description = "Select the moved GGUF file. The old registry entry is retained until Save Current List."
        }
        FileChooser.chooseFile(descriptor, context.project, null) { file ->
            if (!file.name.endsWith(".gguf", ignoreCase = true)) {
                setStatus("Select a GGUF file.")
                return@chooseFile
            }
            canonicalize(Path.of(file.path)) { canonical ->
                if (draftModels.any {
                        it.key != model.key && sameCanonicalPath(
                            it.effectivePath,
                            canonical
                        ) && it.modelId !in removedModelIds
                    }) {
                    setStatus("This model file is already registered or pending in the draft.")
                    return@canonicalize
                }
                model.reconnectPath = canonical
                markDirty("Reconnect pending save: ${model.displayName}")
            }
        }
    }

    private fun setDefault(model: DraftModel) {
        if (saving || !isEligibleDefault(model)) return
        draftDefaultKey = model.key
        markDirty("Default model selection pending save: ${model.displayName}", render = false)
        renderCards()
    }

    private fun setRoleAssigned(model: DraftModel, roleId: String, selected: Boolean) {
        if (saving) return
        val roles = draftRoleAssignments.getOrPut(model.key) { linkedSetOf() }
        if (selected) roles.add(roleId) else roles.remove(roleId)
        markDirty("Role assignments pending save: ${model.displayName}")
    }

    private fun saveDraft() {
        if (!dirty || saving) return
        normalizeRoleAssignments()
        val plan = SavePlan(
            models = draftModels.map { it.copy(inferenceSettings = it.inferenceSettings.copy()) },
            removedModelIds = removedModelIds.toSet(),
            defaultKey = draftDefaultKey,
            rolesByKey = draftRoleAssignments.mapValues { (_, roles) -> roles.toSet() },
        )
        saving = true
        updateSaveState()
        renderCards()
        ApplicationManager.getApplication().executeOnPooledThread {
            val saved = runCatching { applySavePlan(plan) }
            val refreshed = context.modelRegistry.registeredModels()
            ApplicationManager.getApplication().invokeLater {
                saving = false
                saved.fold(
                    onSuccess = { result ->
                        availableRoles = result.roleCatalog
                        commitSimulatedInferenceSettings(plan, result)
                        loadSavedDraft(result.registeredModels, result.assignments)
                        dirty = false
                        setStatus("Model list saved.")
                        context.savedStateChanged()
                    },
                    onFailure = { error ->
                        // Some real registry operations may have succeeded before a later one failed.
                        // Rebase onto that actual state and retain the unsaved user intent for retry.
                        rebaseDraftAfterFailure(refreshed)
                        dirty = true
                        setStatus("Unable to save model list: ${error.message ?: "unknown error"}")
                    },
                )
                updateSaveState()
                renderCards()
            }
        }
    }

    private fun applySavePlan(plan: SavePlan): SaveResult {
        // Reject invalid values BEFORE any persistent registry mutation.
        plan.models.filter { it.modelId !in plan.removedModelIds }.forEach(::validateInferenceSettings)
        val modelIdByKey = linkedMapOf<String, String>()
        val currentById = context.modelRegistry.registeredModels().associateBy(PersistentRegisteredModel::modelId)
        plan.models.filter { !it.isNew && it.modelId !in plan.removedModelIds }.forEach { draft ->
            val current = currentById[requireNotNull(draft.modelId)]
                ?: error("Registered model was not found: ${draft.displayName}")
            val profile = current.profile ?: error("Registered model has no editable profile: ${draft.displayName}")
            val updated = profileWithDraftSettings(profile, draft)
            if (updated != profile) context.modelRegistry.updateLocalProfile(updated)
            modelIdByKey[draft.key] = draft.modelId
        }

        // Removing first permits a replacement or reconnection to use a path whose old entry is being removed.
        plan.removedModelIds.forEach(context.modelRegistry::removeLocalModel)
        plan.models.filter(DraftModel::isNew).forEach { draft ->
            val profile = context.modelRegistry.registerLocalModel(Path.of(draft.canonicalPath), draft.displayName)
            val updated = profileWithDraftSettings(profile, draft)
            if (updated != profile) context.modelRegistry.updateLocalProfile(updated)
            val modelId = context.modelRegistry.registeredModels().firstOrNull { it.profile?.id == profile.id }?.modelId
                ?: error("Registered model could not be resolved: ${draft.displayName}")
            modelIdByKey[draft.key] = modelId
        }
        plan.models.filter { !it.isNew && it.modelId !in plan.removedModelIds && it.reconnectPath != null }
            .forEach { draft ->
                context.modelRegistry.reconnectLocalModel(
                    requireNotNull(draft.modelId),
                    Path.of(requireNotNull(draft.reconnectPath))
                )
            }

        val registered = context.modelRegistry.registeredModels()
        val desiredProfileId = plan.defaultKey?.let { key ->
            val modelId = modelIdByKey[key]
            registered.firstOrNull { it.modelId == modelId && it.isAccessible }?.profile?.id
        }
        val fallbackProfileId = registered.firstOrNull { it.isAccessible }?.profile?.id.orEmpty()
        context.settings.update(
            context.settings.snapshot().copy(defaultModelProfileId = desiredProfileId ?: fallbackProfileId)
        )
        // Persist the actual model-to-role associations in WorkbenchLibrary, never in a session mock.
        val associations = linkedMapOf<String, Set<String>>()
        plan.models.filter { it.modelId !in plan.removedModelIds }.forEach { draft ->
            val id = modelIdByKey[draft.key] ?: error("Model ID missing for ${draft.displayName}")
            associations[id] = plan.rolesByKey[draft.key].orEmpty()
        }
        repository.saveModelRoleAssignments(associations, plan.removedModelIds)
        val assigned = repository.modelRoleAssignments(registered.mapTo(linkedSetOf()) { it.modelId })
        return SaveResult(registered, modelIdByKey, assigned, repository.roles())
    }

    private fun commitSimulatedInferenceSettings(plan: SavePlan, result: SaveResult) {
        val survivingIds = result.registeredModels.mapTo(linkedSetOf()) { it.modelId }
        simulatedSettingsByModel.keys.removeIf { it.first == projectKey && it.second !in survivingIds }
        plan.models.filter { it.modelId !in plan.removedModelIds }.forEach { model ->
            val id = result.modelIdByKey[model.key] ?: return@forEach
            simulatedSettingsByModel[projectKey to id] = model.inferenceSettings.simulated()
        }
    }

    private fun discardChanges() {
        if (saving) return
        dirty = false
        reloadGeneration += 1
        refreshFromServices()
        setStatus("Draft changes discarded.")
    }

    private fun loadSavedDraft(
        registered: List<PersistentRegisteredModel>,
        persistedAssignments: Map<String, Set<String>>,
    ) {
        draftModels = registered.map { it.toDraft() }.toMutableList()
        removedModelIds.clear()
        draftDefaultKey =
            draftModels.firstOrNull { it.profileId == context.settings.snapshot().defaultModelProfileId }?.key
        draftRoleAssignments.clear()
        draftModels.forEach { model ->
            draftRoleAssignments[model.key] =
                persistedAssignments[model.modelId].orEmpty().toMutableSet()
        }
        dirty = false
        updateSaveState()
        renderCards()
    }

    /** Keeps an error-recoverable draft if a multi-operation save partially updated the real registry. */
    private fun rebaseDraftAfterFailure(registered: List<PersistentRegisteredModel>) {
        val previous = draftModels.toList()
        val previousRoles = draftRoleAssignments.mapValues { (_, roles) -> roles.toMutableSet() }
        val byModelId = previous.filter { it.modelId != null }.associateBy { requireNotNull(it.modelId) }
        val byPath = previous.associateBy { canonicalKey(it.effectivePath) }
        val previousDefault = draftDefaultKey
        val rebuilt = registered.map { current ->
            val old = byModelId[current.modelId] ?: byPath[canonicalKey(current.canonicalPath)]
            DraftModel(
                key = current.modelId,
                modelId = current.modelId,
                profileId = current.profile?.id,
                originalDisplayName = current.displayName,
                displayName = old?.displayName ?: current.displayName,
                canonicalPath = current.canonicalPath,
                accessible = current.isAccessible,
                reconnectPath = old?.reconnectPath?.takeUnless { sameCanonicalPath(it, current.canonicalPath) },
                inferenceSettings = old?.inferenceSettings?.copy()
                    ?: current.profile?.toDraftInferenceSettings()?.apply {
                        simulatedSettingsByModel[projectKey to current.modelId]?.let(::applySimulated)
                    }
                    ?: DraftInferenceSettings(),
            )
        }.toMutableList()
        previous.filter(DraftModel::isNew).forEach { pending ->
            if (registered.none { sameCanonicalPath(it.canonicalPath, pending.canonicalPath) }) rebuilt.add(pending)
        }
        draftModels = rebuilt
        removedModelIds.retainAll(registered.mapTo(linkedSetOf()) { it.modelId })
        draftRoleAssignments.clear()
        rebuilt.forEach { model ->
            val old = byModelId[model.modelId] ?: byPath[canonicalKey(model.canonicalPath)]
            draftRoleAssignments[model.key] = previousRoles[old?.key ?: model.key].orEmpty().toMutableSet()
        }
        draftDefaultKey = rebuilt.firstOrNull { model ->
            model.key == previousDefault ||
                    model.modelId == previousDefault ||
                    byModelId[model.modelId]?.key == previousDefault ||
                    byPath[canonicalKey(model.canonicalPath)]?.key == previousDefault
        }?.key ?: firstEligibleModelKey()
    }

    private fun PersistentRegisteredModel.toDraft(): DraftModel {
        val settings = profile?.toDraftInferenceSettings() ?: DraftInferenceSettings()
        simulatedSettingsByModel[projectKey to modelId]?.let(settings::applySimulated)
        return DraftModel(
            key = modelId,
            modelId = modelId,
            profileId = profile?.id,
            originalDisplayName = displayName,
            displayName = displayName,
            canonicalPath = canonicalPath,
            accessible = isAccessible,
            inferenceSettings = settings,
        )
    }

    private fun normalizeRoleAssignments() {
        val valid = availableRoles.mapTo(linkedSetOf()) { it.id }
        draftRoleAssignments.values.forEach { it.retainAll(valid) }
    }

    private fun renderCards() {
        count.text = "${draftModels.count { it.modelId !in removedModelIds }} listed"
        responsiveGroups.clear()
        cards.removeAll()
        val filter = search.text.trim().lowercase(Locale.ROOT)
        val defaultGroup = ButtonGroup()
        draftModels.asSequence()
            .filterNot { it.modelId in removedModelIds }
            .filter {
                filter.isBlank() || it.displayName.lowercase(Locale.ROOT)
                    .contains(filter) || fileName(it.effectivePath).lowercase(Locale.ROOT).contains(filter)
            }
            .forEach { model ->
                cards.add(modelCard(model, defaultGroup))
                cards.add(Box.createVerticalStrut(JBUI.scale(8)))
            }
        if (cards.componentCount == 0) {
            cards.add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(28)
                add(JBLabel("◇").apply { alignmentX = Component.CENTER_ALIGNMENT })
                add(JBLabel("No matching models").apply { alignmentX = Component.CENTER_ALIGNMENT })
                add(JBLabel("Add a GGUF file or adjust your search.").apply { alignmentX = Component.CENTER_ALIGNMENT })
                add(JButton("Add Model to List").apply {
                    alignmentX = Component.CENTER_ALIGNMENT
                    isEnabled = !saving
                    addActionListener { chooseModel() }
                })
            })
        }
        updateResponsiveLayouts()
        cards.revalidate()
        cards.repaint()
    }

    private fun registerResponsive(panel: JPanel, ribbon: Boolean): JPanel {
        responsiveGroups.add(ResponsiveGroup(panel, ribbon))
        return panel
    }

    private fun updateResponsiveLayouts() {
        val available = cardScrollPane.viewport.width.takeIf { it > 0 }
            ?: width.takeIf { it > 0 }
            ?: JBUI.scale(900)
        for (group in responsiveGroups) {
            val columns = if (group.ribbon) {
                when {
                    available >= JBUI.scale(1180) -> 4
                    available >= JBUI.scale(620) -> 2
                    else -> 1
                }
            } else {
                if (available >= JBUI.scale(770)) 2 else 1
            }
            val current = group.panel.layout as? GridLayout
            if (current?.columns != columns) {
                group.panel.layout = GridLayout(0, columns, JBUI.scale(10), JBUI.scale(5))
                group.panel.revalidate()
            }
        }
        // BoxLayout honors each card's maximum height. Recalculate it after reflow,
        // otherwise a formerly wide card is clipped when the viewport gets narrow.
        cards.components.filterIsInstance<JPanel>().forEach { card ->
            card.maximumSize = Dimension(Int.MAX_VALUE, card.preferredSize.height)
        }
        cards.revalidate()
        cards.repaint()
    }

    private fun modelCard(model: DraftModel, defaultGroup: ButtonGroup): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createLineBorder(JBColor.border())
            add(JPanel(BorderLayout(JBUI.scale(12), 0)).apply {
                border = JBUI.Borders.empty(9)
                add(JBLabel("◈").apply {
                    horizontalAlignment = SwingConstants.CENTER
                    preferredSize = Dimension(JBUI.scale(32), JBUI.scale(32))
                    minimumSize = preferredSize
                    maximumSize = preferredSize
                    toolTipText = "Local GGUF model"
                }, BorderLayout.WEST)
                add(modelMain(model, defaultGroup), BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            if (model.key in expandedModelKeys) add(modelDetails(model), BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

    private fun modelMain(model: DraftModel, defaultGroup: ButtonGroup): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                add(JBLabel(model.displayName).apply {
                    font = font.deriveFont(font.style or java.awt.Font.BOLD)
                    toolTipText = model.effectivePath
                }, BorderLayout.CENTER)
                val default = JRadioButton("Default", model.key == draftDefaultKey).apply {
                    isEnabled = !saving && isEligibleDefault(model)
                    toolTipText =
                        if (isEnabled) "Use this model as the default." else "Only an accessible model with a profile can become default."
                    addActionListener { if (isSelected) setDefault(model) }
                }
                defaultGroup.add(default)
                add(default, BorderLayout.EAST)
            }, BorderLayout.NORTH)

            // All immutable GGUF information belongs in the compact upper ribbon.
            add(metadataRibbon(model), BorderLayout.CENTER)

            add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
                val roleCount = draftRoleAssignments[model.key].orEmpty().size
                add(tag("$roleCount role${if (roleCount == 1) "" else "s"}", accent = true))
                add(compactButton(if (model.key in expandedModelKeys) "Hide details" else "Configure model") {
                    toggleExpanded(
                        model.key
                    )
                })
                add(compactButton("Rename") { renameModel(model) })
                add(compactButton("Inspect GGUF") { inspectMetadata(model) }.apply {
                    isEnabled = !saving && (model.accessible || model.reconnectPath != null)
                    toolTipText = "Read GGUF metadata without modifying the model file."
                })
                if (!model.accessible || model.reconnectPath != null) {
                    add(compactButton(if (model.reconnectPath == null) "Reconnect" else "Change file") {
                        stageReconnect(
                            model
                        )
                    })
                }
                add(compactButton("Remove") { confirmRemove(model) }.apply {
                    toolTipText = "Remove only the registry entry; never delete the GGUF."
                })
            }, BorderLayout.SOUTH)
        }

    private fun metadataRibbon(model: DraftModel): JPanel =
        registerResponsive(JPanel(GridLayout(0, 4, JBUI.scale(8), JBUI.scale(3))), ribbon = true).apply {
            val features = declaredFeaturesText(model)
            add(metaTag("GGUF", fileName(model.effectivePath), model.effectivePath))
            add(metaTag("Size", sizeText(model)))
            add(metaTag("Architecture", architectureText(model)))
            add(metaTag("Quantization", quantizationText(model)))
            add(metaTag("Context limit", metadataContextText(model)))
            add(metaTag("Parameters", parameterCountText(model)))
            add(metaTag("Tokenizer", tokenizerText(model)))
            add(metaTag("Features", features))
            metadataErrorsByModelKey[model.key]?.let { error ->
                add(metaTag("Metadata error", "Unavailable", error))
            }
            if (model.reconnectPath != null) add(metaTag("File", "Reconnect pending", model.reconnectPath!!))
        }

    private fun metaTag(label: String, value: String, details: String = value): JBLabel =
        tag("$label: $value").apply { toolTipText = "$label: $details" }

    /** Only editable values remain below the header: inference settings and roles. */
    private fun modelDetails(model: DraftModel): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.empty(10),
            )
            add(JPanel(BorderLayout(0, JBUI.scale(7))).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                add(JBLabel("Inference settings").apply {
                    font = font.deriveFont(font.style or java.awt.Font.BOLD)
                }, BorderLayout.NORTH)
                add(inferenceSettingsEditor(model), BorderLayout.CENTER)
                add(JBLabel("Additional settings marked SIMULATION are session-only and do not affect real inference.").apply {
                    foreground = JBColor.GRAY
                }, BorderLayout.SOUTH)
            })
            add(Box.createVerticalStrut(JBUI.scale(10)))
            add(JPanel(BorderLayout(0, JBUI.scale(6))).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                add(JPanel(BorderLayout()).apply {
                    add(JBLabel("Assigned roles").apply {
                        font = font.deriveFont(font.style or java.awt.Font.BOLD)
                    }, BorderLayout.CENTER)
                    add(JButton("Refresh roles").apply {
                        isEnabled = !saving
                        addActionListener { refreshFromServices() }
                    }, BorderLayout.EAST)
                }, BorderLayout.NORTH)
                add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(7), 0)).apply {
                    if (availableRoles.isEmpty()) {
                        add(JBLabel("No roles in Repository Library. Create one in Capabilities → Studio."))
                    } else {
                        val assigned = draftRoleAssignments[model.key].orEmpty()
                        availableRoles.forEach { role ->
                            val label = "${role.name} [${role.scope.name.lowercase(Locale.ROOT)}]"
                            add(JBCheckBox(label, role.id in assigned).apply {
                                isEnabled = !saving
                                toolTipText = "Persistent role association. Permissions are governed by Workflow."
                                addActionListener { setRoleAssigned(model, role.id, isSelected) }
                            })
                        }
                    }
                }, BorderLayout.CENTER)
            })
        }

    private fun inferenceSettingsEditor(model: DraftModel): JPanel =
        registerResponsive(JPanel(GridLayout(0, 2, JBUI.scale(12), JBUI.scale(7))), ribbon = false).apply {
            val settings = model.inferenceSettings
            val editable = !saving && (model.isNew || model.profileId != null)

            fun optionRow(name: String, input: Component, help: String): JPanel =
                JPanel(BorderLayout(0, JBUI.scale(3))).apply {
                    border = JBUI.Borders.empty(2)
                    add(JBLabel(name).apply { toolTipText = help }, BorderLayout.NORTH)
                    add(input, BorderLayout.CENTER)
                    alignmentX = Component.LEFT_ALIGNMENT
                    toolTipText = help
                }

            fun numeric(name: String, current: () -> String, real: Boolean = false, update: (String) -> Unit) {
                val help = if (real) {
                    if (name == "Context window tokens")
                        "Optional. Blank = automatic runtime context. Integer only; no grouping separators."
                    else
                        "Optional. Blank = retain the existing saved ModelProfile value (NOT an omitted llama.cpp argument). " +
                                "Both comma and dot decimal separators are accepted where applicable."
                } else {
                    "SIMULATION ONLY: session-scoped; not sent to llama.cpp. " +
                            "Blank = Auto. Both decimal comma and decimal dot are accepted where applicable."
                }
                val display = if (real) name else "$name [SIM]"
                val editor = inferenceField(model, name, current, editable, real, update).apply {
                    toolTipText = help
                    emptyText.text = if (real && name != "Context window tokens") "Keep saved value" else "Auto"
                }
                add(optionRow(display, editor, help))
            }

            fun choice(name: String, current: () -> String, options: Array<String>, update: (String) -> Unit) {
                val help = "SIMULATION ONLY: session-scoped. Does not alter real inference. AUTO uses default behavior."
                val editor = ComboBox(options).apply {
                    selectedItem = current()
                    isEnabled = editable
                    toolTipText = help
                    addActionListener {
                        val next = selectedItem as? String ?: return@addActionListener
                        if (next != current()) {
                            update(next)
                            markDirty("Inference settings pending save: ${model.displayName}", render = false)
                        }
                    }
                }
                add(optionRow("$name [SIM]", editor, help))
            }

            numeric("Temperature", { settings.temperature }, real = true) { settings.temperature = it }
            numeric("Top-p", { settings.topP }, real = true) { settings.topP = it }
            numeric("Top-k", { settings.topK }) { settings.topK = it }
            numeric("Max output tokens", { settings.maxOutputTokens }, real = true) { settings.maxOutputTokens = it }
            numeric(
                "Context window tokens",
                { settings.contextWindowTokens },
                real = true
            ) { settings.contextWindowTokens = it }
            numeric("Timeout seconds", { settings.timeoutSeconds }, real = true) { settings.timeoutSeconds = it }
            numeric("Seed", { settings.seed }) { settings.seed = it }
            numeric("Repeat penalty", { settings.repeatPenalty }) { settings.repeatPenalty = it }
            numeric("Presence penalty", { settings.presencePenalty }) { settings.presencePenalty = it }
            numeric("Frequency penalty", { settings.frequencyPenalty }) { settings.frequencyPenalty = it }
            choice("Mirostat", { settings.mirostat }, arrayOf("AUTO", "OFF", "V1", "V2")) { settings.mirostat = it }
            choice("Stream", { settings.stream }, arrayOf("AUTO", "ON", "OFF")) { settings.stream = it }
            choice("Reasoning", { settings.reasoning }, arrayOf("AUTO", "ON", "OFF")) { settings.reasoning = it }

            val reasoningHelp =
                "Stored in ModelProfile.thinkingLevel; actual reasoning support depends on the model and runtime."
            add(optionRow("Reasoning level", ComboBox(ThinkingLevel.entries.toTypedArray()).apply {
                selectedItem = settings.thinkingLevel
                isEnabled = editable
                toolTipText = reasoningHelp
                addActionListener {
                    val level = selectedItem as? ThinkingLevel ?: ThinkingLevel.DEFAULT
                    if (level != settings.thinkingLevel) {
                        settings.thinkingLevel = level
                        markDirty("Reasoning level pending save: ${model.displayName}", render = false)
                    }
                }
            }, reasoningHelp))
            numeric("Reasoning token budget", { settings.reasoningTokenBudget }) { settings.reasoningTokenBudget = it }
        }

    /** A single field validator is used for inline feedback and pre-save validation. */
    private fun numericError(label: String, raw: String): String? {
        if (raw.isBlank()) return null // No typed value is mandatory.
        val normalized = normalizeNumericText(raw)
        val integers = mapOf(
            "Top-k" to 0L,
            "Max output tokens" to 1L,
            "Context window tokens" to 1L,
            "Timeout seconds" to 1L,
            "Seed" to -1L,
            "Reasoning token budget" to -1L,
        )
        val decimals = mapOf(
            "Temperature" to (0.0 to 5.0),
            "Top-p" to (0.0 to 1.0),
            "Repeat penalty" to (0.0 to 10.0),
            "Presence penalty" to (-2.0 to 2.0),
            "Frequency penalty" to (-2.0 to 2.0),
        )
        if (label in integers) {
            val minimum = requireNotNull(integers[label])
            val number = normalized.toLongOrNull() ?: return "Enter a whole number (no commas or decimal points)."
            if (number < minimum) return "Value must be at least $minimum."
            if (label in listOf(
                    "Top-k",
                    "Max output tokens",
                    "Context window tokens",
                    "Reasoning token budget"
                ) && number > Int.MAX_VALUE
            )
                return "Value must be at most ${Int.MAX_VALUE}."
            return null
        }
        val bounds = decimals[label] ?: return "Unsupported numeric field."
        val number = normalized.toDoubleOrNull() ?: return "Enter a number, using a comma or dot as decimal separator."
        if (!number.isFinite() || number !in bounds.first..bounds.second)
            return "Value must be between ${bounds.first} and ${bounds.second}."
        return null
    }

    private fun normalizeNumericText(value: String): String = value.trim().replace(',', '.')

    private fun validationIssue(model: DraftModel): String? {
        val s = model.inferenceSettings
        val fields = listOf(
            "Temperature" to s.temperature,
            "Top-p" to s.topP,
            "Top-k" to s.topK,
            "Max output tokens" to s.maxOutputTokens,
            "Context window tokens" to s.contextWindowTokens,
            "Timeout seconds" to s.timeoutSeconds,
            "Seed" to s.seed,
            "Repeat penalty" to s.repeatPenalty,
            "Presence penalty" to s.presencePenalty,
            "Frequency penalty" to s.frequencyPenalty,
            "Reasoning token budget" to s.reasoningTokenBudget,
        )
        for ((label, value) in fields) {
            val error = numericError(label, value)
            if (error != null) return "${model.displayName}: $label — $error"
        }
        return null
    }

    private fun inferenceField(
        model: DraftModel,
        label: String,
        current: () -> String,
        editable: Boolean,
        real: Boolean,
        update: (String) -> Unit,
    ): JBTextField = JBTextField(current()).apply {
        isEnabled = editable
        val standardBorder = border
        var lastWarning: String? = null

        fun capture() {
            if (!editable || saving) return
            val entered = text.trim()
            val warning = numericError(label, entered)
            if (warning != lastWarning) {
                lastWarning = warning
                border = if (warning == null) standardBorder else BorderFactory.createLineBorder(JBColor.RED)
                toolTipText =
                    warning ?: if (real) "Blank = preserve current saved value." else "Blank = Auto (simulation only)."
            }
            if (entered != current()) {
                update(entered)
                markDirty("Inference settings pending save: ${model.displayName}", render = false)
            }
        }
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = capture()
            override fun removeUpdate(event: DocumentEvent) = capture()
            override fun changedUpdate(event: DocumentEvent) = capture()
        })
        addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                val entered = text.trim()
                if (entered.isNotEmpty() && numericError(label, entered) == null) {
                    val normalized = normalizeNumericText(entered)
                    if (text != normalized) text = normalized
                }
                capture()
            }
        })
        addActionListener { capture() }
    }

    private fun validateInferenceSettings(draft: DraftModel) {
        validationIssue(draft)?.let { error(it) }
    }

    private fun profileWithDraftSettings(profile: ModelProfile, draft: DraftModel): ModelProfile {
        val settings = draft.inferenceSettings
        validateInferenceSettings(draft)
        // The current ModelProfile API has non-null numeric fields. Empty input must NOT
        // manufacture a fake "runtime default": preserve the existing persisted value.
        // To implement omission from requests, a nullable/override-aware backend contract is needed.
        fun decimalOrPrevious(raw: String, previous: Double): Double =
            raw.takeIf(String::isNotBlank)?.let { normalizeNumericText(it).toDouble() } ?: previous

        fun intOrPrevious(raw: String, previous: Int): Int =
            raw.takeIf(String::isNotBlank)?.toInt() ?: previous

        fun timeoutOrPrevious(raw: String, previous: java.time.Duration): java.time.Duration =
            raw.takeIf(String::isNotBlank)?.let { java.time.Duration.ofSeconds(it.toLong()) } ?: previous

        val contextWindow = settings.contextWindowTokens.takeIf(String::isNotBlank)?.toInt()
        return profile.copy(
            displayName = draft.displayName,
            temperature = decimalOrPrevious(settings.temperature, profile.temperature),
            topP = decimalOrPrevious(settings.topP, profile.topP),
            maxOutputTokens = intOrPrevious(settings.maxOutputTokens, profile.maxOutputTokens),
            timeout = timeoutOrPrevious(settings.timeoutSeconds, profile.timeout),
            contextWindowTokens = contextWindow,
            thinkingLevel = settings.thinkingLevel,
        )
    }

    private fun ModelProfile.toDraftInferenceSettings() = DraftInferenceSettings(
        temperature = temperature.toString(),
        topP = topP.toString(),
        maxOutputTokens = maxOutputTokens.toString(),
        timeoutSeconds = timeout.seconds.toString(),
        contextWindowTokens = contextWindowTokens?.toString().orEmpty(),
        thinkingLevel = thinkingLevel,
    )

    private fun compactButton(text: String, action: () -> Unit): JButton = JButton(text).apply {
        isFocusable = false
        isEnabled = !saving
        addActionListener { if (!saving) action() }
    }

    private fun tag(text: String, accent: Boolean = false): JBLabel = JBLabel(if (accent) "◆ $text" else text).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border()),
            JBUI.Borders.empty(2, 5)
        )
        toolTipText = text
    }

    private fun markDirty(message: String, render: Boolean = true) {
        dirty = true
        setStatus(message)
        updateSaveState()
        if (render) renderCards()
    }

    private fun updateSaveState() {
        savedState.text = when {
            saving -> "Saving model list…"
            dirty -> "Unsaved model list changes"
            else -> "Model list is saved"
        }
        val invalid = draftModels.asSequence()
            .filterNot { it.modelId in removedModelIds }
            .mapNotNull(::validationIssue)
            .firstOrNull()
        saveButton.isEnabled = dirty && !saving && invalid == null
        discardButton.isEnabled = dirty && !saving
        if (invalid != null && !saving) setStatus("Fix invalid input: $invalid")
    }

    private fun setStatus(message: String) {
        status.text = message
        status.toolTipText = message
    }

    private fun firstEligibleModelKey(): String? =
        draftModels.firstOrNull { it.modelId !in removedModelIds && isEligibleDefault(it) }?.key

    private fun isEligibleDefault(model: DraftModel): Boolean =
        model.modelId == null || (model.accessible && model.profileId != null && model.reconnectPath == null)

    private fun sizeText(model: DraftModel): String {
        if (!model.accessible && model.reconnectPath == null) return "Size unavailable"
        return runCatching {
            val bytes = Files.size(Path.of(model.effectivePath))
            "%.1f MiB".format(Locale.ROOT, bytes / (1024.0 * 1024.0))
        }.getOrDefault("Size unavailable")
    }

    private fun architectureText(model: DraftModel): String =
        metadataByModelKey[model.key]?.architecture ?: "Not inspected"

    private fun quantizationText(model: DraftModel): String =
        metadataByModelKey[model.key]?.quantization ?: "Not inspected"

    private fun metadataContextText(model: DraftModel): String = metadataByModelKey[model.key]?.contextLength
        ?.let { "$it tokens" }
        ?: "Not inspected"

    private fun parameterCountText(model: DraftModel): String = metadataByModelKey[model.key]?.parameterCount
        ?.let { "%,d".format(Locale.ROOT, it) }
        ?: "Not recorded in GGUF"

    private fun tokenizerText(model: DraftModel): String =
        metadataByModelKey[model.key]?.tokenizerModel ?: "Not inspected"

    private fun declaredFeaturesText(model: DraftModel): String = metadataByModelKey[model.key]
        ?.declaredFeatures
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString()
        ?: "None declared or not inspected"

    private fun fileName(path: String): String = runCatching { Path.of(path).fileName.toString() }.getOrDefault(path)
    private fun sameCanonicalPath(first: String, second: String): Boolean = canonicalKey(first) == canonicalKey(second)
    private fun canonicalKey(path: String): String = path.lowercase(Locale.ROOT)
}
