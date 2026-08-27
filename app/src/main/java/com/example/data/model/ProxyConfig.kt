package com.example.data.model

data class ProxyConfig(
    val enabled: Boolean = false,
    val type: ProxyProtocol = ProxyProtocol.SOCKS5,
    val host: String = "",
    val port: Int = 1080,
    val username: String = "",
    val password: String = ""
)

enum class ProxyProtocol {
    SOCKS5, HTTP, HTTPS
}
