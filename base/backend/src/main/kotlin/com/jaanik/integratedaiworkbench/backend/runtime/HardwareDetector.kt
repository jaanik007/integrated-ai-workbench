package com.jaanik.integratedaiworkbench.backend.runtime

data class HardwareSnapshot(
    val operatingSystem: String,
    val architecture: String,
    val availableProcessors: Int,
    val maxHeapBytes: Long,
)

interface HardwareDetector {
    fun detect(): HardwareSnapshot
}

class JvmHardwareDetector : HardwareDetector {
    override fun detect(): HardwareSnapshot = HardwareSnapshot(
        operatingSystem = System.getProperty("os.name"),
        architecture = System.getProperty("os.arch"),
        availableProcessors = Runtime.getRuntime().availableProcessors(),
        maxHeapBytes = Runtime.getRuntime().maxMemory(),
    )
}
