package dev.kdriver.proxy

import io.ktor.util.collections.*
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object LocalProxyController {

    private val logger = KtorSimpleLogger("LocalProxyController")

    private val proxies = ConcurrentMap<Int, Socks5ProxyServer>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startProxy(port: Int, proxy: Proxy) {
        if (proxies.containsKey(port)) {
            logger.info("Proxy on port $port is already running.")
            return
        }

        val server = Socks5ProxyServer(port, proxy)
        server.start(scope)
        proxies[port] = server
        logger.info("Started proxy on port $port")
    }

    fun stopProxy(port: Int) {
        proxies.remove(port)?.let {
            it.stop()
            logger.info("Stopped proxy on port $port")
        } ?: logger.info("No proxy found on port $port")
    }

    fun stopAll() {
        proxies.values.forEach { it.stop() }
        proxies.clear()
    }

}
