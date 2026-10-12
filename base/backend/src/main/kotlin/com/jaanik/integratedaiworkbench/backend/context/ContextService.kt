package com.jaanik.integratedaiworkbench.backend.context

import com.jaanik.integratedaiworkbench.backend.security.SecretSanitizer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

interface ContextService {
    fun collect(request: GroundingRequest): GroundingContext
}

class FileSystemContextService(
    approvedRoots: Set<Path>,
    private val sanitizer: SecretSanitizer = SecretSanitizer(),
    private val supportedExtensions: Set<String> = setOf("kt", "java", "md", "txt", "json", "xml", "yml", "yaml", "properties", "gradle", "kts"),
) : ContextService {
    private val roots = approvedRoots.map { it.toRealPath() }.toSet()

    override fun collect(request: GroundingRequest): GroundingContext {
        require(request.maximumFiles > 0 && request.maximumCharacters > 0 && request.maximumCharactersPerFile > 0) { "Grounding limits must be positive." }
        val accepted = mutableListOf<GroundedFile>()
        val omissions = mutableListOf<GroundingOmission>()
        var characters = 0
        request.requestedPaths.sorted().forEach { requested ->
            if (accepted.size >= request.maximumFiles) { omissions += GroundingOmission(requested, "File limit reached."); return@forEach }
            val resolved = resolve(requested, omissions) ?: return@forEach
            if (!isSupported(resolved)) { omissions += GroundingOmission(requested, "Unsupported or binary file type."); return@forEach }
            val content = try { Files.readString(resolved, StandardCharsets.UTF_8) } catch (_: Exception) { omissions += GroundingOmission(requested, "File is unreadable."); return@forEach }
            if (content.indexOf('\u0000') >= 0) { omissions += GroundingOmission(requested, "Binary content is excluded."); return@forEach }
            val bounded = content.take(request.maximumCharactersPerFile)
            val remaining = request.maximumCharacters - characters
            if (remaining <= 0) { omissions += GroundingOmission(requested, "Context limit reached."); return@forEach }
            val safe = sanitizer.sanitize(bounded.take(remaining))
            accepted += GroundedFile(roots.first { resolved.startsWith(it) }.relativize(resolved).toString(), safe)
            characters += safe.length
            if (bounded.length < content.length || safe.length >= remaining) omissions += GroundingOmission(requested, "Content truncated by configured limit.")
        }
        return GroundingContext(accepted, characters, omissions.isNotEmpty(), omissions)
    }

    private fun resolve(requested: String, omissions: MutableList<GroundingOmission>): Path? = try {
        val candidate = Path.of(requested)
        val real = candidate.toRealPath()
        if (!Files.isRegularFile(real) || roots.none { real.startsWith(it) }) null.also { omissions += GroundingOmission(requested, "Path is not an approved regular file.") } else real
    } catch (_: Exception) { null.also { omissions += GroundingOmission(requested, "Path is missing or inaccessible.") } }

    private fun isSupported(path: Path): Boolean = path.fileName.toString().substringAfterLast('.', "").lowercase() in supportedExtensions
}
