package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.jaanik.integratedaiworkbench.backend.provider.PersistedProviderRecord
import com.jaanik.integratedaiworkbench.backend.runtime.InferenceCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/** Modeless actual-inference chat, independent of the Settings window's lifecycle. */
internal object WorkbenchFloatingChat {
    sealed interface Target {
        val title: String

        data class Local(
            override val title: String,
            val complete: suspend (String, InferenceCancellation) -> String,
        ) : Target

        data class Remote(
            val provider: PersistedProviderRecord,
            val modelId: String,
            val apiKeyLookup: () -> String?,
        ) : Target {
            override val title: String get() = "${provider.displayName} · $modelId"
        }
    }

    private const val MAX_HISTORY_CHARS = 32_000
    private const val MAX_HISTORY_TURNS = 16

    fun open(project: Project, target: Target, system: String, roleName: String?) {
        val owner = WindowManager.getInstance().getFrame(project)
        val dialog = JDialog(owner, "Workbench Chat · ${roleName ?: target.title}", false)
        val worker = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val closed = AtomicBoolean(false)
        val history = mutableListOf<ProviderInferenceClient.Turn>()
        val output = JBTextArea().apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
        val entry = JBTextArea(4, 55).apply { lineWrap = true; wrapStyleWord = true }
        val status = JBLabel("Ready · Enter to send · Shift+Enter for a new line")
        val post = JButton("Post")
        val cancel = JButton("Cancel").apply { isEnabled = false }
        val clear = JButton("Clear context")
        val save = JButton("Save conversation")
        var activeLocal: InferenceCancellation? = null
        var activeRemote: ProviderInferenceClient.Cancellation? = null
        var running = false

        fun redraw() {
            output.text = buildString {
                append("Target: ${target.title}\n")
                if (roleName != null) append("Role: $roleName\n")
                append("History stays in this window unless explicitly saved.\n\n")
                for (turn in history) {
                    append(if (turn.role == "user") "You" else "Assistant")
                    append(":\n${turn.content}\n\n")
                }
            }
            output.caretPosition = output.document.length
        }

        /** Prior turns are included in every request: native llama.cpp receives text transcript,
         * while API providers receive their structured chat-messages format. */
        fun localPrompt(messages: List<ProviderInferenceClient.Turn>): String = buildString {
            if (system.isNotBlank()) append("SYSTEM INSTRUCTIONS:\n$system\n\n")
            append("CONVERSATION:\n")
            for (turn in messages) {
                append(if (turn.role == "user") "User" else "Assistant")
                append(":\n${turn.content}\n\n")
            }
            append("Assistant:\n")
        }

        fun submit() {
            if (running || closed.get()) return
            val message = entry.text.trim()
            if (message.isBlank()) return
            // Stop context from silently growing beyond a bounded request size.
            val prior = history.takeLast(MAX_HISTORY_TURNS)
            val priorText = prior.sumOf { it.content.length }
            if (message.length + system.length + priorText > MAX_HISTORY_CHARS) {
                status.text = "Context too large. Clear context or save and start a new conversation."
                return
            }
            val input = prior + ProviderInferenceClient.Turn("user", message)
            entry.text = ""
            running = true; post.isEnabled = false; clear.isEnabled = false; cancel.isEnabled = true
            status.text = "Generating…"
            worker.launch {
                val answer = runCatching {
                    when (target) {
                        is Target.Local -> {
                            val token = InferenceCancellation().also { activeLocal = it }
                            target.complete(localPrompt(input), token)
                        }

                        is Target.Remote -> {
                            val token = ProviderInferenceClient.Cancellation().also { activeRemote = it }
                            val messages = buildList {
                                if (system.isNotBlank()) add(ProviderInferenceClient.Turn("system", system))
                                addAll(input)
                            }
                            ProviderInferenceClient.complete(
                                target.provider,
                                target.modelId,
                                messages,
                                target.apiKeyLookup(),
                                token
                            ).content
                        }
                    }
                }
                SwingUtilities.invokeLater {
                    if (closed.get()) return@invokeLater
                    answer.onSuccess {
                        history.add(ProviderInferenceClient.Turn("user", message))
                        history.add(ProviderInferenceClient.Turn("assistant", it))
                        redraw()
                        status.text = "Response received · ${history.size / 2} turns"
                    }.onFailure {
                        entry.text = message
                        status.text = "Inference failed: ${it.message ?: "unknown error"}"
                    }
                    activeLocal = null; activeRemote = null; running = false
                    post.isEnabled = true; clear.isEnabled = true; cancel.isEnabled = false
                }
            }
        }

        post.addActionListener { submit() }
        cancel.addActionListener {
            activeLocal?.cancel(); activeRemote?.cancel()
            status.text = "Cancellation requested…"
        }
        clear.addActionListener {
            if (running) return@addActionListener
            history.clear(); redraw()
            status.text = "Local conversation context cleared. Provider-side caches may persist."
        }
        save.addActionListener {
            if (running) return@addActionListener
            val chooser = JFileChooser().apply {
                dialogTitle = "Save Workbench Conversation"
                selectedFile = java.io.File("workbench-chat-${System.currentTimeMillis()}.md")
            }
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                runCatching {
                    val path = chooser.selectedFile.toPath()
                    val markdown = buildString {
                        append("# Workbench Chat\n\nSaved: ${Instant.now()}\n\n")
                        append("Target: ${target.title}\n\n")
                        if (roleName != null) append("Role: $roleName\n\n")
                        for (turn in history) {
                            append("## ${if (turn.role == "user") "User" else "Assistant"}\n\n")
                            append(turn.content); append("\n\n")
                        }
                    }
                    Files.writeString(
                        path,
                        markdown,
                        StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE_NEW
                    )
                }.onSuccess { status.text = "Conversation saved." }
                    .onFailure { status.text = "Save failed: ${it.message ?: "unknown error"}" }
            }
        }
        entry.inputMap.put(KeyStroke.getKeyStroke("ENTER"), "post-message")
        entry.actionMap.put("post-message", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) = submit()
        })
        redraw()
        dialog.contentPane = JPanel(BorderLayout(8, 8)).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(JBScrollPane(output), BorderLayout.CENTER)
            add(JPanel(BorderLayout(0, 6)).apply {
                add(JBScrollPane(entry).apply { preferredSize = Dimension(400, 100) }, BorderLayout.CENTER)
                add(JPanel(BorderLayout()).apply {
                    add(status, BorderLayout.CENTER)
                    add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                        add(save); add(clear); add(cancel); add(post)
                    }, BorderLayout.EAST)
                }, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
        }
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                closed.set(true)
                activeLocal?.cancel(); activeRemote?.cancel()
                worker.cancel()
            }
        })
        dialog.minimumSize = Dimension(500, 360)
        dialog.setSize(760, 660)
        dialog.setLocationRelativeTo(owner)
        dialog.isVisible = true
        entry.requestFocusInWindow()
    }
}
