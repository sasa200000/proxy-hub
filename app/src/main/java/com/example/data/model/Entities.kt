package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProtocolType {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    HYSTERIA2,
    TUIC,
    WIREGUARD,
    OTHER
}

enum class ProxyType {
    MTPROTO,
    SOCKS5
}

@Entity(
    tableName = "configs",
    indices = [Index(value = ["rawUri"], unique = true)]
)
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawUri: String,
    val protocol: ProtocolType,
    val remark: String,
    val server: String,
    val port: Int,
    val pingMs: Long = -1, // -1: Not tested, -2: Dead/Timeout, >0: Latency in ms
    val isAlive: Boolean? = null,
    val lastTestedAt: Long = 0,
    val sourceChannel: String = "",
    val isFavorite: Boolean = false,
    val details: String = "" // Additional parsed params (tls, type, security, etc.)
)

@Entity(
    tableName = "proxies",
    indices = [Index(value = ["rawUri"], unique = true)]
)
data class ProxyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawUri: String,
    val type: ProxyType = ProxyType.MTPROTO,
    val server: String,
    val port: Int,
    val secret: String = "",
    val username: String = "",
    val password: String = "",
    val pingMs: Long = -1, // -1: Not tested, -2: Dead/Timeout, >0: Latency in ms
    val isAlive: Boolean? = null,
    val lastTestedAt: Long = 0,
    val sourceChannel: String = "",
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "channels",
    indices = [Index(value = ["username"], unique = true)]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String, // e.g. "v2rayng_org" (without @)
    val title: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val lastFetchTime: Long = 0,
    val fetchedConfigCount: Int = 0,
    val fetchedProxyCount: Int = 0
)

enum class FilterStatus {
    ALL,
    ALIVE_ONLY,
    FAST_ONLY, // < 400ms
    UNTESTED,
    DEAD_ONLY,
    FAVORITES
}

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentChannel: String = "",
    val totalChannels: Int = 0,
    val currentChannelIndex: Int = 0,
    val configsFound: Int = 0,
    val proxiesFound: Int = 0
)

data class TestProgress(
    val isTesting: Boolean = false,
    val totalCount: Int = 0,
    val testedCount: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val currentItemName: String = ""
)
