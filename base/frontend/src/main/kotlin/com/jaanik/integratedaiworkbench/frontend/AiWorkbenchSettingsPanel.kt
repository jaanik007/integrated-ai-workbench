package com.jaanik.integratedaiworkbench.frontend

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.backend.model.PersistentModelRegistry
import com.jaanik.integratedaiworkbench.backend.provider.PersistentProviderRegistry
import com.jaanik.integratedaiworkbench.backend.service.AiWorkbenchService
import com.jaanik.integratedaiworkbench.frontend.tabs.RefreshableWorkbenchTab
import com.jaanik.integratedaiworkbench.frontend.tabs.SettingsPanelCapabilitiesTab
import com.jaanik.integratedaiworkbench.frontend.tabs.SettingsPanelExecuteTab
import com.jaanik.integratedaiworkbench.frontend.tabs.SettingsPanelModelsTab
import com.jaanik.integratedaiworkbench.frontend.tabs.SettingsPanelOverviewTab
import com.jaanik.integratedaiworkbench.frontend.tabs.SettingsPanelProvidersTab
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import java.awt.BorderLayout
import javax.swing.JPanel

/** The shared native Tool Window panel and lifecycle owner for the five Workbench tabs. */
class AiWorkbenchSettingsPanel(project: Project) : JPanel(BorderLayout()), Disposable {
    private val refreshableTabs = mutableListOf<RefreshableWorkbenchTab>()
    private val tabs = JBTabbedPane()
    private val context = WorkbenchFrontendContext(project, ::refreshAll, ::selectTab)

    init {
        border = JBUI.Borders.empty(8)
        val overview = SettingsPanelOverviewTab(context)
        val models = SettingsPanelModelsTab(context)
        val providers = SettingsPanelProvidersTab(context)
        val capabilities = SettingsPanelCapabilitiesTab(context)
        val execute = SettingsPanelExecuteTab(context)
        refreshableTabs += listOf(overview, models, providers, capabilities, execute)
        Disposer.register(this, execute)

        tabs.apply {
            addTab("Overview", overview)
            addTab("Models", models)
            addTab("Providers", providers)
            addTab("Capabilities", capabilities)
            addTab("Execute", execute)
            addChangeListener { refreshableTabs.getOrNull(selectedIndex)?.refreshFromServices() }
        }
        add(tabs, BorderLayout.CENTER)
        refreshAll()
    }

    private fun selectTab(index: Int) {
        if (index in 0 until tabs.tabCount) tabs.selectedIndex = index
    }

    private fun refreshAll() {
        val refresh = { refreshableTabs.forEach(RefreshableWorkbenchTab::refreshFromServices) }
        if (ApplicationManager.getApplication().isDispatchThread) refresh()
        else ApplicationManager.getApplication().invokeLater(refresh)
    }

    override fun dispose() = Unit
}

/** Frontend-only access to existing services and notification between tabs. */
class WorkbenchFrontendContext(
    val project: Project,
    private val refresh: () -> Unit,
    private val selectTab: (Int) -> Unit,
) {
    val settings: WorkbenchSettingsService = service()
    val modelRegistry: PersistentModelRegistry = service()
    val providerRegistry: PersistentProviderRegistry = service()
    val workbench: AiWorkbenchService = project.service()
    val simulation = WorkbenchSimulationState()

    fun savedStateChanged() = refresh()

    fun openTab(index: Int) = selectTab(index)
}
