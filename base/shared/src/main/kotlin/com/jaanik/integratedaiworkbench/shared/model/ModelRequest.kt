package com.jaanik.integratedaiworkbench.shared.model

import java.time.Duration

data class ModelRequest(
    val profileId: String,
    val prompt: String,
    val timeout: Duration,
    val expectedSchema: String? = null,
)
