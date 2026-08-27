package com.example.data.tester

import com.example.data.model.ProxyConfig
import com.example.data.model.ProxyProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object PingTester {

    private var proxyConfig: ProxyConfig? = null

    fun updateProxy(config: ProxyConfig?) {
        proxyConfig = config
    }

    data class PingResult(
        val isAlive: Boolean,
        val pingMs: Long
    )

    /**
     * Tests TCP connectivity and latency.
     * For port 443, also tries TLS handshake to verify the server is truly reachable.
     */
    suspend fun testTcpPing(
        host: String,
        port: Int,
        timeoutMs: Int = 2500
    ): PingResult = withContext(Dispatchers.IO) {
        if (host.isBlank() || port <= 0 || port > 65535) {
            return@withContext PingResult(isAlive = false, pingMs = -2)
        }

        var socket: Socket? = null
        try {
            val startTime = System.nanoTime()

            val cfg = proxyConfig
            socket = if (cfg != null && cfg.enabled && cfg.host.isNotBlank()) {
                connectThroughProxy(cfg, host, port, timeoutMs)
            } else {
                val s = Socket()
                s.soTimeout = timeoutMs
                s.connect(InetSocketAddress(host, port), timeoutMs)
                s
            }

            val elapsedTcp = (System.nanoTime() - startTime) / 1_000_000

            // If port 443, try TLS handshake for more accurate result
            if (port == 443 && socket.isConnected) {
                try {
                    val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    })
                    val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                    sslContext.init(null, trustAll, java.security.SecureRandom())

                    val factory = sslContext.socketFactory as SSLSocketFactory
                    val tlsStartTime = System.nanoTime()
                    val sslSocket = factory.createSocket(socket, host, port) as SSLSocket
                    sslSocket.soTimeout = timeoutMs
                    sslSocket.startHandshake()
                    val elapsedTls = (System.nanoTime() - tlsStartTime) / 1_000_000
                    sslSocket.close()

                    return@withContext PingResult(
                        isAlive = true,
                        pingMs = if (elapsedTls <= 0) 1 else elapsedTls
                    )
                } catch (_: Exception) {
                    // TLS failed but TCP worked - still alive but slower
                }
            }

            val elapsedMs = elapsedTcp
            PingResult(
                isAlive = true,
                pingMs = if (elapsedMs <= 0) 1 else elapsedMs
            )
        } catch (e: Exception) {
            PingResult(
                isAlive = false,
                pingMs = -2
            )
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun connectThroughProxy(
        config: ProxyConfig,
        targetHost: String,
        targetPort: Int,
        timeoutMs: Int
    ): Socket {
        return when (config.type) {
            ProxyProtocol.SOCKS5 -> connectSocks5(config, targetHost, targetPort, timeoutMs)
            ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> connectHttp(config, targetHost, targetPort, timeoutMs)
        }
    }

    private fun connectSocks5(
        config: ProxyConfig,
        targetHost: String,
        targetPort: Int,
        timeoutMs: Int
    ): Socket {
        val socket = Socket()
        socket.soTimeout = timeoutMs
        socket.connect(InetSocketAddress(config.host, config.port), timeoutMs)
        val output = socket.getOutputStream()
        val input = socket.getInputStream()

        output.write(byteArrayOf(0x05, 0x01, 0x00))
        output.flush()

        val response = ByteArray(2)
        readFull(input, response)

        if (response[0] != 0x05.toByte()) {
            socket.close()
            throw Exception("SOCKS5 handshake failed")
        }

        if (response[1] != 0x00.toByte()) {
            output.write(byteArrayOf(0x05, 0x01, 0x02))
            output.flush()
            val authResp = ByteArray(2)
            readFull(input, authResp)
            if (authResp[1] != 0x02.toByte()) {
                socket.close()
                throw Exception("SOCKS5 auth required")
            }
            val username = config.username.toByteArray()
            val password = config.password.toByteArray()
            val authMsg = ByteArray(3 + username.size + 1 + password.size)
            authMsg[0] = 0x01
            authMsg[1] = username.size.toByte()
            System.arraycopy(username, 0, authMsg, 2, username.size)
            authMsg[2 + username.size] = password.size.toByte()
            System.arraycopy(password, 0, authMsg, 3 + username.size, password.size)
            output.write(authMsg)
            output.flush()
            val authResult = ByteArray(2)
            readFull(input, authResult)
            if (authResult[1] != 0x00.toByte()) {
                socket.close()
                throw Exception("SOCKS5 auth failed")
            }
        }

        val hostBytes = targetHost.toByteArray()
        val connectReq = ByteArray(7 + hostBytes.size)
        connectReq[0] = 0x05
        connectReq[1] = 0x01
        connectReq[2] = 0x00
        connectReq[3] = 0x03
        connectReq[4] = hostBytes.size.toByte()
        System.arraycopy(hostBytes, 0, connectReq, 5, hostBytes.size)
        connectReq[5 + hostBytes.size] = ((targetPort shr 8) and 0xFF).toByte()
        connectReq[6 + hostBytes.size] = (targetPort and 0xFF).toByte()
        output.write(connectReq)
        output.flush()

        val connectResp = ByteArray(10)
        readFull(input, connectResp)

        if (connectResp[1] != 0x00.toByte()) {
            socket.close()
            throw Exception("SOCKS5 connect failed: ${connectResp[1]}")
        }

        return socket
    }

    private fun connectHttp(
        config: ProxyConfig,
        targetHost: String,
        targetPort: Int,
        timeoutMs: Int
    ): Socket {
        val socket = Socket()
        socket.soTimeout = timeoutMs
        socket.connect(InetSocketAddress(config.host, config.port), timeoutMs)

        val output = socket.getOutputStream()
        val input = socket.getInputStream()

        val connectRequest = "CONNECT $targetHost:$targetPort HTTP/1.1\r\nHost: $targetHost:$targetPort\r\n\r\n"
        output.write(connectRequest.toByteArray())
        output.flush()

        val reader = BufferedReader(InputStreamReader(input))
        val statusLine = reader.readLine() ?: throw Exception("HTTP CONNECT failed")

        if (!statusLine.contains("200")) {
            socket.close()
            throw Exception("HTTP CONNECT failed: $statusLine")
        }

        var line = reader.readLine()
        while (line != null && line.isNotEmpty()) {
            line = reader.readLine()
        }

        return socket
    }

    private fun readFull(input: java.io.InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw java.io.EOFException("Connection closed")
            offset += read
        }
    }
}
