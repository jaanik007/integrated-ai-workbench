package com.jaanik.integratedaiworkbench.backend.provider

import com.jaanik.integratedaiworkbench.backend.mcp.JsonValue
import com.jaanik.integratedaiworkbench.backend.mcp.JsonValueCodec
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.time.Instant
import java.util.concurrent.TimeUnit

enum class ProviderConnectionStatus {
    AVAILABLE,
    AUTH_REQUIRED,
    OFFLINE,
    NOT_CHECKED,
    ERROR,
}

data class ProviderProbeResult(
    val status: ProviderConnectionStatus,
    val models: List<String>,
    val latencyMs: Long,
    val errorMessage: String?,
    val checkedAt: Instant = Instant.now(),
)

/** Probes a provider's model-catalog endpoint, not its ability to perform inference. */
object ProviderDiscoveryService {
    private const val MAX_BODY_BYTES = 1_048_576
    private const val TIMEOUT_MS = 3_000

    fun probe(endpoint: String, protocol: String, apiKey: String? = null): ProviderProbeResult {
        val started = System.nanoTime()
        fun result(status: ProviderConnectionStatus, models: List<String> = emptyList(), message: String? = null) =
            ProviderProbeResult(status, models, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), message)

        var connection: HttpURLConnection? = null
        return try {
            val target = resolveCatalogUri(endpoint, protocol)
            if (!apiKey.isNullOrBlank() && target.scheme.equals("http", true) && !isLoopback(target.host.orEmpty())) {
                return result(
                    ProviderConnectionStatus.ERROR,
                    message = "Refusing to send an API key over remote HTTP; use HTTPS."
                )
            }
            connection = target.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false // Do not leak authorization headers to redirected hosts.
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            if (!apiKey.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
            val code = connection.responseCode
            when {
                code == 401 -> return result(
                    ProviderConnectionStatus.AUTH_REQUIRED,
                    message = "Authentication required (HTTP 401)."
                )

                code == 403 -> return result(
                    ProviderConnectionStatus.AUTH_REQUIRED,
                    message = "Access denied by provider (HTTP 403)."
                )

                code in 300..399 -> return result(
                    ProviderConnectionStatus.ERROR,
                    message = "Endpoint redirected (HTTP $code); check the base URL."
                )

                code !in 200..299 -> return result(
                    ProviderConnectionStatus.ERROR,
                    message = "Model catalog returned HTTP $code."
                )
            }

            val bytes = connection.inputStream.use(::readLimited)
            val root = (JsonValueCodec.parse(bytes.toString(Charsets.UTF_8)) as? JsonValue.ObjectValue)
                ?: return result(ProviderConnectionStatus.ERROR, message = "Catalog returned invalid JSON structure.")
            val isOllama = protocol.contains("Ollama", ignoreCase = true)
            val field = if (isOllama) "models" else "data"
            val array = root.values[field] as? JsonValue.ArrayValue
                ?: return result(
                    ProviderConnectionStatus.ERROR,
                    message = "Catalog JSON is missing the '$field' array."
                )
            val nameField = if (isOllama) "name" else "id"
            val models = array.values.mapNotNull { item ->
                (item as? JsonValue.ObjectValue)?.string(nameField)?.takeIf(String::isNotBlank)
            }.distinct()
            result(ProviderConnectionStatus.AVAILABLE, models)
        } catch (_: ConnectException) {
            result(ProviderConnectionStatus.OFFLINE, message = "Connection refused; service may be offline.")
        } catch (_: SocketTimeoutException) {
            result(ProviderConnectionStatus.OFFLINE, message = "Connection timed out.")
        } catch (_: UnknownHostException) {
            result(ProviderConnectionStatus.OFFLINE, message = "Provider hostname cannot be resolved.")
        } catch (e: Exception) {
            result(ProviderConnectionStatus.ERROR, message = e.message?.take(240) ?: "Provider probe failed.")
        } finally {
            connection?.disconnect()
        }
    }

    /** Accepts both server roots and /v1 base URLs without appending /v1 twice. */
    private fun resolveCatalogUri(endpoint: String, protocol: String): URI {
        val uri = URI(endpoint.trim().trimEnd('/'))
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) { "Use an HTTP(S) endpoint." }
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Endpoint must have a hostname and no userinfo, query or fragment."
        }
        val path = uri.path.orEmpty().trimEnd('/')
        val suffix = when {
            protocol.contains("Ollama", true) -> when {
                path.endsWith("/api/tags", true) -> ""
                path.endsWith("/api", true) -> "/tags"
                else -> "/api/tags"
            }

            protocol.contains("OpenAI", true) || protocol.contains("/v1", true) -> when {
                path.endsWith("/models", true) -> ""
                path.endsWith("/v1", true) -> "/models"
                else -> "/v1/models"
            }

            else -> throw IllegalArgumentException("Unsupported protocol '$protocol': choose Ollama API or OpenAI-compatible /v1.")
        }
        return URI(uri.scheme, null, uri.host, uri.port, path + suffix, null, null)
    }

    private fun readLimited(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            if (output.size() + read > MAX_BODY_BYTES) throw IllegalStateException("Provider catalog response exceeds 1 MiB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun isLoopback(host: String): Boolean = host.equals("localhost", true) ||
            host == "127.0.0.1" || host == "::1" || host == "[::1]"
}
