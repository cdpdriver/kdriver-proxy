package dev.kdriver.proxy

/**
 * KMP-compatible URL representation for proxy configuration
 */
data class ProxyUrl(
    val scheme: String,
    val host: String,
    val port: Int,
) {

    companion object {

        /**
         * Parse a URL string into a ProxyUrl
         * Supports formats:
         * - http://host:port
         * - https://host:port
         * - host:port (defaults to http)
         */
        fun parse(url: String): ProxyUrl {
            // Remove leading/trailing whitespace
            val trimmed = url.trim()

            // Check if URL has a scheme
            val schemeEnd = trimmed.indexOf("://")
            val (scheme, remaining) = if (schemeEnd != -1) {
                val s = trimmed.substring(0, schemeEnd).lowercase()
                val r = trimmed.substring(schemeEnd + 3)
                s to r
            } else {
                "http" to trimmed
            }

            // Parse host and port
            val hostPortEnd = remaining.indexOf('/')
            val hostPort = if (hostPortEnd != -1) {
                remaining.substring(0, hostPortEnd)
            } else {
                remaining
            }

            val parts = hostPort.split(':')
            require(parts.isNotEmpty()) { "Invalid URL: $url" }

            val host = parts[0]
            require(host.isNotBlank()) { "Host cannot be empty: $url" }

            val port = if (parts.size > 1) {
                parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid port: ${parts[1]}")
            } else {
                // Default port based on scheme
                when (scheme) {
                    "https" -> 443
                    "http" -> 80
                    else -> throw IllegalArgumentException("No port specified and cannot determine default for scheme: $scheme")
                }
            }

            return ProxyUrl(scheme, host, port)
        }

    }

    override fun toString(): String = "$scheme://$host:$port"

}
