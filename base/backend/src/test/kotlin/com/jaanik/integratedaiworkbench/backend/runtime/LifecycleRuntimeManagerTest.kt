package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LifecycleRuntimeManagerTest {
    private fun adapter() = FakeLocalRuntimeAdapter(runtimeCapabilities = RuntimeCapabilities("fake", true, false, false))

    @Test fun `moves through startup load and idempotent shutdown`() {
        val manager = LifecycleRuntimeManager(adapter())
        manager.start(); manager.start()
        manager.loadModel()
        manager.shutdown(); manager.shutdown()
        assertEquals(RuntimeLifecycleState.STOPPED, manager.status().state)
    }

    @Test fun `startup and load failures leave safe failed state`() {
        val startup = LifecycleRuntimeManager(adapter().also { it.failStart = true })
        assertFailsWith<IllegalStateException> { startup.start() }
        assertEquals(RuntimeLifecycleState.FAILED, startup.status().state)
        val loading = LifecycleRuntimeManager(adapter().also { it.failLoad = true })
        loading.start()
        assertFailsWith<IllegalStateException> { loading.loadModel() }
        assertEquals(RuntimeLifecycleState.FAILED, loading.status().state)
    }
}
