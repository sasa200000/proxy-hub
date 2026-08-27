package com.example.data.tester

import com.example.data.model.ProxyConfig
import com.example.data.model.ProxyProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
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
     * تست واقعی پروکسی MTProto:
     * 1. TCP connect
     * 2. ارسال secret
     * 3. بررسی پاسخ سرور
     * 4. اگر سرور connection رو بست = پروکسی خرابه
     * 5. اگر سرور داده برگردوند = پروکسی کار می‌کنه
     */
    suspend fun testMtprotoProxy(
        host: String,
        port: Int,
        secret: String,
        timeoutMs: Int = 5000
    ): PingResult = withContext(Dispatchers.IO) {
        if (host.isBlank() || port <= 0 || port > 65535 || secret.isBlank()) {
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

            if (!socket.isConnected) {
                return@withContext PingResult(isAlive = false, pingMs = -2)
            }

            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            // MTProto proxy protocol:
            // Tag: 0xefefefef (4 bytes)
            // Secret length: 4 bytes big-endian
            // Secret: 16 bytes
            val secretBytes = hexStringToByteArray(secret)
            if (secretBytes.size < 16) {
                socket.close()
                return@withContext PingResult(isAlive = false, pingMs = -2)
            }

            // Send tag
            output.write(byteArrayOf(0xEF.toByte(), 0xEF.toByte(), 0xEF.toByte(), 0xEF.toByte()))

            // Send length (16 bytes)
            output.write(byteArrayOf(0x00, 0x00, 0x00, 0x10))

            // Send secret
            output.write(secretBytes.copyOf(16))
            output.flush()

            // Wait for server response
            val buffer = ByteArray(2048)
            var totalRead = 0
            var responseTime = 0L

            try {
                // Wait a bit for response
                Thread.sleep(1000)

                val available = input.available()
                if (available > 0) {
                    val read = input.read(buffer)
                    totalRead = read
                    responseTime = (System.nanoTime() - startTime) / 1_000_000

                    if (read > 0 && !socket.isClosed) {
                        // Got response = proxy is alive and accepted secret
                        return@withContext PingResult(
                            isAlive = true,
                            pingMs = if (responseTime <= 0) 1 else responseTime
                        )
                    }
                }

                // Check if connection is still alive after sending secret
                // Try to read more data (with short timeout)
                try {
                    Thread.sleep(500)
                    val available2 = input.available()
                    if (available2 > 0) {
                        val read2 = input.read(buffer)
                        responseTime = (System.nanoTime() - startTime) / 1_000_000
                        if (read2 > 0) {
                            return@withContext PingResult(
                                isAlive = true,
                                pingMs = if (responseTime <= 0) 1 else responseTime
                            )
                        }
                    }
                } catch (_: Exception) {}

                responseTime = (System.nanoTime() - startTime) / 1_000_000

                // If connection is still open, proxy might be working
                // But if server didn't respond at all, it's suspicious
                if (!socket.isClosed && socket.isConnected) {
                    // Server didn't close connection = proxy might be alive
                    // But no response = likely dead/expired
                    return@withContext PingResult(
                        isAlive = false,
                        pingMs = -2
                    )
                }

                PingResult(isAlive = false, pingMs = -2)

            } catch (e: java.net.SocketTimeoutException) {
                // Timeout on read = server didn't respond = proxy dead
                val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                PingResult(isAlive = false, pingMs = -2)
            } catch (e: java.io.EOFException) {
                // Server closed connection = proxy dead
                PingResult(isAlive = false, pingMs = -2)
            } catch (e: Exception) {
                // Connection reset/closed = proxy dead
                PingResult(isAlive = false, pingMs = -2)
            }

        } catch (e: java.net.ConnectException) {
            // Connection refused = server is not accepting connections
            PingResult(isAlive = false, pingMs = -2)
        } catch (e: Exception) {
            PingResult(isAlive = false, pingMs = -2)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * تست TCP ping معمولی (برای کانفیگ‌ها)
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
                        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, trustAll, SecureRandom())

                    val tlsStartTime = System.nanoTime()
                    val sslSocket = (sslContext.socketFactory as javax.net.ssl.SSLSocketFactory).createSocket() as javax.net.ssl.SSLSocket
                    sslSocket.soTimeout = timeoutMs
                    sslSocket.connect(InetSocketAddress(host, port), timeoutMs)
                    sslSocket.startHandshake()
                    val elapsedTls = (System.nanoTime() - tlsStartTime) / 1_000_000
                    sslSocket.close()

                    return@withContext PingResult(
                        isAlive = true,
                        pingMs = if (elapsedTls <= 0) 1 else elapsedTls
                    )
                } catch (_: Exception) {}
            }

            PingResult(
                isAlive = true,
                pingMs = if (elapsedTcp <= 0) 1 else elapsedTcp
            )
        } catch (e: Exception) {
            PingResult(isAlive = false, pingMs = -2)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun connectThroughProxy(config: ProxyConfig, targetHost: String, targetPort: Int, timeoutMs: Int): Socket {
        return when (config.type) {
            ProxyProtocol.SOCKS5 -> connectSocks5(config, targetHost, targetPort, timeoutMs)
            ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> connectHttp(config, targetHost, targetPort, timeoutMs)
        }
    }

    private fun connectSocks5(config: ProxyConfig, targetHost: String, targetPort: Int, timeoutMs: Int): Socket {
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

    private fun connectHttp(config: ProxyConfig, targetHost: String, targetPort: Int, timeoutMs: Int): Socket {
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

    private fun hexStringToByteArray(s: String): ByteArray {
        val clean = s.removePrefix("dd")
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
