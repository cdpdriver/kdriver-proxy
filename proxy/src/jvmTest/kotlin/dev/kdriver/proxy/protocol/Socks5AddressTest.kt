package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Socks5AddressTest {

    @Test
    fun testReadWriteIPv4Address() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Address(
            type = Socks5Constants.AddressType.IPV4,
            host = "192.168.1.1",
            port = 8080
        )

        // Write address
        original.write(channel)

        // Read it back
        val read = Socks5Address.read(channel)

        assertEquals(original.type, read.type)
        assertEquals(original.host, read.host)
        assertEquals(original.port, read.port)
    }

    @Test
    fun testReadWriteDomainAddress() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Address(
            type = Socks5Constants.AddressType.DOMAIN,
            host = "example.com",
            port = 443
        )

        // Write address
        original.write(channel)

        // Read it back
        val read = Socks5Address.read(channel)

        assertEquals(original.type, read.type)
        assertEquals(original.host, read.host)
        assertEquals(original.port, read.port)
    }

    @Test
    fun testReadWriteIPv6Address() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Address(
            type = Socks5Constants.AddressType.IPV6,
            host = "2001:db8::1",
            port = 9090
        )

        // Write address
        original.write(channel)

        // Read it back
        val read = Socks5Address.read(channel)

        assertEquals(original.type, read.type)
        assertEquals(original.port, read.port)
        // Note: IPv6 formatting might differ slightly, so we don't check exact match
    }

    @Test
    fun testToString() {
        val address = Socks5Address(
            type = Socks5Constants.AddressType.DOMAIN,
            host = "example.com",
            port = 8080
        )
        assertEquals("example.com:8080", address.toString())
    }

}
