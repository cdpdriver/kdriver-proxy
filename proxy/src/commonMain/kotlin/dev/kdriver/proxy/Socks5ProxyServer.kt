package dev.kdriver.proxy

import dev.kdriver.proxy.connector.HttpConnectProxyConnector
import dev.kdriver.proxy.protocol.Socks5Constants
import dev.kdriver.proxy.protocol.Socks5Handshake
import dev.kdriver.proxy.protocol.Socks5Reply
import dev.kdriver.proxy.protocol.Socks5Request
import dev.kdriver.proxy.relay.BidirectionalRelay
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*

internal class Socks5ProxyServer(
    private val listenPort: Int,
    private val proxy: Proxy,
) {

    private val logger = KtorSimpleLogger("Socks5ProxyServer")

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var selectorManager: SelectorManager? = null

    fun start(scope: CoroutineScope) {
        serverJob = scope.launch {
            try {
                // Create selector manager for network I/O
                selectorManager = SelectorManager(Dispatchers.Default)

                // Create and bind server socket
                serverSocket = aSocket(selectorManager!!)
                    .tcp()
                    .bind("0.0.0.0", listenPort)

                logger.info("SOCKS5 proxy listening on port $listenPort")

                // Accept client connections
                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch {
                            handleSocks5Client(clientSocket)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (isActive) {
                            logger.error("Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Normal shutdown
            } catch (e: Exception) {
                logger.error("Server error", e)
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverSocket?.close()
        selectorManager?.close()
    }

    private suspend fun handleSocks5Client(clientSocket: Socket) {
        try {
            val remoteAddr = clientSocket.remoteAddress.toString()
            logger.info("New connection from $remoteAddr")

            val readChannel = clientSocket.openReadChannel()
            val writeChannel = clientSocket.openWriteChannel(autoFlush = false)

            // Perform SOCKS5 handshake (method selection and authentication)
            Socks5Handshake.serverHandshake(
                readChannel = readChannel,
                writeChannel = writeChannel,
                requireAuth = false, // TODO: Make configurable
                validateCredentials = null // TODO: Implement credential validation
            )

            // Read SOCKS5 request
            val request = Socks5Request.read(readChannel)
            logger.info("Request from $remoteAddr: $request")

            // Handle different commands
            when {
                request.isConnect() -> handleConnect(clientSocket, request, readChannel, writeChannel, remoteAddr)
                request.isBind() -> handleBind(clientSocket, writeChannel, remoteAddr)
                request.isUdpAssociate() -> handleUdpAssociate(clientSocket, writeChannel, remoteAddr)
                else -> {
                    logger.error("Unsupported command: ${request.command}")
                    Socks5Reply.error(Socks5Constants.Reply.COMMAND_NOT_SUPPORTED).write(writeChannel)
                    clientSocket.close()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error handling client", e)
            try {
                clientSocket.close()
            } catch (closeError: Exception) {
                // Ignore close errors
            }
        }
    }

    private suspend fun handleConnect(
        clientSocket: Socket,
        request: Socks5Request,
        readChannel: io.ktor.utils.io.ByteReadChannel,
        writeChannel: io.ktor.utils.io.ByteWriteChannel,
        remoteAddr: String,
    ) {
        var targetSocket: Socket? = null
        try {
            // Connect to target through upstream proxy
            targetSocket = HttpConnectProxyConnector.connect(
                proxy = proxy,
                targetHost = request.address.host,
                targetPort = request.address.port,
                selectorManager = selectorManager!!
            )

            logger.info("Connected to ${request.address} via proxy for $remoteAddr")

            // Send success reply
            Socks5Reply.success().write(writeChannel)

            // Start bidirectional relay
            logger.info("Starting relay: $remoteAddr <-> ${request.address}")
            BidirectionalRelay.relay(
                scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
                socket1 = clientSocket,
                socket2 = targetSocket,
                onBytesTransferred = { fromClient, fromTarget ->
                    logger.info("Relay completed: $remoteAddr <-> ${request.address} (sent: $fromClient bytes, received: $fromTarget bytes)")
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error connecting to ${request.address}", e)
            try {
                Socks5Reply.fromException(e).write(writeChannel)
            } catch (replyError: Exception) {
                // Ignore if we can't send reply
            }
            clientSocket.close()
            targetSocket?.close()
        }
    }

    private suspend fun handleBind(
        clientSocket: Socket,
        writeChannel: io.ktor.utils.io.ByteWriteChannel,
        remoteAddr: String,
    ) {
        logger.warn("BIND command not supported from $remoteAddr")
        Socks5Reply.error(Socks5Constants.Reply.COMMAND_NOT_SUPPORTED).write(writeChannel)
        clientSocket.close()
    }

    private suspend fun handleUdpAssociate(
        clientSocket: Socket,
        writeChannel: io.ktor.utils.io.ByteWriteChannel,
        remoteAddr: String,
    ) {
        logger.warn("UDP ASSOCIATE command not supported from $remoteAddr")
        Socks5Reply.error(Socks5Constants.Reply.COMMAND_NOT_SUPPORTED).write(writeChannel)
        clientSocket.close()
    }

}
