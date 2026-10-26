package com.learningcalendar.desktop.sync

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.BindException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.file.Files
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject

class DesktopSyncServer(
    private val store: DesktopSyncStore,
    private val preferredPort: Int = 8765,
) : AutoCloseable {
    private val pairCode = AtomicReference(newPairCode())
    private val syncToken = loadOrCreateSyncToken(store.applicationRoot)
    private val executor = Executors.newFixedThreadPool(4)
    private var server: HttpServer? = null

    var onSyncReceived: ((LoadedSync) -> Unit)? = null

    val port: Int
        get() = server?.address?.port ?: preferredPort

    val pairingCode: String
        get() = pairCode.get()

    fun start(): DesktopSyncServer {
        var lastError: Exception? = null
        for (candidate in preferredPort until preferredPort + 10) {
            try {
                server = HttpServer.create(
                    InetSocketAddress("0.0.0.0", candidate),
                    0,
                ).also { created ->
                    created.executor = executor
                    created.createContext("/api/v1/sync", ::handleSync)
                    created.start()
                }
                return this
            } catch (error: BindException) {
                lastError = error
            }
        }
        throw IOException("无法启动电脑同步服务，请检查防火墙或端口占用", lastError)
    }

    fun regeneratePairingCode(): String {
        val code = newPairCode()
        pairCode.set(code)
        return code
    }

    fun localAddresses(): List<String> = runCatching {
        val available = NetworkInterface.getNetworkInterfaces()
            .toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .filterNot { networkInterface ->
                val identity = buildString {
                    append(networkInterface.name)
                    append(' ')
                    append(networkInterface.displayName)
                }.lowercase(Locale.ROOT)
                VIRTUAL_INTERFACE_MARKERS.any(identity::contains)
            }
            .toList()
        val wireless = available.filter { networkInterface ->
            val identity = buildString {
                append(networkInterface.name)
                append(' ')
                append(networkInterface.displayName)
            }.lowercase(Locale.ROOT)
            WIRELESS_INTERFACE_MARKERS.any(identity::contains)
        }
        val preferredInterfaces = wireless.ifEmpty { available }
        preferredInterfaces
            .asSequence()
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { it.isSiteLocalAddress || it.isLinkLocalAddress }
            .mapNotNull { it.hostAddress }
            .distinct()
            .toList()
    }.getOrDefault(emptyList())

    private fun handleSync(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") {
                respond(exchange, 405, "只支持同步请求")
                return
            }
            val suppliedCode = exchange.requestHeaders
                .getFirst("X-Pairing-Code")
                .orEmpty()
            val suppliedToken = exchange.requestHeaders
                .getFirst("X-Sync-Token")
                .orEmpty()
            val authenticated = secureEquals(suppliedCode, pairingCode) ||
                (suppliedToken.isNotBlank() && secureEquals(suppliedToken, syncToken))
            if (!authenticated) {
                respond(exchange, 401, "配对码已失效，请在电脑端查看当前配对码")
                return
            }
            val formatVersion = exchange.requestHeaders
                .getFirst("X-Format-Version")
                ?.toIntOrNull()
            if (formatVersion != 1) {
                respond(exchange, 400, "数据格式版本不受支持")
                return
            }
            val contentLength = exchange.requestHeaders
                .getFirst("Content-Length")
                ?.toLongOrNull()
                ?: -1L
            if (contentLength <= 0 || contentLength > MAX_UPLOAD_BYTES) {
                respond(exchange, 413, "同步数据为空或超过大小限制")
                return
            }

            val incomingDirectory = File(store.applicationRoot, "incoming")
            incomingDirectory.mkdirs()
            val incoming = Files.createTempFile(
                incomingDirectory.toPath(),
                "upload-",
                ".zip",
            ).toFile()
            try {
                var received = 0L
                incoming.outputStream().buffered().use { output ->
                    exchange.requestBody.use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            received += count
                            if (received > MAX_UPLOAD_BYTES) {
                                respond(exchange, 413, "同步数据超过大小限制")
                                return
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val loaded = store.install(incoming)
                onSyncReceived?.invoke(loaded)
                respond(
                    exchange = exchange,
                    status = 200,
                    message = "同步完成",
                    syncToken = syncToken,
                )
            } finally {
                incoming.delete()
            }
        } catch (error: Exception) {
            runCatching {
                respond(exchange, 500, error.message ?: "电脑端保存失败")
            }
        }
    }

    private fun respond(
        exchange: HttpExchange,
        status: Int,
        message: String,
        syncToken: String? = null,
    ) {
        val body = JSONObject().apply {
            put("ok", status in 200..299)
            put("message", message)
            syncToken?.let { put("syncToken", it) }
        }.toString().toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set(
            "Content-Type",
            "application/json; charset=utf-8",
        )
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }

    private fun secureEquals(actual: String, expected: String): Boolean =
        MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expected.toByteArray(Charsets.UTF_8),
        )

    override fun close() {
        server?.stop(1)
        server = null
        executor.shutdownNow()
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 500L * 1024 * 1024
        const val SYNC_TOKEN_FILE = "sync-token.txt"
        val VIRTUAL_INTERFACE_MARKERS = listOf(
            "vmware",
            "virtual",
            "hyper-v",
            "vethernet",
            "wsl",
            "radmin",
            "vpn",
            "loopback",
            "tap",
            "bluetooth",
        )
        val WIRELESS_INTERFACE_MARKERS = listOf("wi-fi", "wifi", "wireless", "wlan")
        val random = SecureRandom()

        fun newPairCode(): String = "%06d".format(random.nextInt(1_000_000))

        fun loadOrCreateSyncToken(applicationRoot: File): String {
            val tokenFile = File(applicationRoot, SYNC_TOKEN_FILE)
            val existing = runCatching {
                tokenFile.readText(Charsets.US_ASCII).trim()
            }.getOrNull()
            if (existing != null && existing.matches(Regex("[0-9a-f]{64}"))) {
                return existing
            }
            val bytes = ByteArray(32).also(random::nextBytes)
            val created = bytes.joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
            applicationRoot.mkdirs()
            tokenFile.writeText(created, Charsets.US_ASCII)
            return created
        }
    }
}
