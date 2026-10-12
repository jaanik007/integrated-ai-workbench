package com.jaanik.integratedaiworkbench.backend.context

data class GroundingContext(
    val files: List<GroundedFile>,
    val characterCount: Int,
    val truncated: Boolean,
    val omissions: List<GroundingOmission> = emptyList(),
)

data class GroundedFile(
    val relativePath: String,
    val content: String,
)

data class GroundingOmission(val requestedPath: String, val reason: String)

data class GroundingRequest(
    val requestedPaths: Set<String>,
    val maximumFiles: Int,
    val maximumCharacters: Int,
    val maximumCharactersPerFile: Int = maximumCharacters,
)
