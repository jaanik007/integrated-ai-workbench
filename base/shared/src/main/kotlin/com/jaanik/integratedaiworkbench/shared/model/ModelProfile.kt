package com.jaanik.integratedaiworkbench.shared.model

import java.time.Duration

/** User-owned configuration for a model; it never contains model binaries or credentials. */
data class ModelProfile(
    val id: String,
    val displayName: String,
    val providerId: String,
    val modelReference: String,
    val temperature: Double = 0.2,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 2_048,
    val timeout: Duration = Duration.ofSeconds(60),
    val executionPolicy: ExecutionPolicy = ExecutionPolicy.LOCAL_ONLY,
)

enum class ExecutionPolicy {
    LOCAL_ONLY,
    REMOTE_ALLOWED,
}
