package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*

/**
 * Represents a SOCKS5 request
 * Format: [VER(1) | CMD(1) | RSV(1) | ATYP(1) | DST.ADDR(variable) | DST.PORT(2)]
 */
internal data class Socks5Request(
    val command: Byte,
    val address: Socks5Address,
) {

    companion object {

        /**
         * Read a SOCKS5 request from a ByteReadChannel
         */
        suspend fun read(channel: ByteReadChannel): Socks5Request {
            // Read fixed header: [VER(1) | CMD(1) | RSV(1) | ATYP(1)]
            val version = channel.readByte()
            if (version != Socks5Constants.VERSION) {
                throw IllegalArgumentException("Unsupported SOCKS version: $version")
            }

            val command = channel.readByte()

            // Read and verify reserved byte
            val reserved = channel.readByte()
            if (reserved != Socks5Constants.RESERVED) {
                // Some implementations may not send 0x00, so we just log instead of throwing
                // throw IllegalArgumentException("Reserved byte must be 0x00, got: $reserved")
            }

            // Read address (which includes the address type as first byte)
            val address = Socks5Address.read(channel)

            return Socks5Request(command, address)
        }

        /**
         * Create a CONNECT request
         */
        fun connect(host: String, port: Int): Socks5Request {
            // Determine address type
            val addressType = when {
                isIPv4(host) -> Socks5Constants.AddressType.IPV4
                isIPv6(host) -> Socks5Constants.AddressType.IPV6
                else -> Socks5Constants.AddressType.DOMAIN
            }

            return Socks5Request(
                command = Socks5Constants.Command.CONNECT,
                address = Socks5Address(addressType, host, port)
            )
        }

        private fun isIPv4(host: String): Boolean {
            val parts = host.split(".")
            if (parts.size != 4) return false
            return parts.all { part ->
                part.toIntOrNull()?.let { it in 0..255 } ?: false
            }
        }

        private fun isIPv6(host: String): Boolean {
            return host.contains(":")
        }

    }

    /**
     * Write this request to a ByteWriteChannel
     */
    suspend fun write(channel: ByteWriteChannel) {
        // Write header
        channel.writeByte(Socks5Constants.VERSION)
        channel.writeByte(command)
        channel.writeByte(Socks5Constants.RESERVED)

        // Write address (includes address type, host, and port)
        address.write(channel)

        channel.flush()
    }

    /**
     * Check if this is a CONNECT command
     */
    fun isConnect(): Boolean = command == Socks5Constants.Command.CONNECT

    /**
     * Check if this is a BIND command
     */
    fun isBind(): Boolean = command == Socks5Constants.Command.BIND

    /**
     * Check if this is a UDP ASSOCIATE command
     */
    fun isUdpAssociate(): Boolean = command == Socks5Constants.Command.UDP_ASSOCIATE

    override fun toString(): String {
        val commandName = when (command) {
            Socks5Constants.Command.CONNECT -> "CONNECT"
            Socks5Constants.Command.BIND -> "BIND"
            Socks5Constants.Command.UDP_ASSOCIATE -> "UDP_ASSOCIATE"
            else -> "UNKNOWN($command)"
        }
        return "$commandName ${address.host}:${address.port}"
    }

}
