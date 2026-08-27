package com.example.data.tester

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    data class PingResult(
        val isAlive: Boolean,
        val pingMs: Long
    )

    /**
     * Tests TCP connectivity and latency (in milliseconds) to target host:port.
     * Returns PingResult with accurate latency or -2 if unreachable/dead.
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
            socket = Socket()
            socket.soTimeout = timeoutMs
            val socketAddress = InetSocketAddress(host, port)
            socket.connect(socketAddress, timeoutMs)

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
}
