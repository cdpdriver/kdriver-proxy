package dev.kdriver.proxy

import io.ktor.http.*

/**
 * Represents a network proxy configuration
 */
data class Proxy(
    /**
     * The proxy URL
     */
    val url: Url,
    /**
     * Optional username for proxy authentication
     */
    val username: String? = null,
    /**
     * Optional password for proxy authentication
     */
    val password: String? = null,
) {

    /**
     * Create a Proxy from a URL string
     *
     * @param url The proxy URL string
     * @param username Optional username for proxy authentication
     * @param password Optional password for proxy authentication
     *
     * @return A Proxy instance
     */
    constructor(
        url: String,
        username: String? = null,
        password: String? = null,
    ) : this(
        Url(url),
        username,
        password
    )

}
