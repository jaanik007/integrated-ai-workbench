package com.jaanik.integratedaiworkbench.frontend

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import java.awt.BorderLayout
import javax.swing.JPanel

/** Compact dashboard; detailed configuration remains available in IDE Settings. */
class AiWorkbenchPanel(project: Project) : JPanel(BorderLayout()), Disposable {
    private val execution = WorkbenchExecutionPanel(project)

    init {
        border = JBUI.Borders.empty(8)

        val settings = service<WorkbenchSettingsService>().snapshot()
        val tabs = JBTabbedPane().apply {
            addTab("Overview", overview(settings))
            addTab("Models", ModelCatalogPanel(project))
            addTab("Providers", InferenceProviderPanel())
            addTab("Capabilities", WorkbenchFeaturePanel())
            if (settings.showExecutionPanel) addTab("Execute", execution)
        }

        add(tabs, BorderLayout.CENTER)
        Disposer.register(this, execution)
    }

    private fun overview(settings: com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsState): JPanel =
        JPanel(BorderLayout()).apply {
            val directory = settings.defaultModelDirectory.ifBlank { "No default model directory configured" }
            val text = """
                <html>
                <b>Integrated AI Workbench</b><br><br>
                Configure local models, providers and capability permissions here or in
                Settings | Tools | Integrated AI Workbench.<br><br>
                <b>Default model directory:</b> ${escape(directory)}<br>
                <b>Managed llama.cpp:</b> ${enabled(settings.managedLlamaCppEnabled)}<br>
                <b>Runtime agents:</b> ${enabled(settings.runtimeAgentsEnabled)}<br>
                <b>Project grounding:</b> ${enabled(settings.projectGroundingEnabled)}<br>
                <b>Read-only MCP tools:</b> ${enabled(settings.readOnlyMcpToolsEnabled)}
                </html>
            """.trimIndent()
            add(JBLabel(text), BorderLayout.NORTH)
        }

    private fun enabled(value: Boolean): String = if (value) "Enabled" else "Disabled"

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    override fun dispose() = Unit
}
