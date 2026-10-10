package com.jaanik.integratedaiworkbench.backend.security

class SecretSanitizer {
    private val labelledSecret = Regex(
        "(?i)\\b(api[_-]?key|access[_-]?token|secret|password)\\s*[:=]\\s*[^\\s]+",
    )

    fun sanitize(text: String): String = labelledSecret.replace(text) { match ->
        "${match.groupValues[1]}=[REDACTED]"
    }
}
