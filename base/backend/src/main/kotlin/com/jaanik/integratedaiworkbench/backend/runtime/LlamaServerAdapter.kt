package com.jaanik.integratedaiworkbench.backend.runtime

import com.jaanik.integratedaiworkbench.backend.mcp.ToolCallEnvelopeParser
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValue
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValueCodec
import com.jaanik.integratedaiworkbench.shared.model.AccelerationBackend
import com.jaanik.integratedaiworkbench.shared.model.ExecutionDevice
import com.jaanik.integratedaiworkbench.shared.model.ModelRequest
import com.jaanik.integratedaiworkbench.shared.model.ModelResponse
import com.jaanik.integratedaiworkbench.shared.model.RuntimeCapabilities
import java.net.HttpURLConnection
import java.net.URI
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.Properties
import kotlin.math.min

data class LlamaServerConfiguration(
    val executablePath: Path,
    val modelPath: Path,
    val port: Int = 18080,
    val startupTimeout: Duration = Duration.ofSeconds(30),
    val releaseTag: String,
    val officialArtifactName: String,
    val expectedExecutableSha256: String,
    val cudaEnabled: Boolean = false,
    val gpuLayers: Int? = null,
) {
    init {
        require(executablePath.isAbsolute && executablePath.fileName.toString().equals("llama-server.exe", true)) {
            "llama-server executable must be an absolute llama-server.exe path."
        }
        require(modelPath.isAbsolute && modelPath.fileName.toString().endsWith(".gguf", true)) {
            "llama.cpp requires an absolute GGUF model path."
        }
        require(port in 1024..65535) { "Runtime port must be between 1024 and 65535." }
        require(!startupTimeout.isZero && !startupTimeout.isNegative) { "Startup timeout must be positive." }
        require(releaseTag.isNotBlank() && officialArtifactName.isNotBlank()) { "Official release provenance is required." }
        require(expectedExecutableSha256.matches(Regex("[A-Fa-f0-9]{64}"))) { "Executable SHA-256 must be provided." }
        require(gpuLayers == null || gpuLayers >= 0) { "GPU layer count must not be negative." }
        require(!cudaEnabled || gpuLayers != null) { "CUDA configuration requires an explicit GPU layer count." }
    }
}

/** Loads machine-local runtime provenance without embedding workstation paths in plugin code. */
fun loadLlamaServerConfiguration(file: Path, modelPath: Path, port: Int): LlamaServerConfiguration {
    require(Files.isRegularFile(file) && Files.isReadable(file)) { "Workbench llama.cpp configuration is unavailable." }
    val properties = Properties().also { Files.newInputStream(file).use(it::load) }
    fun required(name: String): String = properties.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Workbench llama.cpp configuration property '$name' is required.")
    return LlamaServerConfiguration(
        executablePath = Path.of(required("executablePath")).toRealPath(), modelPath = modelPath, port = port,
        startupTimeout = Duration.ofSeconds(properties.getProperty("startupTimeoutSeconds", "120").toLong()),
        releaseTag = required("releaseTag"), officialArtifactName = required("officialArtifactName"),
        expectedExecutableSha256 = required("expectedExecutableSha256"),
        cudaEnabled = properties.getProperty("cudaEnabled", "false").toBoolean(),
        gpuLayers = properties.getProperty("gpuLayers")?.toIntOrNull(),
    )
}

/** Workbench-owned localhost llama-server process; no model or native binary is bundled. */
class LlamaServerAdapter(private val configuration: LlamaServerConfiguration) : ManagedLocalRuntimeAdapter {
    override val runtimeId: String = "llama-cpp-server"
    @Volatile private var process: Process? = null

    override fun capabilities(): RuntimeCapabilities = RuntimeCapabilities(
        runtimeId = runtimeId,
        supportsLocalInference = true,
        supportsStructuredOutput = true,
        supportsThinkingLevel = false,
        supportedModelFormats = setOf("gguf"),
        supportedOperatingSystems = setOf("Windows"),
        supportedArchitectures = setOf("amd64", "x86_64"),
        supportedExecutionDevices = if (configuration.cudaEnabled) setOf(ExecutionDevice.CPU, ExecutionDevice.GPU) else setOf(ExecutionDevice.CPU),
        supportedAccelerationBackends = if (configuration.cudaEnabled) setOf(AccelerationBackend.CUDA) else emptySet(),
    )

    @Synchronized override fun start() {
        if (process?.isAlive == true) return
        validateResources()
        process = ProcessBuilder(command()).redirectErrorStream(false).start()
    }

    override fun isReady(): Boolean {
        val running = process?.isAlive == true
        if (!running) return false
        val deadline = System.nanoTime() + configuration.startupTimeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (healthCheck()) return true
            Thread.sleep(100)
        }
        return false
    }

    override fun loadModel() {
        check(process?.isAlive == true && healthCheck()) { "llama-server is not ready to serve the configured model." }
    }

    @Synchronized override fun shutdown(force: Boolean) {
        val owned = process ?: return
        if (!owned.isAlive) return
        owned.destroy()
        if (force || !owned.waitFor(5, TimeUnit.SECONDS)) owned.destroyForcibly()
        process = null
    }

    override suspend fun generate(request: ModelRequest): ModelResponse {
        if (process?.isAlive != true || !healthCheck()) {
            return ModelResponse("", status = com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus.FAILED, metadata = mapOf("runtime" to runtimeId, "error" to "Runtime unavailable."))
        }
        return try {
            val timeoutMillis = min(request.timeout.toMillis(), Int.MAX_VALUE.toLong()).coerceAtLeast(1).toInt()
            val connection = URI("http://127.0.0.1:${configuration.port}/v1/chat/completions").toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(requestBody(request).toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) error("llama-server rejected the generation request.")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val content = chatCompletionContent(body)
            ModelResponse(content, toolCalls = ToolCallEnvelopeParser.parse(content), metadata = mapOf("runtime" to runtimeId))
        } catch (_: SocketTimeoutException) {
            ModelResponse("", status = com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus.TIMED_OUT, metadata = mapOf("runtime" to runtimeId, "error" to "Inference request timed out."))
        } catch (_: Exception) {
            ModelResponse("", status = com.jaanik.integratedaiworkbench.shared.model.ModelResponseStatus.FAILED, metadata = mapOf("runtime" to runtimeId, "error" to "Inference request failed."))
        }
    }

    internal fun command(): List<String> = buildList {
        add(configuration.executablePath.toString())
        addAll(listOf("--host", "127.0.0.1", "--port", configuration.port.toString(), "--model", configuration.modelPath.toString()))
        addAll(listOf("--reasoning", "off"))
        configuration.gpuLayers?.let { addAll(listOf("--gpu-layers", it.toString())) }
    }

    private fun validateResources() {
        require(Files.isRegularFile(configuration.executablePath) && Files.isReadable(configuration.executablePath)) {
            "Configured llama-server.exe is missing or unreadable."
        }
        require(Files.isRegularFile(configuration.modelPath) && Files.isReadable(configuration.modelPath)) {
            "Configured GGUF model is missing or unreadable."
        }
        val actualHash = Files.newInputStream(configuration.executablePath).use { stream ->
            MessageDigest.getInstance("SHA-256").digest(stream.readBytes()).joinToString("") { "%02x".format(it) }
        }
        require(actualHash.equals(configuration.expectedExecutableSha256, true)) {
            "Configured llama-server.exe does not match the configured official release hash."
        }
    }

    private fun healthCheck(): Boolean = runCatching {
        val connection = URI("http://127.0.0.1:${configuration.port}/health").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 250
        connection.readTimeout = 250
        connection.requestMethod = "GET"
        connection.responseCode in 200..299
    }.getOrDefault(false)

    internal fun requestBody(request: ModelRequest): String = buildString {
        append("{\"model\":\"local\",\"messages\":[{\"role\":\"user\",\"content\":\"")
            .append(escapeJson(request.prompt)).append("\"}]")
        request.maxOutputTokens?.let { append(",\"max_tokens\":").append(it) }
        request.temperature?.let { append(",\"temperature\":").append(it) }
        request.topP?.let { append(",\"top_p\":").append(it) }
        request.expectedSchema?.let { schema ->
            val parsed = JsonValueCodec.parse(schema) as? JsonValue.ObjectValue
                ?: error("Structured output schema must be a JSON object.")
            append(",\"response_format\":{\"type\":\"json_schema\",\"json_schema\":{\"name\":\"workbench_response\",\"schema\":")
            append(JsonValueCodec.encode(parsed))
            append(",\"strict\":true}}")
        }
        append("}")
    }

    private fun chatCompletionContent(body: String): String {
        val root = JsonValueCodec.parse(body) as? JsonValue.ObjectValue
            ?: error("llama-server returned an invalid chat response.")
        val choice = (root.values["choices"] as? JsonValue.ArrayValue)?.values?.firstOrNull() as? JsonValue.ObjectValue
            ?: error("llama-server returned no chat choice.")
        val message = choice.values["message"] as? JsonValue.ObjectValue
            ?: error("llama-server returned no chat message.")
        return (message.values["content"] as? JsonValue.StringValue)?.value
            ?: error("llama-server returned no completion content.")
    }

    private fun escapeJson(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
