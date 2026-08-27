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
import java.security.SecureRandom
import java.security.cert.X509Certificate
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
     * Tests TCP connectivity and latency (in milliseconds) to target host:port.
     * Uses proxy if configured.
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
            if (cfg != null && cfg.enabled && cfg.host.isNotBlank()) {
                // Connect through proxy
                socket = connectThroughProxy(cfg, host, port, timeoutMs)
            } else {
                // Direct connection
                socket = Socket()
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }

            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

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
            try {
                socket?.close()
            } catch (_: Exception) {}
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

        // Connect to SOCKS5 proxy
        socket.connect(InetSocketAddress(config.host, config.port), timeoutMs)
        val output = socket.getOutputStream()
        val input = socket.getInputStream()

        // SOCKS5 handshake: version 5, no auth
        output.write(byteArrayOf(0x05, 0x01, 0x00))
        output.flush()

        // Read server response
        val response = ByteArray(2)
        readFull(input, response)

        if (response[0] != 0x05.toByte()) {
            socket.close()
            throw Exception("SOCKS5 handshake failed: version ${response[0]}")
        }

        if (response[1] != 0x00.toByte()) {
            // Try with auth
            output.write(byteArrayOf(0x05, 0x01, 0x02))
            output.flush()
            val authResp = ByteArray(2)
            readFull(input, authResp)
            if (authResp[1] != 0x02.toByte()) {
                socket.close()
                throw Exception("SOCKS5 auth required but no credentials provided")
            }
            // Username/Password auth (RFC 1929)
            val username = config.username.toByteArray()
            val password = config.password.toByteArray()
            val authMsg = ByteArray(3 + username.size + 1 + password.size)
            authMsg[0] = 0x01 // Version
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
                throw Exception("SOCKS5 authentication failed")
            }
        }

        // SOCKS5 connect request: version 5, connect command, reserved, domain type
        val hostBytes = targetHost.toByteArray()
        val connectReq = ByteArray(7 + hostBytes.size)
        connectReq[0] = 0x05 // Version
        connectReq[1] = 0x01 // Connect command
        connectReq[2] = 0x00 // Reserved
        connectReq[3] = 0x03 // Domain name type
        connectReq[4] = hostBytes.size.toByte()
        System.arraycopy(hostBytes, 0, connectReq, 5, hostBytes.size)
        connectReq[5 + hostBytes.size] = ((targetPort shr 8) and 0xFF).toByte()
        connectReq[6 + hostBytes.size] = (targetPort and 0xFF).toByte()
        output.write(connectReq)
        output.flush()

        // Read connect response
        val connectResp = ByteArray(10)
        readFull(input, connectResp)

        if (connectResp[1] != 0x00.toByte()) {
            socket.close()
            val error = when (connectResp[1].toInt() and 0xFF) {
                0x01 -> "General SOCKS server failure"
                0x02 -> "Connection not allowed by ruleset"
                0x03 -> "Network unreachable"
                0x04 -> "Host unreachable"
                0x05 -> "Connection refused"
                0x06 -> "TTL expired"
                0x07 -> "Command not supported"
                0x08 -> "Address type not supported"
                else -> "Unknown error ${connectResp[1]}"
            }
            throw Exception("SOCKS5 connect failed: $error")
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

        // HTTP CONNECT
        val connectRequest = "CONNECT $targetHost:$targetPort HTTP/1.1\r\nHost: $targetHost:$targetPort\r\n\r\n"
        output.write(connectRequest.toByteArray())
        output.flush()

        // Read response
        val reader = BufferedReader(InputStreamReader(input))
        val statusLine = reader.readLine() ?: throw Exception("HTTP CONNECT failed: no response")

        if (!statusLine.contains("200")) {
            socket.close()
            throw Exception("HTTP CONNECT failed: $statusLine")
        }

        // Skip remaining headers
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
            if (read == -1) throw java.io.EOFException("Connection closed during SOCKS5 handshake")
            offset += read
        }
    }
}
