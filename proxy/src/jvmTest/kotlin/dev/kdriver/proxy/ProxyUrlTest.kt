package dev.kdriver.proxy

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyUrlTest {

    @Test
    fun testParseHttpUrl() {
        val url = ProxyUrl.parse("http://proxy.example.com:8080")
        assertEquals("http", url.scheme)
        assertEquals("proxy.example.com", url.host)
        assertEquals(8080, url.port)
    }

    @Test
    fun testParseHttpsUrl() {
        val url = ProxyUrl.parse("https://proxy.example.com:8443")
        assertEquals("https", url.scheme)
        assertEquals("proxy.example.com", url.host)
        assertEquals(8443, url.port)
    }

    @Test
    fun testParseUrlWithDefaultHttpPort() {
        val url = ProxyUrl.parse("http://proxy.example.com")
        assertEquals("http", url.scheme)
        assertEquals("proxy.example.com", url.host)
        assertEquals(80, url.port)
    }

    @Test
    fun testParseUrlWithDefaultHttpsPort() {
        val url = ProxyUrl.parse("https://proxy.example.com")
        assertEquals("https", url.scheme)
        assertEquals("proxy.example.com", url.host)
        assertEquals(443, url.port)
    }

    @Test
    fun testParseHostPort() {
        val url = ProxyUrl.parse("proxy.example.com:8080")
        assertEquals("http", url.scheme) // defaults to http
        assertEquals("proxy.example.com", url.host)
        assertEquals(8080, url.port)
    }

    @Test
    fun testToString() {
        val url = ProxyUrl("https", "proxy.example.com", 8443)
        assertEquals("https://proxy.example.com:8443", url.toString())
    }

}
