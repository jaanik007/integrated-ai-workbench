package com.jaanik.integratedaiworkbench.frontend

import com.jaanik.integratedaiworkbench.shared.settings.WorkbenchSettingsService
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorkbenchSettingsServiceTest {
    @Test
    fun `persists a validated complete preference snapshot`() {
        val service = WorkbenchSettingsService()
        val directory = createTempDirectory("iwb-models")

        service.update(service.snapshot().copy(
            defaultModelDirectory = directory.toString(),
            lmStudioEndpoint = "http://127.0.0.1:1234",
            readOnlyMcpToolsEnabled = true,
        ))

        val state = service.snapshot()
        assertEquals(directory.toString(), state.defaultModelDirectory)
        assertEquals("http://127.0.0.1:1234", state.lmStudioEndpoint)
        assertTrue(state.readOnlyMcpToolsEnabled)
    }

    @Test
    fun `rejects a missing model directory without replacing saved state`() {
        val service = WorkbenchSettingsService()
        val before = service.snapshot()

        assertFailsWith<IllegalArgumentException> {
            service.update(before.copy(defaultModelDirectory = "C:/missing-iwb-model-directory"))
        }

        assertEquals(before, service.snapshot())
    }

    @Test
    fun `rejects non HTTP LM Studio endpoint`() {
        val service = WorkbenchSettingsService()

        assertFailsWith<IllegalArgumentException> {
            service.update(service.snapshot().copy(lmStudioEndpoint = "file:///not-a-provider"))
        }
    }
}
