package dev.kdriver.proxy.connector

import dev.kdriver.proxy.Proxy
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers

/**
 * Connector that establishes connections through an HTTP/HTTPS CONNECT proxy
 */
internal object HttpConnectProxyConnector {

    /**
     * Connect to a target host through an HTTP CONNECT proxy
     *
     * @param proxy The upstream proxy to connect through
     * @param targetHost The final destination host
     * @param targetPort The final destination port
     * @param selectorManager Optional SelectorManager (will create one if not provided)
     * @return Connected socket ready for data transfer
     */
    suspend fun connect(
        proxy: Proxy,
        targetHost: String,
        targetPort: Int,
        selectorManager: SelectorManager = SelectorManager(Dispatchers.Default),
    ): Socket {
        // Parse proxy URL
        val proxyHost = proxy.url.host
        val proxyPort = if (proxy.url.port != DEFAULT_PORT) {
            proxy.url.port
        } else {
            when (proxy.url.protocol.name.lowercase()) {
                "https" -> 443
                "http" -> 80
                else -> throw IllegalArgumentException("Unsupported proxy scheme: ${proxy.url.protocol.name}")
            }
        }
        val isHttps = proxy.url.protocol.name.lowercase() == "https"

        // Connect to proxy server
        var socket: Socket = aSocket(selectorManager)
            .tcp()
            .connect(proxyHost, proxyPort)

        // Wrap with TLS if HTTPS proxy
        if (isHttps) {
            socket = socket.tls(coroutineContext = Dispatchers.Default) {
                serverName = proxyHost
            }
        }

        try {
            // Get channels for communication
            val readChannel = socket.openReadChannel()
            val writeChannel = socket.openWriteChannel(autoFlush = false)

            // Send HTTP CONNECT request
            sendConnectRequest(writeChannel, targetHost, targetPort, proxy.username, proxy.password)

            // Read and validate HTTP response
            readConnectResponse(readChannel, targetHost, targetPort)

            // Connection established, return socket for data transfer
            return socket
        } catch (e: Exception) {
            socket.close()
            throw e
        }
    }

    /**
     * Send HTTP CONNECT request to proxy
     * Format:
     * CONNECT target:port HTTP/1.1
     * Host: target:port
     * [Proxy-Authorization: Basic base64(username:password)]
     * [blank line]
     */
    private suspend fun sendConnectRequest(
        channel: ByteWriteChannel,
        targetHost: String,
        targetPort: Int,
        username: String?,
        password: String?,
    ) {
        val request = buildString {
            // Request line
            append("CONNECT $targetHost:$targetPort HTTP/1.1\r\n")

            // Host header
            append("Host: $targetHost:$targetPort\r\n")

            // Proxy-Connection header (recommended for HTTP/1.1 proxies)
            append("Proxy-Connection: Keep-Alive\r\n")

            // Authentication if provided
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                val credentials = "$username:$password"
                val encoded = credentials.encodeToByteArray().encodeBase64()
                append("Proxy-Authorization: Basic $encoded\r\n")
            }

            // Blank line to end headers
            append("\r\n")
        }

        channel.writeStringUtf8(request)
        channel.flush()
    }

    /**
     * Read and validate HTTP CONNECT response
     * Expected format:
     * HTTP/1.1 200 Connection Established
     * [headers...]
     * [blank line]
     */
    private suspend fun readConnectResponse(
        channel: ByteReadChannel,
        targetHost: String,
        targetPort: Int,
    ) {
        // Read status line
        val statusLine = channel.readUTF8Line()
            ?: throw IllegalStateException("No response from proxy")

        // Parse status code
        val parts = statusLine.split(" ", limit = 3)
        if (parts.size < 2) {
            throw IllegalStateException("Invalid HTTP response: $statusLine")
        }

        val statusCode = parts[1].toIntOrNull()
            ?: throw IllegalStateException("Invalid status code in response: $statusLine")

        // Check for success (200)
        if (statusCode != 200) {
            throw IllegalStateException("Proxy connect failed: $statusLine")
        }

        // Read and discard headers until blank line
        while (true) {
            val line = channel.readUTF8Line()
            if (line.isNullOrBlank()) {
                break
            }
        }

        // Connection established successfully
    }

    /**
     * Base64 encoding for Basic authentication
     * Note: This is a simple implementation. For production use, consider using a proper Base64 library
     */
    private fun ByteArray.encodeBase64(): String {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val output = StringBuilder()

        var i = 0
        while (i < size) {
            val b1 = this[i++].toInt() and 0xFF
            val b2 = if (i < size) this[i++].toInt() and 0xFF else 0
            val b3 = if (i < size) this[i++].toInt() and 0xFF else 0

            val triple = (b1 shl 16) or (b2 shl 8) or b3

            output.append(base64Chars[(triple shr 18) and 0x3F])
            output.append(base64Chars[(triple shr 12) and 0x3F])
            output.append(if (i > size + 1) '=' else base64Chars[(triple shr 6) and 0x3F])
            output.append(if (i > size) '=' else base64Chars[triple and 0x3F])
        }

        return output.toString()
    }

}
