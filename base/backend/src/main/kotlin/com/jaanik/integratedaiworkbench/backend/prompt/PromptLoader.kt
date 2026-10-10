package com.jaanik.integratedaiworkbench.backend.prompt

import java.nio.file.Path

interface PromptLoader {
    fun load(markdownPath: Path): String?
}
