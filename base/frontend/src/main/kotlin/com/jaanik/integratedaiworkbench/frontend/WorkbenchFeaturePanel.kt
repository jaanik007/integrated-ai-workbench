package com.jaanik.integratedaiworkbench.frontend

import com.intellij.openapi.components.service
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

/** Feature preferences whose values are enforced by [AiWorkbenchService]. */
class WorkbenchFeaturePanel : JPanel(BorderLayout(0, JBUI.scale(8))) {
    private val settings = service<WorkbenchSettingsService>()
    private val managedRuntime = JBCheckBox("Allow Workbench-managed llama.cpp")
    private val runtimeAgents = JBCheckBox("Allow runtime agents and skills")
    private val grounding = JBCheckBox("Allow explicitly selected project grounding")
    private val readOnlyTools = JBCheckBox("Allow permitted read-only IntelliJ MCP tools")
    private val showExecution = JBCheckBox("Show the Execute tab (reopen the Tool Window after saving)")
    private val status = JBLabel()

    init {
        border = JBUI.Borders.empty(4)
        val form = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            add(managedRuntime)
            add(runtimeAgents)
            add(grounding)
            add(readOnlyTools)
            add(showExecution)
        }
        add(form, BorderLayout.NORTH)
        add(
            JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                add(status)
                add(JButton("Save capability preferences").apply { addActionListener { save() } })
            },
            BorderLayout.SOUTH,
        )
        reset()
    }

    private fun reset() {
        settings.snapshot().also { value ->
            managedRuntime.isSelected = value.managedLlamaCppEnabled
            runtimeAgents.isSelected = value.runtimeAgentsEnabled
            grounding.isSelected = value.projectGroundingEnabled
            readOnlyTools.isSelected = value.readOnlyMcpToolsEnabled
            showExecution.isSelected = value.showExecutionPanel
        }
    }

    private fun save() = runCatching {
        settings.update(settings.snapshot().copy(
            managedLlamaCppEnabled = managedRuntime.isSelected,
            runtimeAgentsEnabled = runtimeAgents.isSelected,
            projectGroundingEnabled = grounding.isSelected,
            readOnlyMcpToolsEnabled = readOnlyTools.isSelected,
            showExecutionPanel = showExecution.isSelected,
        ))
    }.fold(
        onSuccess = { status.text = "Saved." },
        onFailure = { status.text = "Error: ${it.message ?: "Unable to save settings."}" },
    )
}
