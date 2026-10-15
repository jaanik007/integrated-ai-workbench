package com.jaanik.integratedaiworkbench.frontend

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import javax.swing.JComponent
import javax.swing.JPanel

class WorkbenchSettingsConfigurable : Configurable {
    private val settingsService = service<WorkbenchSettingsService>()
    
    private val managedLlamaCppBox = JBCheckBox("Allow Workbench-managed llama.cpp")
    private val runtimeAgentsBox = JBCheckBox("Allow runtime agents and skills")
    private val projectGroundingBox = JBCheckBox("Allow explicitly selected project grounding")
    private val readOnlyMcpToolsBox = JBCheckBox("Allow permitted read-only IntelliJ MCP tools")
    private val showExecutionPanelBox = JBCheckBox("Show Execute tab in Tool Window")

    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Integrated AI Workbench"

    override fun createComponent(): JComponent {
        panel = FormBuilder.createFormBuilder()
            .addComponent(managedLlamaCppBox)
            .addComponent(runtimeAgentsBox)
            .addComponent(projectGroundingBox)
            .addComponent(readOnlyMcpToolsBox)
            .addComponent(showExecutionPanelBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val current = settingsService.snapshot()
        return managedLlamaCppBox.isSelected != current.managedLlamaCppEnabled ||
                runtimeAgentsBox.isSelected != current.runtimeAgentsEnabled ||
                projectGroundingBox.isSelected != current.projectGroundingEnabled ||
                readOnlyMcpToolsBox.isSelected != current.readOnlyMcpToolsEnabled ||
                showExecutionPanelBox.isSelected != current.showExecutionPanel
    }

    override fun apply() {
        val current = settingsService.snapshot()
        settingsService.update(current.copy(
            managedLlamaCppEnabled = managedLlamaCppBox.isSelected,
            runtimeAgentsEnabled = runtimeAgentsBox.isSelected,
            projectGroundingEnabled = projectGroundingBox.isSelected,
            readOnlyMcpToolsEnabled = readOnlyMcpToolsBox.isSelected,
            showExecutionPanel = showExecutionPanelBox.isSelected,
        ))
    }

    override fun reset() {
        val current = settingsService.snapshot()
        managedLlamaCppBox.isSelected = current.managedLlamaCppEnabled
        runtimeAgentsBox.isSelected = current.runtimeAgentsEnabled
        projectGroundingBox.isSelected = current.projectGroundingEnabled
        readOnlyMcpToolsBox.isSelected = current.readOnlyMcpToolsEnabled
        showExecutionPanelBox.isSelected = current.showExecutionPanel
    }

    override fun disposeUIResources() {
        panel = null
    }
}
