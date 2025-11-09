package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*

/**
 * Handles SOCKS5 handshake: method selection and authentication
 */
internal object Socks5Handshake {

    /**
     * Perform server-side handshake with client
     * Returns the selected authentication method
     *
     * Server handshake flow:
     * 1. Read method selection request: [VER(1) | NMETHODS(1) | METHODS(1-255)]
     * 2. Send method selection response: [VER(1) | METHOD(1)]
     * 3. If USERNAME_PASSWORD, perform authentication sub-negotiation
     */
    suspend fun serverHandshake(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel,
        requireAuth: Boolean = false,
        validateCredentials: ((username: String, password: String) -> Boolean)? = null,
    ): Byte {
        // Read version
        val version = readChannel.readByte()
        if (version != Socks5Constants.VERSION) {
            throw IllegalArgumentException("Unsupported SOCKS version: $version")
        }

        // Read number of methods
        val nMethods = readChannel.readByte().toInt() and 0xFF
        if (nMethods == 0) {
            throw IllegalArgumentException("No authentication methods provided")
        }

        // Read methods
        val methods = ByteArray(nMethods)
        readChannel.readFully(methods, 0, nMethods)

        // Select method
        val selectedMethod = selectMethod(methods.toList(), requireAuth)

        // Send method selection response
        writeChannel.writeByte(Socks5Constants.VERSION)
        writeChannel.writeByte(selectedMethod)
        writeChannel.flush()

        // If no acceptable method, close connection
        if (selectedMethod == Socks5Constants.AuthMethod.NO_ACCEPTABLE) {
            throw IllegalArgumentException("No acceptable authentication method")
        }

        // Perform authentication if required
        if (selectedMethod == Socks5Constants.AuthMethod.USERNAME_PASSWORD) {
            performUsernamePasswordAuth(readChannel, writeChannel, validateCredentials)
        }

        return selectedMethod
    }

    /**
     * Perform client-side handshake with server
     */
    suspend fun clientHandshake(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel,
        username: String? = null,
        password: String? = null,
    ): Byte {
        // Determine which methods to offer
        val methods = mutableListOf<Byte>()
        if (username != null && password != null) {
            methods.add(Socks5Constants.AuthMethod.USERNAME_PASSWORD)
        }
        methods.add(Socks5Constants.AuthMethod.NO_AUTH)

        // Send method selection request
        writeChannel.writeByte(Socks5Constants.VERSION)
        writeChannel.writeByte(methods.size.toByte())
        for (method in methods) {
            writeChannel.writeByte(method)
        }
        writeChannel.flush()

        // Read method selection response
        val version = readChannel.readByte()
        if (version != Socks5Constants.VERSION) {
            throw IllegalArgumentException("Unsupported SOCKS version: $version")
        }

        val selectedMethod = readChannel.readByte()
        if (selectedMethod == Socks5Constants.AuthMethod.NO_ACCEPTABLE) {
            throw IllegalArgumentException("No acceptable authentication method")
        }

        // Perform authentication if required
        if (selectedMethod == Socks5Constants.AuthMethod.USERNAME_PASSWORD) {
            if (username == null || password == null) {
                throw IllegalArgumentException("Server requires authentication but no credentials provided")
            }
            sendUsernamePasswordAuth(readChannel, writeChannel, username, password)
        }

        return selectedMethod
    }

    /**
     * Select authentication method from client's offered methods
     */
    private fun selectMethod(clientMethods: List<Byte>, requireAuth: Boolean): Byte {
        return when {
            requireAuth && clientMethods.contains(Socks5Constants.AuthMethod.USERNAME_PASSWORD) ->
                Socks5Constants.AuthMethod.USERNAME_PASSWORD

            !requireAuth && clientMethods.contains(Socks5Constants.AuthMethod.NO_AUTH) ->
                Socks5Constants.AuthMethod.NO_AUTH

            else -> Socks5Constants.AuthMethod.NO_ACCEPTABLE
        }
    }

    /**
     * Perform server-side username/password authentication (RFC 1929)
     * Format: [VER(1) | ULEN(1) | UNAME(1-255) | PLEN(1) | PASSWD(1-255)]
     * Response: [VER(1) | STATUS(1)]
     */
    private suspend fun performUsernamePasswordAuth(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel,
        validateCredentials: ((username: String, password: String) -> Boolean)?,
    ) {
        // Read authentication request
        val version = readChannel.readByte()
        if (version != Socks5Constants.USERNAME_PASSWORD_VERSION) {
            throw IllegalArgumentException("Unsupported auth version: $version")
        }

        // Read username
        val usernameLength = readChannel.readByte().toInt() and 0xFF
        val usernameBytes = ByteArray(usernameLength)
        readChannel.readFully(usernameBytes, 0, usernameLength)
        val username = usernameBytes.decodeToString()

        // Read password
        val passwordLength = readChannel.readByte().toInt() and 0xFF
        val passwordBytes = ByteArray(passwordLength)
        readChannel.readFully(passwordBytes, 0, passwordLength)
        val password = passwordBytes.decodeToString()

        // Validate credentials
        val isValid = validateCredentials?.invoke(username, password) ?: true

        // Send authentication response
        writeChannel.writeByte(Socks5Constants.USERNAME_PASSWORD_VERSION)
        writeChannel.writeByte(
            if (isValid) Socks5Constants.AuthStatus.SUCCESS
            else Socks5Constants.AuthStatus.FAILURE
        )
        writeChannel.flush()

        if (!isValid) {
            throw IllegalArgumentException("Authentication failed")
        }
    }

    /**
     * Perform client-side username/password authentication (RFC 1929)
     */
    private suspend fun sendUsernamePasswordAuth(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel,
        username: String,
        password: String,
    ) {
        val usernameBytes = username.encodeToByteArray()
        val passwordBytes = password.encodeToByteArray()

        require(usernameBytes.size <= 255) { "Username too long" }
        require(passwordBytes.size <= 255) { "Password too long" }

        // Send authentication request
        writeChannel.writeByte(Socks5Constants.USERNAME_PASSWORD_VERSION)
        writeChannel.writeByte(usernameBytes.size.toByte())
        writeChannel.writeFully(usernameBytes)
        writeChannel.writeByte(passwordBytes.size.toByte())
        writeChannel.writeFully(passwordBytes)
        writeChannel.flush()

        // Read authentication response
        val version = readChannel.readByte()
        if (version != Socks5Constants.USERNAME_PASSWORD_VERSION) {
            throw IllegalArgumentException("Unsupported auth version: $version")
        }

        val status = readChannel.readByte()
        if (status != Socks5Constants.AuthStatus.SUCCESS) {
            throw IllegalArgumentException("Authentication failed")
        }
    }

}
