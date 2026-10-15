package com.jaanik.integratedaiworkbench.backend.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class GgufMetadataReaderTest {
    @Test
    fun `reads model basics from a GGUF header without reading tensors`() {
        val file = Files.createTempFile("workbench-metadata", ".gguf")
        val entries = listOf(
            "general.architecture" to "llama",
            "general.name" to "Test Llama",
            "llama.context_length" to 131_072L,
            "general.parameter_count" to 1_234_567_890L,
            "general.size_label" to "1B",
            "general.file_type" to 7L,
            "tokenizer.ggml.model" to "llama",
            "tokenizer.chat_template" to "{% if tools %}<tool_call>{{ tools }}</tool_call>{% endif %}<think>{{ content }}</think>",
        )
        val buffer = ByteBuffer.allocate(8 * 1024).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("GGUF".encodeToByteArray())
            putInt(3)
            putLong(0)
            putLong(entries.size.toLong())
            entries.forEach { (key, value) ->
                putString(key)
                if (value is String) {
                    putInt(8)
                    putString(value)
                } else {
                    putInt(10)
                    putLong(value as Long)
                }
            }
        }
        Files.write(file, buffer.array().copyOf(buffer.position()))

        val metadata = GgufMetadataReader.read(file)

        assertEquals(3, metadata.formatVersion)
        assertEquals("llama", metadata.architecture)
        assertEquals("Test Llama", metadata.name)
        assertEquals(131_072L, metadata.contextLength)
        assertEquals(1_234_567_890L, metadata.parameterCount)
        assertEquals("1B · GGUF file type 7", metadata.quantization)
        assertEquals("llama", metadata.tokenizerModel)
        assertEquals(setOf("Chat template", "Tool-call template", "Reasoning-tag template"), metadata.declaredFeatures)
    }

    private fun ByteBuffer.putString(value: String) {
        val bytes = value.encodeToByteArray()
        putLong(bytes.size.toLong())
        put(bytes)
    }
}
