package com.jaanik.integratedaiworkbench.frontend

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class AiWorkbenchPanel : JPanel(BorderLayout()) {
    init {
        border = JBUI.Borders.empty(12)
        add(JBLabel("Integrated AI Workbench"), BorderLayout.NORTH)
        add(
            JBLabel(
                "<html>Bootstrap is complete.<br>"
                    + "Configure a local runtime and model profile in a later milestone.</html>",
            ),
            BorderLayout.CENTER,
        )
    }
}
