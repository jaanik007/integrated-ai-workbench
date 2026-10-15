package com.jaanik.integratedaiworkbench.frontend.tabs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jaanik.integratedaiworkbench.backend.provider.PersistedProviderRecord
import com.jaanik.integratedaiworkbench.backend.provider.ProviderConnectionStatus
import com.jaanik.integratedaiworkbench.backend.provider.ProviderDiscoveryService
import com.jaanik.integratedaiworkbench.frontend.WorkbenchFrontendContext
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Settings editor for EXTERNAL inference endpoints only.
 *
 * The integrated llama.cpp runtime is controlled by the Workbench runtime/model services;
 * it is neither duplicated nor falsely reported as connected here.
 * All Swing state is confined to the EDT. Network, registry and credential work runs off it.
 */
class SettingsPanelProvidersTab(private val context: WorkbenchFrontendContext) :
    JPanel(BorderLayout(0, JBUI.scale(10))), RefreshableWorkbenchTab {

    private data class UiProvider(
        val id: String,
        var name: String,
        var type: String,
        val source: String,
        var endpoint: String,
        var protocol: String,
        var storedKey: Boolean = false,
        var credentialEndpoint: String = endpoint,
        var pendingKey: CharArray? = null,
        var clearStoredKey: Boolean = false,
        var connectionStatus: ProviderConnectionStatus = ProviderConnectionStatus.NOT_CHECKED,
        var discoveredModels: List<String> = emptyList(),
        var latencyMs: Long? = null,
        var lastChecked: String = "Never",
        var error: String? = null,
        var isNew: Boolean = false,
        var dirty: Boolean = false,
    ) {
        fun forgetPendingKey() {
            pendingKey?.fill('\u0000')
            pendingKey = null
        }
    }

    private data class ProbeTarget(
        val id: String,
        val endpoint: String,
        val protocol: String,
        val pendingKey: CharArray?,
        val clearStoredKey: Boolean,
        val isNew: Boolean,
        val credentialEndpoint: String,
    ) {
        fun wipe() = pendingKey?.fill('\u0000')
    }

    private data class ProbeOutcome(
        val id: String,
        val endpoint: String,
        val protocol: String,
        val status: ProviderConnectionStatus,
        val models: List<String>,
        val latency: Long,
        val error: String?,
        val checked: String,
    )

    private data class SaveTarget(
        val id: String,
        val name: String,
        val type: String,
        val source: String,
        val endpoint: String,
        val protocol: String,
        val storedKey: Boolean,
        val pendingKey: CharArray?,
        val clearStoredKey: Boolean,
    ) {
        fun wipe() = pendingKey?.fill('\u0000')
    }

    private val cards = object : JPanel(), Scrollable {
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
            JBUI.scale(24)

        override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
            JBUI.scale(240)

        override fun getScrollableTracksViewportWidth() = true
        override fun getScrollableTracksViewportHeight() = false
    }.apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private data class ResponsiveGroup(val panel: JPanel, val ribbon: Boolean)

    private val responsivePanels = mutableListOf<ResponsiveGroup>()
    private val listScroll = JBScrollPane(cards).apply {
        border = JBUI.Borders.emptyTop(0)
        viewportBorder = BorderFactory.createEmptyBorder()
        viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = updateResponsiveLayout()
        })
    }
    private val search = JBTextField().apply { emptyText.text = "Search provider, protocol or endpoint..." }
    private val count = JBLabel()
    private val status = JBLabel().apply { horizontalAlignment = SwingConstants.RIGHT }
    private val discoverButton = JButton("Discover local")
    private val addButton = JButton("Add provider")
    private val refreshButton = JButton("Test all")

    /** Changed only on the Swing event dispatch thread. */
    private var providers: MutableList<UiProvider> = mutableListOf()
    private val expanded = linkedSetOf<String>()
    private var busy = false
    private var loadGeneration = 0L

    init {
        border = JBUI.Borders.empty(4)
        add(header(), BorderLayout.NORTH)
        add(listScroll, BorderLayout.CENTER)
        add(footer(), BorderLayout.SOUTH)
        search.document.addDocumentListener(documentListener { renderCards() })
        discoverButton.addActionListener { discoverLocal() }
        addButton.addActionListener { addManualProvider() }
        refreshButton.addActionListener { testAll() }
        loadProviders()
    }

    private fun header(): JPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border()), JBUI.Borders.empty(10)
        )
        add(JPanel(BorderLayout()).apply {
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JBLabel("External inference providers").apply {
                    font = font.deriveFont(font.style or java.awt.Font.BOLD)
                })
                add(JBLabel("Integrated llama.cpp is managed by Workbench; discover or register additional services here."))
            }, BorderLayout.CENTER)
            add(count, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(search, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(5), 0)).apply {
                discoverButton.toolTipText = "Probe only known loopback addresses for Ollama and LM Studio."
                refreshButton.toolTipText =
                    "Query external model-catalog endpoints; a successful probe does not verify inference."
                add(discoverButton)
                add(addButton)
                add(refreshButton)
            }, BorderLayout.EAST)
        }, BorderLayout.SOUTH)
    }

    private fun footer(): JPanel = JPanel(BorderLayout(JBUI.scale(10), 0)).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border()), JBUI.Borders.empty(10)
        )
        add(
            JBLabel("Listed ≠ connected. A default is a saved preference, not a running connection."),
            BorderLayout.CENTER
        )
        add(status, BorderLayout.EAST)
    }

    override fun refreshFromServices() = loadProviders()

    /** Preserve unsaved edits during tab navigation; do not reset live probe state needlessly. */
    private fun loadProviders() {
        if (busy) return
        val generation = ++loadGeneration
        runBackground("Loading external providers...", work = {
            context.providerRegistry.registeredProviders()
                .filterNot { it.source.equals("BUILT_IN", ignoreCase = true) }
        }) { outcome ->
            if (generation != loadGeneration) return@runBackground
            outcome.fold(onSuccess = { records ->
                val previous = providers.associateBy { it.id }
                val merged = records.map { record ->
                    val old = previous[record.id]
                    if (old != null && (old.isNew || old.dirty)) old else UiProvider(
                        id = record.id,
                        name = record.displayName,
                        type = record.type,
                        source = record.source,
                        endpoint = record.endpoint,
                        protocol = record.protocol,
                        storedKey = record.hasApiKey,
                        credentialEndpoint = record.endpoint,
                        connectionStatus = if (old != null && old.endpoint == record.endpoint &&
                            old.protocol == record.protocol
                        ) old.connectionStatus else ProviderConnectionStatus.NOT_CHECKED,
                        discoveredModels = old?.takeIf { it.endpoint == record.endpoint && it.protocol == record.protocol }
                            ?.discoveredModels.orEmpty(),
                        latencyMs = old?.takeIf { it.endpoint == record.endpoint && it.protocol == record.protocol }?.latencyMs,
                        lastChecked = old?.takeIf { it.endpoint == record.endpoint && it.protocol == record.protocol }
                            ?.lastChecked ?: "Never",
                        error = old?.takeIf { it.endpoint == record.endpoint && it.protocol == record.protocol }?.error,
                    )
                }.toMutableList()
                previous.values.filter { it.isNew || it.dirty }.forEach { old ->
                    if (merged.none { it.id == old.id }) merged.add(old)
                }
                previous.values.filter { old -> merged.none { it === old } && old.pendingKey != null }
                    .forEach(UiProvider::forgetPendingKey)
                providers = merged
                renderCards()
                setStatus("${providers.size} external provider(s) listed")
            }, onFailure = { setStatus("Unable to load providers: ${safeError(it)}") })
        }
    }

    private fun discoverLocal() {
        if (busy) return
        val candidates = listOf(
            Triple("Ollama", "http://127.0.0.1:11434", "Ollama API"),
            Triple("LM Studio", "http://127.0.0.1:1234", "OpenAI Chat Completions /v1"),
        )
        runBackground("Checking known local inference ports...", work = {
            candidates.mapNotNull { (name, address, protocol) ->
                runCatching { ProviderDiscoveryService.probe(address, protocol) }.getOrNull()
                    ?.takeIf {
                        it.status == ProviderConnectionStatus.AVAILABLE ||
                                it.status == ProviderConnectionStatus.AUTH_REQUIRED
                    }
                    ?.let { result ->
                        name to ProbeOutcome(
                            id = "", endpoint = address, protocol = protocol,
                            status = result.status, models = result.models,
                            latency = result.latencyMs, error = result.errorMessage,
                            checked = DateTimeFormatter.ISO_INSTANT.format(result.checkedAt),
                        )
                    }
            }
        }) { outcome ->
            outcome.fold(onSuccess = { found ->
                var added = 0
                found.forEach { (name, probe) ->
                    val existing = providers.firstOrNull { sameEndpoint(it.endpoint, probe.endpoint) }
                    if (existing != null) {
                        if (existing.endpoint == probe.endpoint && existing.protocol == probe.protocol && !existing.dirty) {
                            applyProbe(existing, probe.copy(id = existing.id))
                        }
                    } else {
                        val id = "external-${UUID.randomUUID()}"
                        providers.add(
                            UiProvider(
                                id = id, name = name, type = name, source = "DISCOVERED",
                                endpoint = probe.endpoint, protocol = probe.protocol,
                                connectionStatus = probe.status, discoveredModels = probe.models,
                                latencyMs = probe.latency, lastChecked = probe.checked,
                                error = probe.error, isNew = true, dirty = true,
                            )
                        )
                        added++
                    }
                }
                renderCards()
                setStatus("Local discovery: ${found.size} responsive endpoint(s), $added new. Save to keep.")
            }, onFailure = { setStatus("Local discovery failed: ${safeError(it)}") })
        }
    }

    private fun addManualProvider() {
        if (busy) return
        val id = "external-${UUID.randomUUID()}"
        providers.add(
            UiProvider(
                id = id, name = "New provider", type = "OpenAI-compatible", source = "MANUAL",
                endpoint = "", protocol = "OpenAI Chat Completions /v1", isNew = true, dirty = true,
            )
        )
        expanded.add(id)
        renderCards()
        setStatus("Enter a name and endpoint, then Save and Test.")
    }

    private fun testAll() {
        if (busy) return
        val targets = providers.filter { it.endpoint.isNotBlank() }.map(::snapshotProbeTarget)
        if (targets.isEmpty()) {
            setStatus("No endpoints to test."); return
        }
        runBackground("Testing ${targets.size} external endpoint(s)...", work = {
            try {
                targets.map { target ->
                    runCatching { probe(target) }.getOrElse { error ->
                        ProbeOutcome(
                            target.id, target.endpoint, target.protocol,
                            ProviderConnectionStatus.NOT_CHECKED, emptyList(), 0L,
                            "Probe failed: ${safeError(error)}", "Failed"
                        )
                    }
                }
            } finally {
                targets.forEach(ProbeTarget::wipe)
            }
        }) { outcome ->
            outcome.fold(onSuccess = { results ->
                results.forEach { result ->
                    providers.firstOrNull {
                        it.id == result.id && it.endpoint == result.endpoint && it.protocol == result.protocol
                    }?.let { applyProbe(it, result) }
                }
                renderCards()
                setStatus("Tested ${results.size} external endpoint(s).")
            }, onFailure = { setStatus("Bulk test failed: ${safeError(it)}") })
        }
    }

    private fun testOne(provider: UiProvider) {
        if (busy) return
        val validation = validateEndpoint(provider.endpoint)
        if (validation != null) {
            setStatus(validation); return
        }
        val target = snapshotProbeTarget(provider)
        runBackground("Testing ${provider.name}...", work = {
            try {
                probe(target)
            } finally {
                target.wipe()
            }
        }) { outcome ->
            outcome.fold(onSuccess = { tested ->
                providers.firstOrNull {
                    it.id == tested.id && it.endpoint == tested.endpoint && it.protocol == tested.protocol
                }?.let { applyProbe(it, tested) }
                renderCards()
                setStatus("${provider.name}: ${tested.status} (${tested.latency} ms)")
            }, onFailure = {
                provider.connectionStatus = ProviderConnectionStatus.NOT_CHECKED
                provider.error = safeError(it)
                renderCards()
                setStatus("Connection test failed: ${safeError(it)}")
            })
        }
    }

    private fun snapshotProbeTarget(p: UiProvider) = ProbeTarget(
        id = p.id, endpoint = p.endpoint.trim(), protocol = p.protocol.trim(),
        pendingKey = p.pendingKey?.clone(), clearStoredKey = p.clearStoredKey,
        isNew = p.isNew, credentialEndpoint = p.credentialEndpoint,
    )

    private fun probe(target: ProbeTarget): ProbeOutcome {
        val validation = validateEndpoint(target.endpoint)
        if (validation != null) error(validation)
        // PasswordSafe-backed registry is only read during a real request, on a worker thread.
        val secret = when {
            target.pendingKey != null && target.pendingKey.isNotEmpty() -> String(target.pendingKey)
            target.clearStoredKey || target.isNew ||
                    !sameEndpoint(target.endpoint, target.credentialEndpoint) -> null

            else -> context.providerRegistry.getApiKey(target.id)
        }
        if (secret != null && URI(target.endpoint).scheme.equals("http", ignoreCase = true) &&
            !isLoopback(URI(target.endpoint).host.orEmpty())
        ) {
            error("A remote API key cannot be sent over unencrypted HTTP; use HTTPS.")
        }
        val result = ProviderDiscoveryService.probe(target.endpoint, target.protocol, secret)
        return ProbeOutcome(
            target.id, target.endpoint, target.protocol, result.status, result.models,
            result.latencyMs, result.errorMessage,
            DateTimeFormatter.ISO_INSTANT.format(result.checkedAt),
        )
    }

    private fun applyProbe(provider: UiProvider, result: ProbeOutcome) {
        provider.connectionStatus = result.status
        provider.discoveredModels = result.models
        provider.latencyMs = result.latency
        provider.lastChecked = result.checked
        provider.error = result.error
    }

    private fun saveProvider(provider: UiProvider) {
        if (busy) return
        val name = provider.name.trim()
        if (name.isEmpty()) {
            setStatus("Provider name is required."); return
        }
        val endpoint = provider.endpoint.trim().trimEnd('/')
        validateEndpoint(endpoint)?.let { setStatus(it); return }
        if (provider.protocol.isBlank()) {
            setStatus("Connection protocol is required."); return
        }
        if (provider.storedKey && !sameEndpoint(endpoint, provider.credentialEndpoint) &&
            provider.pendingKey?.isNotEmpty() != true && !provider.clearStoredKey
        ) {
            setStatus("Endpoint changed: enter a replacement API key or choose to delete the saved key.")
            return
        }
        if (providers.any { it.id != provider.id && sameEndpoint(it.endpoint, endpoint) }) {
            setStatus("This endpoint is already listed. Edit the existing provider.")
            return
        }
        val target = SaveTarget(
            provider.id, name, provider.type, provider.source, endpoint, provider.protocol.trim(),
            provider.storedKey, provider.pendingKey?.clone(), provider.clearStoredKey,
        )
        runBackground("Saving ${provider.name}...", work = {
            try {
                val replacement = target.pendingKey?.takeIf { it.isNotEmpty() }?.let(::String)
                // Null means KEEP the existing PasswordSafe credential; deletion is explicit.
                // Never read a stored key just to rewrite it under the same ID.
                if (replacement != null && URI(target.endpoint).scheme.equals("http", ignoreCase = true) &&
                    !isLoopback(URI(target.endpoint).host.orEmpty())
                ) {
                    error("Cannot save credentials for remote HTTP: use HTTPS.")
                }
                context.providerRegistry.saveProvider(
                    PersistedProviderRecord(
                        id = target.id, displayName = target.name, type = target.type,
                        source = target.source, endpoint = target.endpoint, protocol = target.protocol,
                        hasApiKey = !target.clearStoredKey && (replacement != null || target.storedKey),
                    ), replacement, clearApiKey = target.clearStoredKey
                )
                !target.clearStoredKey && (replacement != null || target.storedKey)
            } finally {
                target.wipe()
            }
        }) { outcome ->
            outcome.fold(onSuccess = { hasKey ->
                provider.name = target.name
                provider.endpoint = target.endpoint
                provider.protocol = target.protocol
                provider.storedKey = hasKey
                provider.credentialEndpoint = target.endpoint
                provider.forgetPendingKey()
                provider.clearStoredKey = false
                provider.isNew = false
                provider.dirty = false
                // A previously tested configuration can no longer be assumed valid after Save.
                invalidateProbe(provider)
                renderCards()
                setStatus("Saved ${provider.name}. Test connection before selecting it as default.")
                context.savedStateChanged()
            }, onFailure = { setStatus("Could not save provider: ${safeError(it)}") })
        }
    }

    private fun removeProvider(provider: UiProvider) {
        if (busy) return
        val answer = Messages.showYesNoDialog(
            context.project,
            "Remove '${provider.name}' from the Workbench catalog? This does not uninstall the service.",
            "Remove external provider", "Remove", "Cancel", Messages.getWarningIcon(),
        )
        if (answer != Messages.YES) return
        if (provider.isNew) {
            provider.forgetPendingKey()
            providers.removeIf { it.id == provider.id }
            expanded.remove(provider.id)
            renderCards()
            return
        }
        runBackground("Removing ${provider.name}...", work = {
            context.providerRegistry.removeProvider(provider.id)
            val wasDefault = context.settings.snapshot().selectedProviderId == provider.id
            if (wasDefault) {
                context.settings.update(context.settings.snapshot().copy(selectedProviderId = ""))
            }
            wasDefault
        }) { outcome ->
            outcome.fold(onSuccess = {
                provider.forgetPendingKey()
                providers.removeIf { p -> p.id == provider.id }
                expanded.remove(provider.id)
                renderCards()
                setStatus("Provider removed from the catalog.")
                context.savedStateChanged()
            }, onFailure = { setStatus("Could not remove provider: ${safeError(it)}") })
        }
    }

    /** Selection only changes a preference; it neither authenticates nor starts a service. */
    private fun selectDefault(provider: UiProvider) {
        if (busy || provider.isNew || provider.dirty || provider.connectionStatus != ProviderConnectionStatus.AVAILABLE) {
            setStatus("Save and successfully test the provider before selecting it.")
            return
        }
        runBackground("Setting default provider...", work = {
            context.settings.update(context.settings.snapshot().copy(selectedProviderId = provider.id))
        }) { outcome ->
            outcome.fold(onSuccess = {
                renderCards()
                setStatus("Default provider set: ${provider.name}. Catalog access verified; inference was not tested.")
                context.savedStateChanged()
            }, onFailure = { setStatus("Could not update default: ${safeError(it)}") })
        }
    }

    private fun validateEndpoint(raw: String): String? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull()
            ?: return "Enter a valid absolute http:// or https:// URL."
        if (!(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) || uri.host.isNullOrBlank()) {
            return "Endpoint must be an absolute HTTP(S) URL with a hostname."
        }
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) {
            return "Put credentials in the API Key field, not in the URL; remove query/fragment."
        }
        return null
    }

    private fun isLoopback(host: String): Boolean = host.equals("localhost", true) ||
            host == "127.0.0.1" || host == "[::1]" || host == "::1"

    private fun sameEndpoint(a: String, b: String): Boolean =
        a.trim().trimEnd('/').equals(b.trim().trimEnd('/'), ignoreCase = true)

    private fun invalidateProbe(p: UiProvider) {
        p.connectionStatus = ProviderConnectionStatus.NOT_CHECKED
        p.discoveredModels = emptyList()
        p.latencyMs = null
        p.lastChecked = "Never"
        p.error = null
    }

    private fun edited(p: UiProvider) {
        p.dirty = true
        invalidateProbe(p)
        setStatus("Unsaved changes for ${p.name}")
    }

    private fun renderCards() {
        val filter = search.text.trim().lowercase(Locale.ROOT)
        count.text = "${providers.size} external"
        responsivePanels.clear()
        cards.removeAll()
        val defaultId = context.settings.snapshot().selectedProviderId
        providers.asSequence()
            .filter {
                filter.isBlank() ||
                        listOf(it.name, it.type, it.endpoint, it.protocol).any { value ->
                            value.lowercase(Locale.ROOT).contains(filter)
                        }
            }
            .forEach { p ->
                cards.add(providerCard(p, p.id == defaultId))
                cards.add(Box.createVerticalStrut(JBUI.scale(8)))
            }
        if (cards.componentCount == 0) cards.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(22)
            add(JBLabel("No external providers match this list."))
            add(JBLabel("Discover local services or register a provider manually."))
        })
        discoverButton.isEnabled = !busy
        addButton.isEnabled = !busy
        refreshButton.isEnabled = !busy && providers.any { it.endpoint.isNotBlank() }
        updateResponsiveLayout()
        cards.revalidate()
        cards.repaint()
    }

    private fun providerCard(p: UiProvider, isDefault: Boolean): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(5))).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createLineBorder(JBColor.border())
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                border = JBUI.Borders.empty(9)
                add(JBLabel("◈").apply {
                    horizontalAlignment = SwingConstants.CENTER
                    preferredSize = Dimension(JBUI.scale(32), JBUI.scale(32))
                }, BorderLayout.WEST)
                add(JPanel(BorderLayout(0, JBUI.scale(4))).apply {
                    add(JPanel(BorderLayout()).apply {
                        add(JBLabel(p.name).apply {
                            font = font.deriveFont(font.style or java.awt.Font.BOLD)
                        }, BorderLayout.CENTER)
                        add(JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(5), 0)).apply {
                            add(tag(p.source))
                            add(tag(p.connectionStatus.name))
                            if (isDefault) add(tag("Default", true))
                            if (p.dirty || p.isNew) add(tag("Unsaved"))
                        }, BorderLayout.EAST)
                    }, BorderLayout.NORTH)
                    add(metadataRibbon(p), BorderLayout.CENTER)
                    add(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(5), 0)).apply {
                        add(button(if (p.id in expanded) "Hide details" else "Configure") {
                            if (!expanded.add(p.id)) expanded.remove(p.id)
                            renderCards()
                        })
                        add(button("Test connection") { testOne(p) }.apply {
                            isEnabled = !busy && p.endpoint.isNotBlank()
                        })
                        add(button("Save") { saveProvider(p) }.apply {
                            isEnabled = !busy && (p.isNew || p.dirty)
                        })
                        if (!isDefault) add(button("Set default") { selectDefault(p) }.apply {
                            isEnabled = !busy && !p.isNew && !p.dirty &&
                                    p.connectionStatus == ProviderConnectionStatus.AVAILABLE
                        })
                        add(button("Remove") { removeProvider(p) }.apply { isEnabled = !busy })
                    }, BorderLayout.SOUTH)
                }, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            if (p.id in expanded) add(providerDetails(p), BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

    private fun metadataRibbon(p: UiProvider): JPanel =
        JPanel(GridLayout(0, 3, JBUI.scale(8), JBUI.scale(4))).apply {
            responsivePanels.add(ResponsiveGroup(this, ribbon = true))
            add(tag("Type: ${p.type}"))
            add(tag("Endpoint: ${p.endpoint.ifBlank { "Not set" }.let(::truncate)}"))
            add(tag("Protocol: ${p.protocol}"))
            add(
                tag(
                    "Models: ${
                        when (p.connectionStatus) {
                            ProviderConnectionStatus.NOT_CHECKED -> "Not checked"
                            ProviderConnectionStatus.AUTH_REQUIRED -> "Authorization needed"
                            else -> p.discoveredModels.size.toString()
                        }
                    }"
                )
            )
            add(tag("Latency: ${p.latencyMs?.let { "$it ms" } ?: "—"}"))
            add(tag("Checked: ${p.lastChecked}"))
            p.error?.let { add(tag("Error: ${truncate(it)}")) }
        }

    private fun providerDetails(p: UiProvider): JPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()), JBUI.Borders.empty(10)
        )
        val form = JPanel(GridLayout(0, 2, JBUI.scale(10), JBUI.scale(7)))
        responsivePanels.add(ResponsiveGroup(form, ribbon = false))
        form.add(field("Display name", JBTextField(p.name).apply {
            isEnabled = !busy
            document.addDocumentListener(documentListener {
                if (p.name != text) {
                    p.name = text; edited(p)
                }
            })
        }))
        form.add(field("Endpoint URL", JBTextField(p.endpoint).apply {
            isEnabled = !busy
            document.addDocumentListener(documentListener {
                if (p.endpoint != text) {
                    p.endpoint = text; edited(p)
                }
            })
        }))
        form.add(field("Protocol", JBTextField(p.protocol).apply {
            isEnabled = !busy
            toolTipText = "Use 'Ollama API' or 'OpenAI Chat Completions /v1', or an adapter-specific value."
            document.addDocumentListener(documentListener {
                if (p.protocol != text) {
                    p.protocol = text
                    p.type = if (text == "Ollama API") "Ollama" else "OpenAI-compatible"
                    edited(p)
                }
            })
        }))
        form.add(field("API key (blank = keep saved key)", JPasswordField().apply {
            isEnabled = !busy
            toolTipText =
                if (p.storedKey && !p.clearStoredKey) "Credential stored securely; value is never displayed." else "Enter a new key; it will be stored by ProviderRegistry."
            document.addDocumentListener(documentListener {
                p.forgetPendingKey()
                p.pendingKey = password.takeIf { it.isNotEmpty() }
                p.clearStoredKey = false
                edited(p)
            })
        }))
        add(form, BorderLayout.NORTH)
        add(JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            if (p.storedKey) add(JCheckBox("Delete stored API key on next Save", p.clearStoredKey).apply {
                isEnabled = !busy
                addActionListener {
                    p.clearStoredKey = isSelected
                    if (isSelected) p.forgetPendingKey()
                    edited(p)
                }
            }, BorderLayout.NORTH)
            if (p.discoveredModels.isNotEmpty()) {
                val text = "Catalog models: ${p.discoveredModels.joinToString(", ")}"
                add(JBTextArea(text).apply {
                    lineWrap = true
                    wrapStyleWord = true
                    isEditable = false
                    isOpaque = false
                    rows = 3
                }, BorderLayout.CENTER)
            }
            p.error?.let { add(JBLabel("Last error: ${truncate(it)}").apply { toolTipText = it }, BorderLayout.SOUTH) }
        }, BorderLayout.CENTER)
    }

    private fun field(label: String, control: Component): JPanel = JPanel(BorderLayout(0, JBUI.scale(3))).apply {
        add(JBLabel(label), BorderLayout.NORTH)
        add(control, BorderLayout.CENTER)
    }

    private fun button(name: String, action: () -> Unit): JButton = JButton(name).apply {
        isFocusable = false
        addActionListener { if (!busy) action() }
    }

    private fun tag(value: String, accent: Boolean = false): JBLabel =
        JBLabel(if (accent) "◆ $value" else value).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()), BorderFactory.createEmptyBorder(2, 5, 2, 5)
            )
            toolTipText = value
        }

    private fun updateResponsiveLayout() {
        val width = listScroll.viewport.width.takeIf { it > 0 } ?: width.takeIf { it > 0 } ?: JBUI.scale(900)
        val columns = when {
            width >= JBUI.scale(1180) -> 3
            width >= JBUI.scale(650) -> 2
            else -> 1
        }
        responsivePanels.forEach { group ->
            val preferredColumns = if (group.ribbon) columns else if (width >= JBUI.scale(770)) 2 else 1
            val layout = group.panel.layout as? GridLayout
            if (layout != null && layout.columns != preferredColumns) {
                group.panel.layout = GridLayout(0, preferredColumns, JBUI.scale(8), JBUI.scale(4))
            }
        }
        cards.components.filterIsInstance<JPanel>().forEach { card ->
            card.maximumSize = Dimension(Int.MAX_VALUE, card.preferredSize.height)
        }
        cards.revalidate()
        cards.repaint()
    }

    private fun documentListener(action: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = action()
        override fun removeUpdate(event: DocumentEvent) = action()
        override fun changedUpdate(event: DocumentEvent) = action()
    }

    /** Executes blocking I/O off EDT, sends every result to EDT, and serializes UI operations. */
    private fun <T> runBackground(label: String, work: () -> T, done: (Result<T>) -> Unit) {
        if (busy) return
        busy = true
        setStatus(label)
        renderCards()
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { work() }
            ApplicationManager.getApplication().invokeLater {
                busy = false
                done(result)
                renderCards()
            }
        }
    }

    private fun safeError(error: Throwable): String = error.message?.take(250) ?: error.javaClass.simpleName
    private fun truncate(text: String): String = if (text.length <= 65) text else text.take(62) + "..."
    private fun setStatus(message: String) {
        status.text = message
        status.toolTipText = message
    }
}
