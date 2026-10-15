package com.jaanik.integratedaiworkbench.frontend.tabs

import com.jaanik.integratedaiworkbench.backend.mcp.JsonValue
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValueCodec
import com.jaanik.integratedaiworkbench.backend.provider.PersistedProviderRecord
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Actual non-streaming HTTP inference. Model catalog discovery is NOT proof that inference works. */
internal object ProviderInferenceClient {
    data class Turn(val role: String, val content: String)
    data class Reply(val content: String, val elapsedMs: Long)

    private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024

    class Cancellation {
        private val stopped = AtomicBoolean(false)
        private val current = AtomicReference<HttpURLConnection?>(null)
        fun cancel() {
            stopped.set(true); current.get()?.disconnect()
        }

        internal fun set(connection: HttpURLConnection) {
            current.set(connection)
            if (stopped.get()) connection.disconnect()
        }

        internal fun finish() {
            current.set(null)
        }

        internal fun check() {
            if (stopped.get()) throw InterruptedException("Request cancelled")
        }
    }

    fun complete(
        provider: PersistedProviderRecord,
        model: String,
        messages: List<Turn>,
        apiKey: String?,
        cancellation: Cancellation = Cancellation(),
    ): Reply {
        require(model.isNotBlank()) { "Select a provider model ID." }
        require(messages.isNotEmpty()) { "A message is required." }
        require(messages.all { it.role in setOf("system", "user", "assistant") }) { "Unsupported chat role." }
        val ollama = provider.protocol.contains("Ollama", true)
        val address = completionUri(provider.endpoint, ollama)
        if (apiKey != null && address.scheme == "http" && !isLoopback(address.host.orEmpty())) {
            error("Refusing to send credentials over non-local HTTP. Use HTTPS.")
        }
        if (address.scheme == "http" && !isLoopback(address.host.orEmpty())) {
            error("Refusing to transmit prompts to a remote non-HTTPS endpoint.")
        }
        val payload = "{\"model\":${quote(model)},\"stream\":false,\"messages\":[" +
                messages.joinToString(",") { "{\"role\":${quote(it.role)},\"content\":${quote(it.content)}}" } + "]}"
        val started = System.nanoTime()
        var conn: HttpURLConnection? = null
        try {
            cancellation.check()
            conn = address.toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5_000
            conn.readTimeout = 120_000
            conn.instanceFollowRedirects = false
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            if (!apiKey.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            cancellation.set(conn)
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            cancellation.check()
            if (status !in 200..299) {
                val body =
                    runCatching { conn.errorStream?.use(::limitedRead)?.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
                error("Provider inference failed (HTTP $status)" + if (body.isNotBlank()) ": ${body.take(350)}" else ".")
            }
            val json = conn.inputStream.use(::limitedRead).toString(Charsets.UTF_8)
            val root = JsonValueCodec.parse(json) as? JsonValue.ObjectValue
                ?: error("Inference endpoint returned non-object JSON.")
            val content = if (ollama) {
                (root.values["message"] as? JsonValue.ObjectValue)?.string("content")
            } else {
                val choices = root.values["choices"] as? JsonValue.ArrayValue
                val first = choices?.values?.firstOrNull() as? JsonValue.ObjectValue
                (first?.values?.get("message") as? JsonValue.ObjectValue)?.string("content")
            }?.takeIf(String::isNotBlank) ?: error("Inference response did not contain an assistant message.")
            cancellation.check()
            return Reply(content, (System.nanoTime() - started) / 1_000_000)
        } catch (e: SocketTimeoutException) {
            throw IllegalStateException("Provider inference timed out.", e)
        } finally {
            cancellation.finish()
            conn?.disconnect()
        }
    }

    private fun completionUri(url: String, ollama: Boolean): URI {
        val uri = URI(url.trim().trimEnd('/'))
        require(
            uri.scheme.equals("http", true) || uri.scheme.equals(
                "https",
                true
            )
        ) { "Provider URL must use HTTP(S)." }
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawFragment == null && uri.rawQuery == null) {
            "Provider URL must not contain credentials, query or fragment."
        }
        val base = uri.path.orEmpty().trimEnd('/')
        val suffix = if (ollama) {
            when {
                base.endsWith("/api/chat", true) -> ""
                base.endsWith("/api", true) -> "/chat"
                else -> "/api/chat"
            }
        } else {
            when {
                base.endsWith("/chat/completions", true) -> ""
                base.endsWith("/v1", true) -> "/chat/completions"
                else -> "/v1/chat/completions"
            }
        }
        return URI(uri.scheme, null, uri.host, uri.port, base + suffix, null, null)
    }

    private fun limitedRead(input: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            require(out.size() + n <= MAX_RESPONSE_BYTES) { "Provider response exceeds 4 MiB." }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 32) append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    private fun isLoopback(host: String): Boolean = host.equals("localhost", true) ||
            host == "127.0.0.1" || host == "::1" || host == "[::1]"
}
