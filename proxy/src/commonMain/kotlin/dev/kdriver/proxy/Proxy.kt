package dev.kdriver.proxy

data class Proxy(
    val url: ProxyUrl,
    val username: String? = null,
    val password: String? = null,
) {

    companion object {

        /**
         * Create a Proxy from a URL string
         */
        fun fromUrl(url: String, username: String? = null, password: String? = null): Proxy {
            return Proxy(ProxyUrl.parse(url), username, password)
        }

    }

}
