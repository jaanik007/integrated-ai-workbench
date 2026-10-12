package com.jaanik.integratedaiworkbench.backend.mcp

import com.jaanik.integratedaiworkbench.backend.security.ToolPermissionGuard
import com.jaanik.integratedaiworkbench.shared.model.ToolCall
import com.jaanik.integratedaiworkbench.shared.run.ToolExecutionProvenance
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

data class IntelliJMcpStdioConfiguration(
    val command: Path,
    val arguments: List<String>,
    val projectPath: Path,
    val port: Int,
    val expectedProjectModule: String,
    val timeout: Duration = Duration.ofSeconds(20),
) {
    init {
        require(command.isAbsolute && Files.isRegularFile(command) && Files.isReadable(command)) { "IntelliJ MCP command must be a readable absolute file." }
        require(arguments.isNotEmpty()) { "IntelliJ MCP command arguments are required." }
        require(projectPath.isAbsolute && Files.isDirectory(projectPath)) { "IntelliJ MCP project path must be an existing absolute directory." }
        require(port in 1024..65535) { "IntelliJ MCP port must be between 1024 and 65535." }
        require(expectedProjectModule.isNotBlank()) { "Expected IntelliJ project module is required." }
        require(!timeout.isZero && !timeout.isNegative) { "IntelliJ MCP timeout must be positive." }
    }

    companion object {
        fun load(file: Path): IntelliJMcpStdioConfiguration {
            require(Files.isRegularFile(file) && Files.isReadable(file)) { "IntelliJ MCP configuration is unavailable." }
            val properties = Properties().also { Files.newInputStream(file).use(it::load) }
            val command = required(properties, "command")
            val projectPath = required(properties, "projectPath")
            val args = properties.stringPropertyNames().filter { it.startsWith("arg.") }.sorted()
                .map { required(properties, it) }
            return IntelliJMcpStdioConfiguration(
                command = Path.of(command).toRealPath(),
                arguments = args,
                projectPath = Path.of(projectPath).toRealPath(),
                port = required(properties, "port").toInt(),
                expectedProjectModule = required(properties, "expectedProjectModule"),
                timeout = Duration.ofMillis(properties.getProperty("timeoutMillis", "20000").toLong()),
            )
        }

        private fun required(properties: Properties, name: String): String =
            properties.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("IntelliJ MCP configuration property '$name' is required.")
    }
}

data class McpToolDefinition(val id: String, val description: String, val inputSchema: JsonValue.ObjectValue)
data class McpToolResult(val text: String, val isError: Boolean)

interface McpClient : AutoCloseable {
    fun discoverTools(): List<McpToolDefinition>
    fun call(toolId: String, arguments: Map<String, String>): McpToolResult
    override fun close()
}

fun interface McpClientFactory {
    fun open(): McpClient
}

/** STDIO client for the IntelliJ-provided MCP server. It owns only the child process it starts. */
class IntelliJMcpStdioClient(private val configuration: IntelliJMcpStdioConfiguration) : McpClient {
    private val process = verifyExistingMcpServer().let {
        ProcessBuilder(listOf(configuration.command.toString()) + configuration.arguments)
            .apply {
                environment()["IJ_MCP_SERVER_PROJECT_PATH"] = configuration.projectPath.toString().replace('\\', '/')
                environment()["IJ_MCP_SERVER_PORT"] = configuration.port.toString()
            }
            .start()
    }
    private val writer = BufferedWriter(OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8))
    private val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
    private val readExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "iwb-intellij-mcp-reader").apply {
            isDaemon = true
        }
    }
    private var nextId = 1
    private var initialized = false

    override fun discoverTools(): List<McpToolDefinition> {
        initialize()
        val result = request("tools/list") as? JsonValue.ObjectValue
            ?: throw McpClientException("IntelliJ MCP tools/list response is invalid.")
        val tools = result.values["tools"] as? JsonValue.ArrayValue
            ?: throw McpClientException("IntelliJ MCP exposed no tool list.")
        return tools.values.map { value ->
            val tool =
                value as? JsonValue.ObjectValue ?: throw McpClientException("IntelliJ MCP tool definition is invalid.")
            val id = tool.string("name") ?: throw McpClientException("IntelliJ MCP tool name is missing.")
            val description = tool.string("description") ?: ""
            val inputSchema = tool.values["inputSchema"] as? JsonValue.ObjectValue
                ?: throw McpClientException("IntelliJ MCP tool '$id' has no object input schema.")
            McpToolDefinition(id, description, inputSchema)
        }.sortedBy(McpToolDefinition::id)
    }

    override fun call(toolId: String, arguments: Map<String, String>): McpToolResult {
        initialize()
        val result = request(
            "tools/call", JsonValue.ObjectValue(
                linkedMapOf(
                    "name" to JsonValue.StringValue(toolId),
                    "arguments" to JsonValue.ObjectValue(arguments.mapValues<String, String, JsonValue> {
                        JsonValue.StringValue(
                            it.value
                        )
                    }),
                )
            )
        ) as? JsonValue.ObjectValue ?: throw McpClientException("IntelliJ MCP tool response is invalid.")
        val content = result.values["content"] as? JsonValue.ArrayValue
            ?: throw McpClientException("IntelliJ MCP tool returned no content.")
        val text = content.values.joinToString("\n") { item ->
            (item as? JsonValue.ObjectValue)?.string("text") ?: "[Non-text MCP tool result omitted]"
        }
        return McpToolResult(text, (result.values["isError"] as? JsonValue.BooleanValue)?.value == true)
    }

    private fun initialize() {
        if (initialized) return
        val initialize = request(
            "initialize",
            JsonValue.ObjectValue(
                linkedMapOf(
                    "protocolVersion" to JsonValue.StringValue("2025-11-25"),
                    "capabilities" to JsonValue.ObjectValue(linkedMapOf()),
                    "clientInfo" to JsonValue.ObjectValue(
                        linkedMapOf(
                            "name" to JsonValue.StringValue("Integrated AI Workbench"),
                            "version" to JsonValue.StringValue("0.1.0-SNAPSHOT"),
                        )
                    ),
                )
            ),
        ) as? JsonValue.ObjectValue ?: throw McpClientException("IntelliJ MCP initialization response is invalid.")
        if (initialize.string("protocolVersion")
                .isNullOrBlank()
        ) throw McpClientException("IntelliJ MCP did not negotiate a protocol version.")
        notify("notifications/initialized")
        initialized = true
    }

    private fun request(
        method: String,
        params: JsonValue.ObjectValue = JsonValue.ObjectValue(linkedMapOf())
    ): JsonValue {
        val id = nextId++
        send(
            JsonValue.ObjectValue(
                linkedMapOf(
                    "jsonrpc" to JsonValue.StringValue("2.0"),
                    "id" to JsonValue.NumberValue(id.toString()),
                    "method" to JsonValue.StringValue(method),
                    "params" to params,
                )
            )
        )
        return await(id)
    }

    private fun notify(method: String) = send(
        JsonValue.ObjectValue(
            linkedMapOf(
                "jsonrpc" to JsonValue.StringValue("2.0"),
                "method" to JsonValue.StringValue(method),
                "params" to JsonValue.ObjectValue(linkedMapOf()),
            )
        )
    )

    private fun send(value: JsonValue.ObjectValue) {
        check(process.isAlive) { "IntelliJ MCP process is no longer running." }
        writer.write(JsonValueCodec.encode(value)); writer.newLine(); writer.flush()
    }

    private fun await(id: Int): JsonValue {
        val future = readExecutor.submit(Callable<JsonValue> {
            while (true) {
                val line =
                    reader.readLine() ?: throw McpClientException("IntelliJ MCP process closed its output stream.")
                val message = JsonValueCodec.parse(line) as? JsonValue.ObjectValue ?: continue
                val responseId = (message.values["id"] as? JsonValue.NumberValue)?.value?.toIntOrNull()
                if (responseId != id) continue
                val error = message.values["error"] as? JsonValue.ObjectValue
                if (error != null) throw McpClientException(error.string("message") ?: "IntelliJ MCP request failed.")
                return@Callable message.values["result"]
                    ?: throw McpClientException("IntelliJ MCP response has no result.")
            }
            error("Unreachable MCP reader state.")
        })
        return try {
            future.get(configuration.timeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (error: Exception) {
            close(); throw McpClientException("IntelliJ MCP request timed out or failed.", error)
        }
    }

    override fun close() {
        readExecutor.shutdownNow()
        writer.close()
        reader.close()
        if (process.isAlive) {
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
        }
    }

    /** STDIO is only a bridge to the already-running configured IDE MCP server. */
    private fun verifyExistingMcpServer() {
        runCatching {
            Socket().use { socket -> socket.connect(InetSocketAddress("127.0.0.1", configuration.port), 500) }
        }.getOrElse {
            throw McpClientException(
                "Configured IntelliJ MCP server is not listening; refusing to launch an IDE bridge.",
                it
            )
        }
    }
}

class McpClientException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

sealed interface ToolExecutionDecision {
    data class Completed(val result: String, val records: List<ToolExecutionProvenance>) : ToolExecutionDecision
    data class Rejected(val reason: String, val records: List<ToolExecutionProvenance> = emptyList()) :
        ToolExecutionDecision
}

/** Validates and dispatches one bounded, agent-scoped MCP tool batch. */
class McpToolExecutionService(
    private val clientFactory: McpClientFactory,
    private val permissionGuard: ToolPermissionGuard = ToolPermissionGuard(),
    private val expectedProjectModule: String,
    private val maximumToolCalls: Int = 3,
    private val maximumToolResultCharacters: Int = 16_000,
) {
    init {
        require(maximumToolCalls in 1..10) { "Maximum tool calls must be bounded." }
        require(maximumToolResultCharacters in 1..64_000) { "Maximum tool result size must be bounded." }
    }

    fun discover(allowedToolIds: Set<String>): List<McpToolDefinition> = clientFactory.open().use { client ->
        client.discoverTools().filter { it.id in allowedToolIds && permissionGuard.classify(it.id).name == "READ_ONLY" }
    }

    fun execute(
        calls: List<ToolCall>,
        allowedToolIds: Set<String>,
        approvedConsequentialCalls: Set<String>
    ): ToolExecutionDecision {
        if (calls.isEmpty() || calls.size > maximumToolCalls) return ToolExecutionDecision.Rejected("Tool-call limit exceeded.")
        clientFactory.open().use { client ->
            val tools = runCatching { client.discoverTools().associateBy(McpToolDefinition::id) }
                .getOrElse { return ToolExecutionDecision.Rejected("IntelliJ MCP tool discovery failed.") }
            verifyTargetProject(client, tools["get_project_modules"])?.let { return ToolExecutionDecision.Rejected(it) }
            val records = mutableListOf<ToolExecutionProvenance>()
            val results = mutableListOf<String>()
            calls.forEach { call ->
                val tool = tools[call.toolId]
                    ?: return ToolExecutionDecision.Rejected("Requested IDE tool is unavailable.", records)
                val authorization = permissionGuard.authorize(
                    call.toolId,
                    allowedToolIds,
                    approvalKey(call),
                    approvedConsequentialCalls
                )
                if (!authorization.allowed) {
                    records += ToolExecutionProvenance(call.toolId, false, false, authorization.reason)
                    return ToolExecutionDecision.Rejected(authorization.reason, records)
                }
                if (!McpInputSchemaValidator.accepts(tool.inputSchema, call.arguments)) {
                    records += ToolExecutionProvenance(
                        call.toolId,
                        true,
                        false,
                        "Tool arguments do not match the supported input schema."
                    )
                    return ToolExecutionDecision.Rejected("Tool arguments are invalid.", records)
                }
                val result = runCatching { client.call(call.toolId, call.arguments) }
                    .getOrElse {
                        records += ToolExecutionProvenance(
                            call.toolId,
                            true,
                            false,
                            "IntelliJ MCP tool execution failed."
                        )
                        return ToolExecutionDecision.Rejected("IntelliJ MCP tool execution failed.", records)
                    }
                if (result.isError) {
                    records += ToolExecutionProvenance(
                        call.toolId,
                        true,
                        false,
                        "IntelliJ MCP tool rejected the request."
                    )
                    return ToolExecutionDecision.Rejected("IntelliJ MCP tool rejected the request.", records)
                }
                val safeResult = result.text.take(maximumToolResultCharacters)
                results += "Tool ${call.toolId} result:\n$safeResult"
                records += ToolExecutionProvenance(call.toolId, true, true, "Completed.")
            }
            return ToolExecutionDecision.Completed(results.joinToString("\n\n"), records)
        }
    }

    private fun approvalKey(call: ToolCall): String =
        call.toolId + "|" + call.arguments.entries.sortedBy(Map.Entry<String, String>::key)
            .joinToString("&") { "${it.key}=${it.value}" }

    private fun verifyTargetProject(client: McpClient, moduleTool: McpToolDefinition?): String? {
        if (moduleTool == null || !McpInputSchemaValidator.accepts(
                moduleTool.inputSchema,
                emptyMap()
            )
        ) return "IntelliJ MCP cannot verify the target project."
        val result = runCatching {
            client.call(
                "get_project_modules",
                emptyMap()
            )
        }.getOrElse { return "IntelliJ MCP target-project verification failed." }
        if (result.isError || !result.text.contains(expectedProjectModule)) return "IntelliJ MCP does not target the configured project."
        return null
    }

    companion object {
        fun fromConfiguration(configuration: IntelliJMcpStdioConfiguration): McpToolExecutionService =
            McpToolExecutionService(
                clientFactory = McpClientFactory { IntelliJMcpStdioClient(configuration) },
                expectedProjectModule = configuration.expectedProjectModule,
            )
    }
}

private object McpInputSchemaValidator {
    fun accepts(schema: JsonValue.ObjectValue, arguments: Map<String, String>): Boolean {
        if (schema.string("type")?.let { it != "object" } == true) return false
        val properties = (schema.values["properties"] as? JsonValue.ObjectValue)?.values ?: emptyMap()
        val required =
            (schema.values["required"] as? JsonValue.ArrayValue)?.values?.mapNotNull { (it as? JsonValue.StringValue)?.value }
                ?: emptyList()
        if (required.any { it !in arguments }) return false
        if ((schema.values["additionalProperties"] as? JsonValue.BooleanValue)?.value == false && arguments.keys.any { it !in properties }) return false
        return arguments.all { (name, value) ->
            val property = properties[name] as? JsonValue.ObjectValue ?: return@all false
            when (property.string("type")) {
                "string" -> value.length <= 8_192
                else -> false
            }
        }
    }
}

/** Only this exact bounded JSON envelope is interpreted as a model-proposed tool call. */
object ToolCallEnvelopeParser {
    fun parse(content: String): List<ToolCall> {
        if (content.length > 24_000) return emptyList()
        val root =
            runCatching { JsonValueCodec.parse(content) as? JsonValue.ObjectValue }.getOrNull() ?: return emptyList()
        val calls = root.values["tool_calls"] as? JsonValue.ArrayValue ?: return emptyList()
        return calls.values.mapNotNull { value ->
            val call = value as? JsonValue.ObjectValue ?: return emptyList()
            val id = call.string("toolId") ?: return emptyList()
            val arguments = call.values["arguments"] as? JsonValue.ObjectValue ?: return emptyList()
            val strings = arguments.values.mapValues { (_, argument) ->
                (argument as? JsonValue.StringValue)?.value ?: return emptyList()
            }
            ToolCall(id, strings)
        }
    }
}

sealed interface JsonValue {
    data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue {
        fun string(name: String): String? = (values[name] as? StringValue)?.value
    }

    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object NullValue : JsonValue
}

internal object JsonValueCodec {
    fun encode(value: JsonValue): String = when (value) {
        is JsonValue.ObjectValue -> value.values.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { "${encode(JsonValue.StringValue(it.key))}:${encode(it.value)}" }

        is JsonValue.ArrayValue -> value.values.joinToString(prefix = "[", postfix = "]", transform = ::encode)
        is JsonValue.StringValue -> "\"" + value.value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            .replace("\r", "\\r").replace("\t", "\\t") + "\""

        is JsonValue.NumberValue -> value.value
        is JsonValue.BooleanValue -> value.value.toString()
        JsonValue.NullValue -> "null"
    }

    fun parse(source: String): JsonValue = Parser(source).parse()

    private class Parser(private val source: String) {
        private var index = 0
        fun parse(): JsonValue =
            value().also { whitespace(); require(index == source.length) { "Trailing JSON content." } }

        private fun value(): JsonValue {
            whitespace(); return when (peek()) {
                '{' -> obj(); '[' -> array(); '\"' -> JsonValue.StringValue(string()); 't' -> literal(
                    "true",
                    JsonValue.BooleanValue(true)
                ); 'f' -> literal("false", JsonValue.BooleanValue(false)); 'n' -> literal(
                    "null",
                    JsonValue.NullValue
                ); '-', in '0'..'9' -> number(); else -> error("Invalid JSON value.")
            }
        }

        private fun obj(): JsonValue.ObjectValue {
            expect('{'); whitespace();
            val values =
                linkedMapOf<String, JsonValue>(); if (consume('}')) return JsonValue.ObjectValue(values); while (true) {
                whitespace();
                val key =
                    string(); whitespace(); expect(':'); require(key !in values) { "Duplicate JSON property." }; values[key] =
                    value(); whitespace(); if (consume('}')) return JsonValue.ObjectValue(values); expect(',')
            }
        }

        private fun array(): JsonValue.ArrayValue {
            expect('['); whitespace();
            val values =
                mutableListOf<JsonValue>(); if (consume(']')) return JsonValue.ArrayValue(values); while (true) {
                values += value(); whitespace(); if (consume(']')) return JsonValue.ArrayValue(values); expect(',')
            }
        }

        private fun string(): String {
            expect('\"');
            val result = StringBuilder(); while (true) {
                require(index < source.length) { "Unterminated JSON string." }; when (val character = source[index++]) {
                    '\"' -> return result.toString(); '\\' -> result.append(escape()); in '\u0000'..'\u001f' -> error("Control character in JSON string."); else -> result.append(
                        character
                    )
                }
            }
        }

        private fun escape(): Char =
            when (val escaped = source.getOrNull(index++) ?: error("Incomplete JSON escape.")) {
                '\"', '\\', '/' -> escaped; 'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'u' -> source.substring(
                index,
                (index + 4).also { require(it <= source.length) { "Incomplete unicode escape." } }).also { index += 4 }
                .toInt(16).toChar(); else -> error("Invalid JSON escape.")
            }

        private fun number(): JsonValue.NumberValue {
            val start = index; consume('-'); digits(); if (consume('.')) digits(); if (consume('e') || consume('E')) {
                consume('+') || consume('-'); digits()
            }; return JsonValue.NumberValue(source.substring(start, index))
        }

        private fun digits() {
            val start = index; while (peek() in '0'..'9') index++; require(index > start) { "Invalid JSON number." }
        }

        private fun literal(expected: String, value: JsonValue): JsonValue {
            require(
                source.startsWith(
                    expected,
                    index
                )
            ) { "Invalid JSON literal." }; index += expected.length; return value
        }

        private fun whitespace() {
            while (peek()?.isWhitespace() == true) index++
        }

        private fun peek(): Char? = source.getOrNull(index)
        private fun expect(character: Char) {
            require(peek() == character) { "Expected '$character'." }; index++
        }

        private fun consume(character: Char): Boolean = if (peek() == character) {
            index++; true
        } else false
    }
}
