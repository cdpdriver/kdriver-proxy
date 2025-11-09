package dev.kdriver.proxy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal object LocalProxyController {

    private val logger = LoggerFactory.getLogger("LocalProxyController")

    private val proxies = ConcurrentHashMap<Int, Socks5ProxyServer>()
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
