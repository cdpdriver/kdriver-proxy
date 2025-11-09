package dev.kdriver.proxy.protocol

/**
 * SOCKS5 protocol constants as defined in RFC 1928 and RFC 1929
 */
internal object Socks5Constants {

    /** SOCKS version 5 */
    const val VERSION: Byte = 0x05

    /** Reserved byte (must be 0x00) */
    const val RESERVED: Byte = 0x00

    /**
     * Authentication methods
     */
    object AuthMethod {
        /** No authentication required */
        const val NO_AUTH: Byte = 0x00

        /** GSSAPI authentication */
        const val GSSAPI: Byte = 0x01

        /** Username/Password authentication (RFC 1929) */
        const val USERNAME_PASSWORD: Byte = 0x02

        /** No acceptable methods */
        const val NO_ACCEPTABLE: Byte = 0xFF.toByte()
    }

    /**
     * SOCKS5 commands
     */
    object Command {
        /** Establish a TCP/IP stream connection */
        const val CONNECT: Byte = 0x01

        /** Establish a TCP/IP port binding */
        const val BIND: Byte = 0x02

        /** Associate a UDP port */
        const val UDP_ASSOCIATE: Byte = 0x03
    }

    /**
     * Address types
     */
    object AddressType {
        /** IPv4 address (4 bytes) */
        const val IPV4: Byte = 0x01

        /** Domain name (length-prefixed string) */
        const val DOMAIN: Byte = 0x03

        /** IPv6 address (16 bytes) */
        const val IPV6: Byte = 0x04
    }

    /**
     * Reply codes
     */
    object Reply {
        /** Succeeded */
        const val SUCCEEDED: Byte = 0x00

        /** General SOCKS server failure */
        const val GENERAL_FAILURE: Byte = 0x01

        /** Connection not allowed by ruleset */
        const val NOT_ALLOWED: Byte = 0x02

        /** Network unreachable */
        const val NETWORK_UNREACHABLE: Byte = 0x03

        /** Host unreachable */
        const val HOST_UNREACHABLE: Byte = 0x04

        /** Connection refused */
        const val CONNECTION_REFUSED: Byte = 0x05

        /** TTL expired */
        const val TTL_EXPIRED: Byte = 0x06

        /** Command not supported */
        const val COMMAND_NOT_SUPPORTED: Byte = 0x07

        /** Address type not supported */
        const val ADDRESS_TYPE_NOT_SUPPORTED: Byte = 0x08
    }

    /**
     * Username/Password authentication version (RFC 1929)
     */
    const val USERNAME_PASSWORD_VERSION: Byte = 0x01

    /**
     * Username/Password authentication status
     */
    object AuthStatus {
        /** Success */
        const val SUCCESS: Byte = 0x00

        /** Failure */
        const val FAILURE: Byte = 0x01
    }

}
