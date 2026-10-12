package com.jaanik.integratedaiworkbench.backend.runtime

data class MemorySnapshot(
    val totalPhysicalBytes: Long? = null,
    val availablePhysicalBytes: Long? = null,
    val maxJvmHeapBytes: Long,
)

data class AcceleratorSnapshot(
    val deviceType: String,
    val identifier: String,
    val dedicatedMemoryBytes: Long? = null,
    val availableBackends: Set<String> = emptySet(),
)

data class HardwareSnapshot(
    val operatingSystem: String,
    val operatingSystemVersion: String,
    val architecture: String,
    val availableProcessors: Int,
    val memory: MemorySnapshot,
    val accelerators: List<AcceleratorSnapshot> = emptyList(),
)

interface HardwareDetector {
    fun detect(): HardwareSnapshot
}

class JvmHardwareDetector : HardwareDetector {
    override fun detect(): HardwareSnapshot = HardwareSnapshot(
        operatingSystem = System.getProperty("os.name"),
        operatingSystemVersion = System.getProperty("os.version"),
        architecture = System.getProperty("os.arch"),
        availableProcessors = Runtime.getRuntime().availableProcessors(),
        memory = MemorySnapshot(maxJvmHeapBytes = Runtime.getRuntime().maxMemory()),
    )
}
