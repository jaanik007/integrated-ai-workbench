package com.jaanik.integratedaiworkbench.backend.context

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSystemContextServiceTest {
    @Test fun `collects approved text only with bounded sanitized provenance`() {
        val root = Files.createTempDirectory("grounding")
        val source = Files.writeString(root.resolve("source.kt"), "val token = api_key=secret-value")
        val binary = Files.write(root.resolve("data.bin"), byteArrayOf(0, 1))
        val service = FileSystemContextService(setOf(root))
        val context = service.collect(GroundingRequest(setOf(source.toString(), binary.toString()), 2, 200, 100))
        assertEquals(1, context.files.size)
        assertTrue(context.files.single().content.contains("[REDACTED]"))
        assertTrue(context.omissions.isNotEmpty())
    }

    @Test fun `rejects unapproved paths and enforces budgets`() {
        val root = Files.createTempDirectory("grounding")
        val approved = Files.writeString(root.resolve("source.md"), "1234567890")
        val outside = Files.writeString(Files.createTempFile("outside", ".md"), "outside")
        val context = FileSystemContextService(setOf(root)).collect(GroundingRequest(setOf(approved.toString(), outside.toString()), 1, 4, 4))
        assertEquals(1, context.files.size)
        assertEquals(4, context.characterCount)
        assertTrue(context.omissions.isNotEmpty())
    }
}
