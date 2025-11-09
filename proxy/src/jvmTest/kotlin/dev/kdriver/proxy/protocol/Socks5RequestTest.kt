package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Socks5RequestTest {

    @Test
    fun testReadWriteConnectRequest() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Request.connect("example.com", 443)

        // Write request
        original.write(channel)

        // Read it back
        val read = Socks5Request.read(channel)

        assertEquals(original.command, read.command)
        assertEquals(original.address.host, read.address.host)
        assertEquals(original.address.port, read.address.port)
        assertTrue(read.isConnect())
    }

    @Test
    fun testConnectRequestWithIPv4() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Request.connect("192.168.1.1", 8080)

        original.write(channel)
        val read = Socks5Request.read(channel)

        assertEquals(Socks5Constants.AddressType.IPV4, read.address.type)
        assertEquals("192.168.1.1", read.address.host)
        assertEquals(8080, read.address.port)
    }

    @Test
    fun testConnectRequestWithDomain() = runBlocking {
        val channel = ByteChannel()
        val original = Socks5Request.connect("example.com", 443)

        original.write(channel)
        val read = Socks5Request.read(channel)

        assertEquals(Socks5Constants.AddressType.DOMAIN, read.address.type)
        assertEquals("example.com", read.address.host)
        assertEquals(443, read.address.port)
    }

    @Test
    fun testCommandChecks() {
        val connectReq = Socks5Request(
            command = Socks5Constants.Command.CONNECT,
            address = Socks5Address(Socks5Constants.AddressType.DOMAIN, "example.com", 443)
        )
        assertTrue(connectReq.isConnect())

        val bindReq = Socks5Request(
            command = Socks5Constants.Command.BIND,
            address = Socks5Address(Socks5Constants.AddressType.DOMAIN, "example.com", 443)
        )
        assertTrue(bindReq.isBind())

        val udpReq = Socks5Request(
            command = Socks5Constants.Command.UDP_ASSOCIATE,
            address = Socks5Address(Socks5Constants.AddressType.DOMAIN, "example.com", 443)
        )
        assertTrue(udpReq.isUdpAssociate())
    }

    @Test
    fun testToString() {
        val request = Socks5Request.connect("example.com", 443)
        assertEquals("CONNECT example.com:443", request.toString())
    }

}
