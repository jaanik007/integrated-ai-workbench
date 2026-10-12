package com.jaanik.integratedaiworkbench.backend.validation

import com.jaanik.integratedaiworkbench.backend.security.GuardResult
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse

/**
 * Bounded, deterministic JSON schema subset validator used after generation.
 * It intentionally supports only object schemas, required properties, primitive property types and
 * `additionalProperties`; unsupported schemas fail closed instead of being treated as validated.
 */
class StructuredResponseValidator(private val maximumCharacters: Int = 24_000) {
    fun validate(response: ModelResponse, expectedSchema: String? = null): GuardResult {
        if (response.content.isBlank()) return GuardResult(false, "Response content must not be blank.")
        if (response.toolCalls.any { it.toolId.isBlank() }) return GuardResult(false, "Tool calls must have an id.")
        if (expectedSchema == null) return GuardResult(true)
        if (response.content.length > maximumCharacters || expectedSchema.length > maximumCharacters) {
            return GuardResult(false, "Structured response exceeds the configured limit.")
        }
        val schema = parse(expectedSchema) as? JsonValue.ObjectValue
            ?: return GuardResult(false, "Structured output schema must be a JSON object.")
        val value = parse(response.content) as? JsonValue.ObjectValue
            ?: return GuardResult(false, "Structured response must be a JSON object.")
        return validateObject(value, schema)
    }

    private fun validateObject(value: JsonValue.ObjectValue, schema: JsonValue.ObjectValue): GuardResult {
        val type = schema.string("type") ?: "object"
        if (type != "object") return GuardResult(false, "Only object structured-output schemas are supported.")
        val required = (schema.values["required"] as? JsonValue.ArrayValue)?.values?.map {
            (it as? JsonValue.StringValue)?.value ?: return GuardResult(false, "Schema required entries must be strings.")
        } ?: emptyList()
        if (required.any { it !in value.values }) return GuardResult(false, "Structured response is missing a required field.")
        val properties = (schema.values["properties"] as? JsonValue.ObjectValue)?.values ?: emptyMap()
        if (schema.boolean("additionalProperties") == false && value.values.keys.any { it !in properties }) {
            return GuardResult(false, "Structured response contains an undeclared field.")
        }
        properties.forEach { (name, definition) ->
            val propertySchema = definition as? JsonValue.ObjectValue
                ?: return GuardResult(false, "Schema property definitions must be objects.")
            val property = value.values[name] ?: return@forEach
            val expected = propertySchema.string("type")
                ?: return GuardResult(false, "Schema property type is required.")
            if (!property.matches(expected)) return GuardResult(false, "Structured response field '$name' has the wrong type.")
        }
        return GuardResult(true)
    }

    private fun parse(source: String): JsonValue? = runCatching { JsonParser(source).parse() }.getOrNull()

    private sealed interface JsonValue {
        data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue {
            fun string(name: String): String? = (values[name] as? StringValue)?.value
            fun boolean(name: String): Boolean? = (values[name] as? BooleanValue)?.value
        }
        data class ArrayValue(val values: List<JsonValue>) : JsonValue
        data class StringValue(val value: String) : JsonValue
        data class NumberValue(val value: String) : JsonValue
        data class BooleanValue(val value: Boolean) : JsonValue
        data object NullValue : JsonValue
    }

    private fun JsonValue.matches(type: String): Boolean = when (type) {
        "string" -> this is JsonValue.StringValue
        "number" -> this is JsonValue.NumberValue
        "integer" -> this is JsonValue.NumberValue && value.matches(Regex("-?(0|[1-9]\\d*)"))
        "boolean" -> this is JsonValue.BooleanValue
        "object" -> this is JsonValue.ObjectValue
        "array" -> this is JsonValue.ArrayValue
        "null" -> this is JsonValue.NullValue
        else -> false
    }

    private class JsonParser(private val source: String) {
        private var index = 0

        fun parse(): JsonValue {
            val value = value()
            whitespace()
            require(index == source.length) { "Trailing JSON content." }
            return value
        }

        private fun value(): JsonValue {
            whitespace()
            return when (peek()) {
                '{' -> obj()
                '[' -> array()
                '"' -> JsonValue.StringValue(string())
                't' -> literal("true", JsonValue.BooleanValue(true))
                'f' -> literal("false", JsonValue.BooleanValue(false))
                'n' -> literal("null", JsonValue.NullValue)
                '-', in '0'..'9' -> number()
                else -> error("Invalid JSON value.")
            }
        }

        private fun obj(): JsonValue.ObjectValue {
            expect('{'); whitespace()
            val values = linkedMapOf<String, JsonValue>()
            if (consume('}')) return JsonValue.ObjectValue(values)
            while (true) {
                whitespace(); val key = string(); whitespace(); expect(':')
                require(key !in values) { "Duplicate JSON property." }
                values[key] = value(); whitespace()
                if (consume('}')) return JsonValue.ObjectValue(values)
                expect(',')
            }
        }

        private fun array(): JsonValue.ArrayValue {
            expect('['); whitespace()
            val values = mutableListOf<JsonValue>()
            if (consume(']')) return JsonValue.ArrayValue(values)
            while (true) {
                values += value(); whitespace()
                if (consume(']')) return JsonValue.ArrayValue(values)
                expect(',')
            }
        }

        private fun string(): String {
            expect('"')
            val value = StringBuilder()
            while (true) {
                require(index < source.length) { "Unterminated JSON string." }
                when (val character = source[index++]) {
                    '"' -> return value.toString()
                    '\\' -> value.append(escape())
                    in '\u0000'..'\u001f' -> error("Control character in JSON string.")
                    else -> value.append(character)
                }
            }
        }

        private fun escape(): Char = when (val escape = source.getOrNull(index++) ?: error("Incomplete JSON escape.")) {
            '"', '\\', '/' -> escape
            'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
            'u' -> source.substring(index, (index + 4).also { require(it <= source.length) { "Incomplete unicode escape." } })
                .also { index += 4 }.toInt(16).toChar()
            else -> error("Invalid JSON escape.")
        }

        private fun number(): JsonValue.NumberValue {
            val start = index
            consume('-'); digits(); if (consume('.')) digits(); if (consume('e') || consume('E')) { consume('+') || consume('-'); digits() }
            return JsonValue.NumberValue(source.substring(start, index))
        }

        private fun digits() { val start = index; while (peek() in '0'..'9') index++; require(index > start) { "Invalid JSON number." } }
        private fun literal(expected: String, value: JsonValue): JsonValue { require(source.startsWith(expected, index)) { "Invalid JSON literal." }; index += expected.length; return value }
        private fun whitespace() { while (peek()?.isWhitespace() == true) index++ }
        private fun peek(): Char? = source.getOrNull(index)
        private fun expect(character: Char) { require(peek() == character) { "Expected '$character'." }; index++ }
        private fun consume(character: Char): Boolean = if (peek() == character) { index++; true } else false
    }
}
