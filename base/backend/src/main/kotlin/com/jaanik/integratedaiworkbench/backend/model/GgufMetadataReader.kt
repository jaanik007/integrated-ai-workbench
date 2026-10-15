package com.jaanik.integratedaiworkbench.backend.model

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Read-only, bounded inspection of the GGUF header. It never loads tensors or starts inference. */
data class GgufModelMetadata(
    val formatVersion: Int,
    val architecture: String?,
    val name: String?,
    val contextLength: Long?,
    val parameterCount: Long?,
    val quantization: String?,
    val tokenizerModel: String?,
    /** Features explicitly declared by architecture or chat-template metadata. */
    val declaredFeatures: Set<String>,
)

object GgufMetadataReader {
    private const val MAX_METADATA_ENTRIES = 100_000L
    private const val MAX_ARRAY_ENTRIES = 1_000_000L
    private const val MAX_STRING_BYTES = 16L * 1024 * 1024

    fun read(path: Path): GgufModelMetadata {
        Files.newInputStream(path).use { raw ->
            val input = LittleEndianInput(BufferedInputStream(raw))
            require(input.readAscii(4) == "GGUF") { "The selected file is not a GGUF model." }
            val version = input.readUInt32().toInt()
            require(version in 2..3) { "Unsupported GGUF version $version." }
            input.readUInt64() // Tensor count; tensor data is intentionally not inspected.
            val metadataCount = input.readUInt64()
            require(metadataCount in 0..MAX_METADATA_ENTRIES) { "GGUF metadata entry count is unreasonable." }

            val values = linkedMapOf<String, Any?>()
            repeat(metadataCount.toInt()) {
                val key = input.readString()
                val type = input.readUInt32().toInt()
                val interesting = key in interestingKeys || key.endsWith(".context_length") ||
                    key.startsWith("tokenizer.chat_template") || key.startsWith("clip.") ||
                    key.startsWith("llava.") || key.contains(".vision.")
                if (interesting) {
                    val value = input.readValue(type)
                    values[key] = value
                } else {
                    input.skipValue(type)
                }
            }
            val architecture = values["general.architecture"] as? String
            val contextLength = (values["${architecture.orEmpty()}.context_length"] ?: values["llama.context_length"])
                .asLongOrNull()
            val fileType = values["general.file_type"]?.asLongOrNull()
            val templates = values.filterKeys { it.startsWith("tokenizer.chat_template") }.values.filterIsInstance<String>()
            val templateText = templates.joinToString("\n")
            val declaredFeatures = linkedSetOf<String>()
            if (templates.isNotEmpty()) declaredFeatures += "Chat template"
            if (templateText.contains("tool_call", ignoreCase = true) || templateText.contains("<tool", ignoreCase = true) ||
                templateText.contains("<function", ignoreCase = true)) {
                declaredFeatures += "Tool-call template"
            }
            if (templateText.contains("<think", ignoreCase = true) && templateText.contains("</think", ignoreCase = true)) {
                declaredFeatures += "Reasoning-tag template"
            }
            if (architecture.isVisionArchitecture() || values.keys.any { it.startsWith("clip.") || it.startsWith("llava.") || it.contains(".vision.") }) {
                declaredFeatures += "Vision architecture"
            }
            return GgufModelMetadata(
                formatVersion = version,
                architecture = architecture,
                name = values["general.name"] as? String,
                contextLength = contextLength,
                parameterCount = values["general.parameter_count"].asLongOrNull(),
                quantization = (values["general.quantization"] as? String)
                    ?: (values["general.size_label"] as? String)?.let { label ->
                        fileType?.let { "$label · GGUF file type $it" } ?: label
                    }
                    ?: fileType?.let { "GGUF file type $it" },
                tokenizerModel = values["tokenizer.ggml.model"] as? String,
                declaredFeatures = declaredFeatures,
            )
        }
    }

    private val interestingKeys = setOf(
        "general.architecture",
        "general.name",
        "general.parameter_count",
        "general.quantization",
        "general.size_label",
        "general.file_type",
        "llama.context_length",
        "tokenizer.ggml.model",
    )

    private fun Any?.asLongOrNull(): Long? = when (this) {
        is Long -> takeIf { it >= 0 }
        is Int -> toLong()
        else -> null
    }

    private fun String?.isVisionArchitecture(): Boolean = this?.lowercase() in setOf(
        "llava", "mllama", "qwen2vl", "qwen2.5vl", "internvl", "minicpmv", "pixtral", "gemma3",
    )

    private class LittleEndianInput(private val input: InputStream) {
        fun readAscii(length: Int): String = readBytes(length.toLong()).toString(StandardCharsets.US_ASCII)
        fun readUInt32(): Long = readNumber(4)
        fun readUInt64(): Long = readNumber(8)

        fun readString(): String {
            val length = readUInt64()
            require(length <= MAX_STRING_BYTES) { "GGUF metadata string is too large." }
            return readBytes(length).toString(StandardCharsets.UTF_8)
        }

        fun readValue(type: Int): Any? = when (type) {
            0 -> readNumber(1)
            1 -> readNumber(1).toByte().toInt()
            2 -> readNumber(2)
            3 -> readNumber(2).toShort().toInt()
            4 -> readUInt32()
            5 -> readUInt32().toInt()
            6 -> Float.fromBits(readUInt32().toInt())
            7 -> readNumber(1) != 0L
            8 -> readString()
            9 -> {
                val elementType = readUInt32().toInt()
                val count = readUInt64()
                require(count in 0..MAX_ARRAY_ENTRIES) { "GGUF metadata array is too large." }
                repeat(count.toInt()) { readValue(elementType) }
                null
            }
            10 -> readUInt64()
            11 -> readUInt64()
            12 -> Double.fromBits(readUInt64())
            else -> throw IllegalArgumentException("Unsupported GGUF metadata value type $type.")
        }

        fun skipValue(type: Int) {
            when (type) {
                0, 1, 7 -> skipBytes(1)
                2, 3 -> skipBytes(2)
                4, 5, 6 -> skipBytes(4)
                8 -> skipBytes(readUInt64())
                9 -> {
                    val elementType = readUInt32().toInt()
                    val count = readUInt64()
                    require(count in 0..MAX_ARRAY_ENTRIES) { "GGUF metadata array is too large." }
                    repeat(count.toInt()) { skipValue(elementType) }
                }
                10, 11, 12 -> skipBytes(8)
                else -> throw IllegalArgumentException("Unsupported GGUF metadata value type $type.")
            }
        }

        private fun readNumber(byteCount: Int): Long {
            var value = 0L
            repeat(byteCount) { index -> value = value or (readByte().toLong() shl (index * 8)) }
            return value
        }

        private fun readByte(): Int = input.read().takeIf { it >= 0 } ?: throw EOFException("Unexpected end of GGUF header.")

        private fun readBytes(length: Long): ByteArray {
            require(length <= Int.MAX_VALUE) { "GGUF metadata field is too large." }
            return input.readNBytes(length.toInt()).also {
                if (it.size != length.toInt()) throw EOFException("Unexpected end of GGUF header.")
            }
        }

        private fun skipBytes(length: Long) {
            require(length in 0..MAX_STRING_BYTES) { "GGUF metadata field is too large." }
            var remaining = length
            while (remaining > 0) {
                val skipped = input.skip(remaining)
                if (skipped > 0) remaining -= skipped
                else readByte().also { remaining-- }
            }
        }
    }
}
