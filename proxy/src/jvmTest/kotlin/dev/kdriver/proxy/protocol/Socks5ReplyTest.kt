package dev.kdriver.proxy.protocol

import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Socks5ReplyTest {

    @Test
    fun testSuccessReply() = runBlocking {
        val channel = ByteChannel()
        val reply = Socks5Reply.success()

        reply.write(channel)
        val read = Socks5Reply.read(channel)

        assertEquals(Socks5Constants.Reply.SUCCEEDED, read.replyCode)
        assertTrue(read.isSuccess())
    }

    @Test
    fun testErrorReply() = runBlocking {
        val channel = ByteChannel()
        val reply = Socks5Reply.error(Socks5Constants.Reply.CONNECTION_REFUSED)

        reply.write(channel)
        val read = Socks5Reply.read(channel)

        assertEquals(Socks5Constants.Reply.CONNECTION_REFUSED, read.replyCode)
    }

    @Test
    fun testFromException() {
        val refusedReply = Socks5Reply.fromException(Exception("Connection refused"))
        assertEquals(Socks5Constants.Reply.CONNECTION_REFUSED, refusedReply.replyCode)

        val unreachableReply = Socks5Reply.fromException(Exception("Host unreachable"))
        assertEquals(Socks5Constants.Reply.HOST_UNREACHABLE, unreachableReply.replyCode)

        val timeoutReply = Socks5Reply.fromException(Exception("Connection timeout"))
        assertEquals(Socks5Constants.Reply.TTL_EXPIRED, timeoutReply.replyCode)

        val genericReply = Socks5Reply.fromException(Exception("Something went wrong"))
        assertEquals(Socks5Constants.Reply.GENERAL_FAILURE, genericReply.replyCode)
    }

    @Test
    fun testToString() {
        assertEquals("SUCCESS", Socks5Reply.success().toString())
        assertEquals("CONNECTION_REFUSED", Socks5Reply.error(Socks5Constants.Reply.CONNECTION_REFUSED).toString())
        assertEquals("COMMAND_NOT_SUPPORTED", Socks5Reply.error(Socks5Constants.Reply.COMMAND_NOT_SUPPORTED).toString())
    }

}
