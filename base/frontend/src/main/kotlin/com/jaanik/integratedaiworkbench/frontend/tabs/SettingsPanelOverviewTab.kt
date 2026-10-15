package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.backend.service.WorkbenchRuntimeAvailability
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsState
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.border.TitledBorder

/**
 * Read-only operational overview. Configuration remains in its owning Workbench tabs;
 * this screen only reports state that is backed by an existing service or registry.
 */
class SettingsPanelOverviewTab(private val context: WorkbenchFrontendContext) :
    JPanel(BorderLayout(JBUI.scale(6), JBUI.scale(6))), RefreshableWorkbenchTab {

    private val sections = JTabbedPane()
    private val status = JBLabel("Refreshing saved Workbench state…")

    private val models = JBLabel("Loading…")
    private val providers = JBLabel("Loading…")
    private val library = JBLabel("Loading…")
    private val runtime = JBLabel("Loading…")
    private val workflow = JBLabel("Not implemented")
    private val observability = JBLabel("Not instrumented")

    private val managedRuntimePolicy = JBLabel()
    private val runtimeAgentsPolicy = JBLabel()
    private val groundingPolicy = JBLabel()
    private val mcpPolicy = JBLabel()
    private val authorizations = JBLabel("Not implemented — no task authorization store is available.")

    private val settingsStorage = JBLabel()
    private val providerSecrets = JBLabel()
    private val libraryStorage = JBLabel()
    private val providerTransmission = JBLabel()
    private val retention = JBLabel("Not implemented — no retention or content-logging service is available.")
    private val cleanup = JBLabel("Not implemented — no scoped history, context or cache deletion API is available.")

    private val plugin = JBLabel()
    private val platform = JBLabel()
    private val runtimeEnvironment = JBLabel()
    private val repository = JBLabel("https://github.com/jaanik007/integrated-ai-workbench")

    private var refreshRequest = 0L
    private var latestSnapshot: OverviewSnapshot? = null

    init {
        border = JBUI.Borders.empty(4)
        sections.accessibleContext.accessibleName = "Overview sections"
        sections.addTab("Dashboard", dashboardPanel())
        sections.addTab("Security", securityPanel())
        sections.addTab("Privacy", privacyPanel())
        sections.addTab("About", aboutPanel())
        sections.setMnemonicAt(0, KeyEvent.VK_D)
        sections.setMnemonicAt(1, KeyEvent.VK_S)
        sections.setMnemonicAt(2, KeyEvent.VK_P)
        sections.setMnemonicAt(3, KeyEvent.VK_A)
        add(sections, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
    }

    private fun dashboardPanel() = scrollableStack(
        section(
            "Operational summary",
            row("Models", models, "Open Models") { context.openTab(1) },
            row("Providers", providers, "Open Providers") { context.openTab(2) },
            row("Capabilities / library", library, "Open Capabilities") { context.openTab(3) },
            row("Managed local runtime", runtime, "Open Execute") { context.openTab(4) },
        ),
        section(
            "Workflow and observability",
            row("Current workflow", workflow),
            row("Recorded runs and metrics", observability),
            JBLabel("Refresh reads saved state only. It does not start a model or probe a provider."),
            buttonBar("Refresh" to ::refreshFromServices),
        ),
    )

    private fun securityPanel() = scrollableStack(
        section(
            "Configured security boundaries",
            row("Managed llama.cpp", managedRuntimePolicy, "Configure") { context.openTab(2) },
            row("Runtime agents", runtimeAgentsPolicy, "Configure") { context.openTab(3) },
            row("Project grounding", groundingPolicy, "Configure") { context.openTab(3) },
            row("IntelliJ MCP tools", mcpPolicy, "Configure") { context.openTab(3) },
        ),
        section(
            "Task authorizations",
            row("Active grants", authorizations),
            JBLabel("Role and document assignments define context; they do not grant filesystem, terminal, MCP or database access."),
        ),
    )

    private fun privacyPanel() = scrollableStack(
        section(
            "Stored locally",
            row("Application settings", settingsStorage),
            row("Provider credentials", providerSecrets),
            row("Workbench library", libraryStorage),
        ),
        section(
            "Transmission and controls",
            row("Configured external providers", providerTransmission, "Open Providers") { context.openTab(2) },
            row("Retention / content logging", retention),
            row("Delete stored content", cleanup),
            JBLabel("The Overview never displays API keys, prompt bodies, provider endpoints or library document content."),
        ),
    )

    private fun aboutPanel() = scrollableStack(
        section(
            "Installed plugin and environment",
            row("Plugin", plugin),
            row("IntelliJ Platform", platform),
            row("Java runtime", runtimeEnvironment),
            row("Source repository", repository),
        ),
        section(
            "Diagnostics",
            JBLabel("Copies versions and component availability only; paths, credentials, prompts and document content are excluded."),
            buttonBar("Copy redacted diagnostics" to ::copyDiagnostics),
        ),
    )

    private fun scrollableStack(vararg components: java.awt.Component): JBScrollPane {
        val stack = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        components.forEach {
            stack.add(it)
            stack.add(JPanel().apply { preferredSize = java.awt.Dimension(1, JBUI.scale(6)) })
        }
        return JBScrollPane(stack).apply { border = JBUI.Borders.empty() }
    }

    private fun section(title: String, vararg components: java.awt.Component): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = TitledBorder(title)
        components.forEach(::add)
    }

    private fun row(
        label: String,
        value: JBLabel,
        actionTitle: String? = null,
        action: (() -> Unit)? = null,
    ): JPanel = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
        border = JBUI.Borders.empty(JBUI.scale(4), JBUI.scale(5))
        add(JBLabel(label), BorderLayout.WEST)
        add(value, BorderLayout.CENTER)
        if (actionTitle != null && action != null) add(JButton(actionTitle).apply { addActionListener { action() } }, BorderLayout.EAST)
    }

    private fun buttonBar(vararg actions: Pair<String, () -> Unit>) = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
        actions.forEach { (title, action) -> add(JButton(title).apply { addActionListener { action() } }) }
    }

    override fun refreshFromServices() {
        val request = ++refreshRequest
        status.text = "Refreshing saved Workbench state…"
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching(::loadSnapshot)
            ApplicationManager.getApplication().invokeLater {
                if (request != refreshRequest) return@invokeLater
                result.fold(
                    onSuccess = { snapshot ->
                        latestSnapshot = snapshot
                        render(snapshot)
                        status.text = "Saved Workbench state refreshed."
                    },
                    onFailure = {
                        status.text = "Could not refresh Overview: ${it.javaClass.simpleName}"
                    },
                )
            }
        }
    }

    private fun loadSnapshot(): OverviewSnapshot {
        val settings = context.settings.snapshot()
        val registeredModels = context.modelRegistry.registeredModels()
        val providers = context.providerRegistry.registeredProviders()
        val libraryRepository = WorkbenchLibrary(
            Path.of(PathManager.getConfigPath()).resolve("integrated-ai-workbench"),
            context.project.basePath?.let(Path::of),
        )
        val librarySummary = runCatching {
            if (libraryRepository.databaseExists()) {
                val roles = libraryRepository.roles()
                val documents = libraryRepository.documents()
                "${roles.size} roles (${roles.count { it.scope == WorkbenchLibrary.Scope.GLOBAL }} global, " +
                    "${roles.count { it.scope == WorkbenchLibrary.Scope.PROJECT }} project); " +
                    "${documents.size} documents (${documents.count { it.scope == WorkbenchLibrary.Scope.GLOBAL }} global, " +
                    "${documents.count { it.scope == WorkbenchLibrary.Scope.PROJECT }} project)"
            } else {
                "Not initialized — opening Overview does not create the library database."
            }
        }.getOrElse { "Unavailable — library summary could not be read." }
        val selected = registeredModels.firstOrNull { it.profile?.id == settings.defaultModelProfileId }
        val availability = context.workbench.runtimeAvailability(settings.defaultModelProfileId)
        val descriptor = PluginManagerCore.getPlugin(PluginId.getId("integrated-ai-workbench"))
        return OverviewSnapshot(
            settings = settings,
            modelSummary = "${registeredModels.size} registered; ${registeredModels.count { it.isAccessible }} accessible; " +
                "${registeredModels.count { !it.isAccessible }} unavailable; default: ${selected?.displayName ?: "None"}",
            providerSummary = "${providers.size} configured (${providers.count { it.source.equals("DISCOVERED", true) }} discovered); " +
                "0 connection-verified in persisted state; verification is NOT_CHECKED after restart.",
            providerCount = providers.size,
            librarySummary = librarySummary,
            availability = availability,
            pluginSummary = "${descriptor?.name ?: "Integrated AI Workbench"} · ${descriptor?.pluginId?.idString ?: "integrated-ai-workbench"} · ${descriptor?.version ?: "Unknown version"}",
            platformSummary = "${ApplicationInfo.getInstance().fullApplicationName} · build ${ApplicationInfo.getInstance().build.asString()}",
            javaSummary = "${System.getProperty("java.runtime.name", "Unknown runtime")} ${System.getProperty("java.runtime.version", "Unknown version")}",
        )
    }

    private fun render(snapshot: OverviewSnapshot) {
        models.text = snapshot.modelSummary
        providers.text = snapshot.providerSummary
        library.text = snapshot.librarySummary
        runtime.text = runtimeSummary(snapshot.settings, snapshot.availability)
        managedRuntimePolicy.text = enabled(snapshot.settings.managedLlamaCppEnabled, "enforced before local execution")
        runtimeAgentsPolicy.text = enabled(snapshot.settings.runtimeAgentsEnabled, "agent workflows reject execution when disabled")
        groundingPolicy.text = enabled(snapshot.settings.projectGroundingEnabled, "requested project paths are rejected when disabled")
        mcpPolicy.text = enabled(snapshot.settings.readOnlyMcpToolsEnabled, "only permitted read-only MCP tooling can be requested")
        settingsStorage.text = "Persistent application preferences and local model metadata; no model contents."
        providerSecrets.text = "Provider metadata is persisted; credentials are held by IntelliJ PasswordSafe."
        libraryStorage.text = if (snapshot.librarySummary.startsWith("Not initialized")) {
            "Not initialized."
        } else {
            "SQLite metadata plus immutable local documents; content stays on this machine."
        }
        providerTransmission.text = if (snapshot.providerCount == 0) {
            "No external provider metadata is configured."
        } else {
            "Manual provider probes can contact configured endpoints; remote API keys are refused over non-loopback HTTP."
        }
        plugin.text = snapshot.pluginSummary
        platform.text = snapshot.platformSummary
        runtimeEnvironment.text = snapshot.javaSummary
    }

    private fun runtimeSummary(settings: WorkbenchSettingsState, availability: WorkbenchRuntimeAvailability): String {
        if (!settings.managedLlamaCppEnabled) return "Disabled by saved Workbench policy."
        val reason = if (settings.selectedProviderId == "llama-cpp-gpu") availability.gpuReason else availability.cpuReason
        val selected = if (settings.selectedProviderId == "llama-cpp-gpu") "GPU" else "CPU"
        return "$selected: $reason No runtime readiness or inference result is claimed here."
    }

    private fun enabled(value: Boolean, enabledDetail: String): String =
        if (value) "Enabled — $enabledDetail." else "Disabled by saved Workbench policy."

    private fun copyDiagnostics() {
        val snapshot = latestSnapshot ?: return
        val diagnostics = listOf(
            "Integrated AI Workbench diagnostics (redacted)",
            "Plugin: ${snapshot.pluginSummary}",
            "Platform: ${snapshot.platformSummary}",
            "Java: ${snapshot.javaSummary}",
            "Models: ${snapshot.modelSummary.substringBefore("; default:")}",
            "Providers: ${snapshot.providerCount} configured; connection verification is not persisted.",
            "Library: ${if (snapshot.librarySummary.startsWith("Not initialized")) "not initialized" else "initialized"}",
            "Managed runtime: ${if (snapshot.settings.managedLlamaCppEnabled) "enabled" else "disabled"}; readiness not verified.",
            "Observability: not instrumented; task authorization store: not implemented.",
        ).joinToString("\n")
        CopyPasteManager.getInstance().setContents(StringSelection(diagnostics))
        status.text = "Redacted diagnostics copied to the clipboard."
    }

    private data class OverviewSnapshot(
        val settings: WorkbenchSettingsState,
        val modelSummary: String,
        val providerSummary: String,
        val providerCount: Int,
        val librarySummary: String,
        val availability: WorkbenchRuntimeAvailability,
        val pluginSummary: String,
        val platformSummary: String,
        val javaSummary: String,
    )
}
