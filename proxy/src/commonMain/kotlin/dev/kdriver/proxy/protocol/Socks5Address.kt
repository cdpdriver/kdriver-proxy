package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*

/**
 * Represents a SOCKS5 address (IPv4, IPv6, or Domain name) with port
 */
internal data class Socks5Address(
    val type: Byte,
    val host: String,
    val port: Int,
) {

    companion object {

        /**
         * Read a SOCKS5 address from a ByteReadChannel
         * Format: [ATYP(1) | DST.ADDR(variable) | DST.PORT(2)]
         */
        suspend fun read(channel: ByteReadChannel): Socks5Address {
            val addressType = channel.readByte()

            val host = when (addressType) {
                Socks5Constants.AddressType.IPV4 -> {
                    // Read 4 bytes for IPv4
                    val bytes = ByteArray(4)
                    channel.readFully(bytes, 0, 4)
                    // Convert to dotted decimal notation
                    bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                }

                Socks5Constants.AddressType.DOMAIN -> {
                    // Read length-prefixed domain name
                    val length = channel.readByte().toInt() and 0xFF
                    val bytes = ByteArray(length)
                    channel.readFully(bytes, 0, length)
                    bytes.decodeToString()
                }

                Socks5Constants.AddressType.IPV6 -> {
                    // Read 16 bytes for IPv6
                    val bytes = ByteArray(16)
                    channel.readFully(bytes, 0, 16)
                    // Convert to IPv6 notation (simplified, may need improvement)
                    formatIPv6(bytes)
                }

                else -> throw IllegalArgumentException("Unsupported address type: $addressType")
            }

            // Read 2-byte port (big-endian)
            val portHigh = channel.readByte().toInt() and 0xFF
            val portLow = channel.readByte().toInt() and 0xFF
            val port = (portHigh shl 8) or portLow

            return Socks5Address(addressType, host, port)
        }

        /**
         * Format IPv6 address bytes to standard notation
         */
        private fun formatIPv6(bytes: ByteArray): String {
            require(bytes.size == 16) { "IPv6 address must be 16 bytes" }

            val groups = mutableListOf<String>()
            for (i in 0 until 16 step 2) {
                val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                groups.add(value.toString(16))
            }

            // Join groups with colons
            return groups.joinToString(":")
        }

    }

    /**
     * Write this address to a ByteWriteChannel
     * Format: [ATYP(1) | DST.ADDR(variable) | DST.PORT(2)]
     */
    suspend fun write(channel: ByteWriteChannel) {
        channel.writeByte(type)

        when (type) {
            Socks5Constants.AddressType.IPV4 -> {
                // Write 4 bytes for IPv4
                val parts = host.split(".")
                require(parts.size == 4) { "Invalid IPv4 address: $host" }
                for (part in parts) {
                    channel.writeByte(part.toInt().toByte())
                }
            }

            Socks5Constants.AddressType.DOMAIN -> {
                // Write length-prefixed domain name
                val bytes = host.encodeToByteArray()
                require(bytes.size <= 255) { "Domain name too long: ${bytes.size} bytes" }
                channel.writeByte(bytes.size.toByte())
                channel.writeFully(bytes)
            }

            Socks5Constants.AddressType.IPV6 -> {
                // Write 16 bytes for IPv6
                val bytes = parseIPv6(host)
                channel.writeFully(bytes)
            }
        }

        // Write 2-byte port (big-endian)
        channel.writeByte((port shr 8).toByte())
        channel.writeByte(port.toByte())
    }

    /**
     * Parse IPv6 address string to bytes
     */
    private fun parseIPv6(address: String): ByteArray {
        val result = ByteArray(16)
        val groups = address.split(":")

        var index = 0
        for (group in groups) {
            if (group.isEmpty()) {
                // Handle :: notation (compressed zeros)
                continue
            }
            val value = group.toInt(16)
            result[index++] = (value shr 8).toByte()
            result[index++] = value.toByte()
        }

        return result
    }

    override fun toString(): String = "$host:$port"

}
