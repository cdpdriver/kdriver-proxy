package dev.kdriver.proxy

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.*
import javax.net.ssl.SSLSocketFactory

internal class Socks5ProxyServer(
    private val listenPort: Int,
    private val proxy: Proxy,
) {

    private val logger = LoggerFactory.getLogger("Socks5ProxyServer")

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start(scope: CoroutineScope) {
        serverJob = scope.launch {
            serverSocket = ServerSocket(listenPort)
            logger.info("SOCKS5 proxy listening on port $listenPort")

            while (isActive) {
                val clientSocket = serverSocket?.accept() ?: break
                launch {
                    handleSocks5Client(clientSocket)
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverSocket?.close()
    }

    private fun handleSocks5Client(clientSocket: Socket) {
        try {
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // Read and ignore method negotiation
            val version = input.read()
            if (version != 0x05) throw IOException("Unsupported SOCKS version")
            val nMethods = input.read()
            input.readNBytes(nMethods) // ignore methods
            output.write(byteArrayOf(0x05, 0x00)) // no authentication

            // Read request
            val req = input.readNBytes(4)
            val atyp = req[3].toInt()

            val destHost = when (atyp) {
                0x01 -> InetAddress.getByAddress(input.readNBytes(4)).hostAddress // IPv4
                0x03 -> {
                    val len = input.read()
                    String(input.readNBytes(len))
                }

                0x04 -> InetAddress.getByAddress(input.readNBytes(16)).hostAddress // IPv6
                else -> throw IOException("Unsupported address type")
            }
            val portBytes = input.readNBytes(2)
            val destPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

            val targetSocket = connectViaProxy(proxy.url, destHost, destPort, proxy.username, proxy.password)

            // Reply OK
            output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01))
            output.write(InetAddress.getByName("0.0.0.0").address)
            output.write(byteArrayOf(0x00, 0x00))
            output.flush()

            // Relay traffic
            relayData(clientSocket, targetSocket)
        } catch (e: Exception) {
            e.printStackTrace()
            clientSocket.close()
        }
    }

    private fun connectViaProxy(
        proxy: URI,
        destHost: String,
        destPort: Int,
        username: String?,
        password: String?,
    ): Socket {
        val socket = when (proxy.scheme.lowercase()) {
            "http" -> Socket(proxy.host, proxy.port)
            "https" -> {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                factory.createSocket(proxy.host, proxy.port)
            }

            else -> throw IllegalArgumentException("Unsupported proxy scheme: ${proxy.scheme}")
        }

        val writer = PrintWriter(socket.getOutputStream(), true)
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.println("CONNECT $destHost:$destPort HTTP/1.1")
        writer.println("Host: $destHost:$destPort")
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
            writer.println("Proxy-Authorization: Basic $encoded")
        }
        writer.println()
        writer.flush()

        val statusLine = reader.readLine()
        if (!statusLine.contains("200")) {
            throw IOException("Proxy connect failed: $statusLine")
        }

        while (reader.readLine().isNotEmpty()) {
        }
        return socket
    }

    private fun relayData(socket1: Socket, socket2: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            val in1 = socket1.getInputStream()
            val out2 = socket2.getOutputStream()
            try {
                in1.copyTo(out2)
            } catch (_: IOException) {
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val in2 = socket2.getInputStream()
            val out1 = socket1.getOutputStream()
            try {
                in2.copyTo(out1)
            } catch (_: IOException) {
            }
        }
    }

}
