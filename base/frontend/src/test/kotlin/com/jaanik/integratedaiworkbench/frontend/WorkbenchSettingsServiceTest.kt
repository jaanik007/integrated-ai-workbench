package com.jaanik.integratedaiworkbench.frontend

import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorkbenchSettingsServiceTest {
    @Test
    fun `persists the selected managed provider and default model profile`() {
        val service = WorkbenchSettingsService()

        service.update(service.snapshot().copy(
            selectedProviderId = "llama-cpp-gpu",
            defaultModelProfileId = "local-profile",
            readOnlyMcpToolsEnabled = true,
        ))

        val state = service.snapshot()
        assertEquals("llama-cpp-gpu", state.selectedProviderId)
        assertEquals("local-profile", state.defaultModelProfileId)
        assertEquals(true, state.readOnlyMcpToolsEnabled)
    }

    @Test
    fun `retains legacy directory data while migrating to per file model registration`() {
        val service = WorkbenchSettingsService()
        service.loadState(service.snapshot().copy(defaultModelDirectory = "C:/moved-models"))
        assertEquals("C:/moved-models", service.snapshot().defaultModelDirectory)
    }

    @Test
    fun `rejects an empty provider without replacing saved state`() {
        val service = WorkbenchSettingsService()
        val before = service.snapshot()

        assertFailsWith<IllegalArgumentException> {
            service.update(before.copy(selectedProviderId = ""))
        }

        assertEquals(before, service.snapshot())
    }
}
