package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*

/**
 * Represents a SOCKS5 reply
 * Format: [VER(1) | REP(1) | RSV(1) | ATYP(1) | BND.ADDR(variable) | BND.PORT(2)]
 */
internal data class Socks5Reply(
    val replyCode: Byte,
    val bindAddress: Socks5Address? = null,
) {

    companion object {

        /**
         * Create a success reply
         */
        fun success(bindHost: String = "0.0.0.0", bindPort: Int = 0): Socks5Reply {
            return Socks5Reply(
                replyCode = Socks5Constants.Reply.SUCCEEDED,
                bindAddress = Socks5Address(
                    type = Socks5Constants.AddressType.IPV4,
                    host = bindHost,
                    port = bindPort
                )
            )
        }

        /**
         * Create an error reply
         */
        fun error(replyCode: Byte): Socks5Reply {
            return Socks5Reply(
                replyCode = replyCode,
                bindAddress = Socks5Address(
                    type = Socks5Constants.AddressType.IPV4,
                    host = "0.0.0.0",
                    port = 0
                )
            )
        }

        /**
         * Create a reply from an exception
         */
        fun fromException(exception: Throwable): Socks5Reply {
            val replyCode = when {
                exception.message?.contains("refused", ignoreCase = true) == true ->
                    Socks5Constants.Reply.CONNECTION_REFUSED

                exception.message?.contains("unreachable", ignoreCase = true) == true ->
                    Socks5Constants.Reply.HOST_UNREACHABLE

                exception.message?.contains("network", ignoreCase = true) == true ->
                    Socks5Constants.Reply.NETWORK_UNREACHABLE

                exception.message?.contains("timeout", ignoreCase = true) == true ->
                    Socks5Constants.Reply.TTL_EXPIRED

                else -> Socks5Constants.Reply.GENERAL_FAILURE
            }
            return error(replyCode)
        }

        /**
         * Read a SOCKS5 reply from a ByteReadChannel
         */
        suspend fun read(channel: ByteReadChannel): Socks5Reply {
            // Read fixed header: [VER(1) | REP(1) | RSV(1) | ATYP(1)]
            val version = channel.readByte()
            if (version != Socks5Constants.VERSION) {
                throw IllegalArgumentException("Unsupported SOCKS version: $version")
            }

            val replyCode = channel.readByte()

            // Read and verify reserved byte
            val reserved = channel.readByte()
            if (reserved != Socks5Constants.RESERVED) {
                // Some implementations may not send 0x00, so we just ignore
            }

            // Read bind address
            val bindAddress = Socks5Address.read(channel)

            return Socks5Reply(replyCode, bindAddress)
        }

    }

    /**
     * Write this reply to a ByteWriteChannel
     */
    suspend fun write(channel: ByteWriteChannel) {
        // Write header
        channel.writeByte(Socks5Constants.VERSION)
        channel.writeByte(replyCode)
        channel.writeByte(Socks5Constants.RESERVED)

        // Write bind address (or default 0.0.0.0:0 if not provided)
        val addr = bindAddress ?: Socks5Address(
            type = Socks5Constants.AddressType.IPV4,
            host = "0.0.0.0",
            port = 0
        )
        addr.write(channel)

        channel.flush()
    }

    /**
     * Check if this reply indicates success
     */
    fun isSuccess(): Boolean = replyCode == Socks5Constants.Reply.SUCCEEDED

    override fun toString(): String {
        val replyName = when (replyCode) {
            Socks5Constants.Reply.SUCCEEDED -> "SUCCESS"
            Socks5Constants.Reply.GENERAL_FAILURE -> "GENERAL_FAILURE"
            Socks5Constants.Reply.NOT_ALLOWED -> "NOT_ALLOWED"
            Socks5Constants.Reply.NETWORK_UNREACHABLE -> "NETWORK_UNREACHABLE"
            Socks5Constants.Reply.HOST_UNREACHABLE -> "HOST_UNREACHABLE"
            Socks5Constants.Reply.CONNECTION_REFUSED -> "CONNECTION_REFUSED"
            Socks5Constants.Reply.TTL_EXPIRED -> "TTL_EXPIRED"
            Socks5Constants.Reply.COMMAND_NOT_SUPPORTED -> "COMMAND_NOT_SUPPORTED"
            Socks5Constants.Reply.ADDRESS_TYPE_NOT_SUPPORTED -> "ADDRESS_TYPE_NOT_SUPPORTED"
            else -> "UNKNOWN($replyCode)"
        }
        return replyName
    }

}
