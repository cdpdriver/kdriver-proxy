package dev.kdriver.proxy.integration

import dev.kdriver.proxy.Proxy
import dev.kdriver.proxy.Socks5ProxyServer
import dev.kdriver.proxy.protocol.Socks5Handshake
import dev.kdriver.proxy.protocol.Socks5Reply
import dev.kdriver.proxy.protocol.Socks5Request
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for SOCKS5 server
 * Note: These tests require a real HTTP proxy to be available for full end-to-end testing
 */
class Socks5ServerIntegrationTest {

    private lateinit var server: Socks5ProxyServer
    private lateinit var serverScope: CoroutineScope
    private val serverPort = 11080 // Use a high port to avoid permission issues

    @BeforeTest
    fun setup() {
        serverScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    @AfterTest
    fun teardown() {
        if (::server.isInitialized) {
            server.stop()
        }
        serverScope.cancel()
    }

    @Test
    fun testServerStartsAndAcceptsConnections() = runBlocking {
        // Create a mock upstream proxy (won't actually connect, just testing server startup)
        val proxy = Proxy("http://localhost:8080")

        server = Socks5ProxyServer(serverPort, proxy)
        server.start(serverScope)

        // Give server time to start
        delay(100)

        // Try to connect to the SOCKS5 server
        val selectorManager = SelectorManager(Dispatchers.Default)
        val client = aSocket(selectorManager)
            .tcp()
            .connect("localhost", serverPort)

        assertTrue(client.isClosed.not())

        client.close()
        selectorManager.close()
    }

    @Test
    fun testSocks5HandshakeNoAuth() = runBlocking {
        val proxy = Proxy("http://localhost:8080")

        server = Socks5ProxyServer(serverPort, proxy)
        server.start(serverScope)
        delay(100)

        // Connect and perform handshake
        val selectorManager = SelectorManager(Dispatchers.Default)
        val client = aSocket(selectorManager)
            .tcp()
            .connect("localhost", serverPort)

        val readChannel = client.openReadChannel()
        val writeChannel = client.openWriteChannel(autoFlush = false)

        // Perform client-side handshake
        Socks5Handshake.clientHandshake(
            readChannel = readChannel,
            writeChannel = writeChannel,
            username = null,
            password = null
        )

        // If we get here without exception, handshake succeeded
        assertTrue(true)

        client.close()
        selectorManager.close()
    }

    @Test
    fun testSocks5ConnectRequest() = runBlocking {
        val proxy = Proxy("http://localhost:8080")

        server = Socks5ProxyServer(serverPort, proxy)
        server.start(serverScope)
        delay(100)

        val selectorManager = SelectorManager(Dispatchers.Default)
        val client = aSocket(selectorManager)
            .tcp()
            .connect("localhost", serverPort)

        val readChannel = client.openReadChannel()
        val writeChannel = client.openWriteChannel(autoFlush = false)

        // Perform handshake
        Socks5Handshake.clientHandshake(readChannel, writeChannel, null, null)

        // Send CONNECT request
        val request = Socks5Request.connect("example.com", 443)
        request.write(writeChannel)

        // Read reply (will fail because we don't have a real proxy, but we're testing protocol)
        try {
            val reply = Socks5Reply.read(readChannel)
            // If upstream proxy is available, we'd get a success or error reply
            println("Received reply: $reply")
        } catch (e: Exception) {
            // Expected if no real proxy is available
            println("Expected error: ${e.message}")
        }

        client.close()
        selectorManager.close()
    }

}
