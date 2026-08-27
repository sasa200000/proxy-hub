package com.example.data.parser

import android.net.Uri
import android.util.Base64
import com.example.data.model.ConfigEntity
import com.example.data.model.ProtocolType
import com.example.data.model.ProxyEntity
import com.example.data.model.ProxyType
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigParser {

    private val CONFIG_REGEX = Regex("""(?i)(?:vless|vmess|trojan|ss|hysteria2|hy2|tuic|wireguard)://[^\s<>"'\r\n)\]}]+""")
    private val PROXY_REGEX = Regex("""(?i)(?:tg://(?:proxy|socks)\?[^\s<>"'\r\n)\]}]+|https?://t\.me/(?:proxy|socks)\?[^\s<>"'\r\n)\]}]+|socks5://[^\s<>"'\r\n)\]}]+)""")

    data class ParseResult(
        val configs: List<ConfigEntity>,
        val proxies: List<ProxyEntity>
    )

    /**
     * Parses any text block, HTML message, or subscription body
     * and extracts all valid V2Ray configs and Telegram proxies.
     */
    fun extractAll(rawText: String, sourceChannel: String = ""): ParseResult {
        if (rawText.isBlank()) return ParseResult(emptyList(), emptyList())

        val configs = mutableListOf<ConfigEntity>()
        val proxies = mutableListOf<ProxyEntity>()
        val seenConfigUris = mutableSetOf<String>()
        val seenProxyUris = mutableSetOf<String>()

        // Check if rawText is a full Base64 encoded subscription
        val decodedSubscription = tryDecodeBase64(rawText.trim())
        val textToProcess = if (decodedSubscription.isNotBlank() && 
            (decodedSubscription.contains("vmess://") || decodedSubscription.contains("vless://") || decodedSubscription.contains("trojan://") || decodedSubscription.contains("ss://"))) {
            decodedSubscription + "\n" + rawText
        } else {
            rawText
        }

        // 1. Extract Configs with Regex
        for (match in CONFIG_REGEX.findAll(textToProcess)) {
            val uri = cleanUri(match.value)
            if (uri.length > 10 && seenConfigUris.add(uri)) {
                parseConfigUri(uri, sourceChannel)?.let { configs.add(it) }
            }
        }

        // 2. Extract Proxies with Regex
        for (match in PROXY_REGEX.findAll(textToProcess)) {
            val uri = cleanUri(match.value)
            if (uri.length > 10 && seenProxyUris.add(uri)) {
                parseProxyUri(uri, sourceChannel)?.let { proxies.add(it) }
            }
        }

        return ParseResult(configs = configs, proxies = proxies)
    }

    private fun cleanUri(raw: String): String {
        var clean = raw.trim()
        val endChars = listOf('<', '>', '"', '\'', ' ', '\t', '\n', '\r', ')', ']', '}', '،', ',')
        for (ch in endChars) {
            val idx = clean.indexOf(ch)
            if (idx != -1) {
                clean = clean.substring(0, idx)
            }
        }
        return clean
    }

    fun parseConfigUri(rawUri: String, sourceChannel: String = ""): ConfigEntity? {
        val trimmed = cleanUri(rawUri)
        return when {
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed, sourceChannel)
            trimmed.startsWith("vless://", ignoreCase = true) -> parseStandardUri(trimmed, ProtocolType.VLESS, sourceChannel)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseStandardUri(trimmed, ProtocolType.TROJAN, sourceChannel)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed, sourceChannel)
            trimmed.startsWith("hysteria2://", ignoreCase = true) || trimmed.startsWith("hy2://", ignoreCase = true) -> 
                parseStandardUri(trimmed, ProtocolType.HYSTERIA2, sourceChannel)
            trimmed.startsWith("tuic://", ignoreCase = true) -> parseStandardUri(trimmed, ProtocolType.TUIC, sourceChannel)
            trimmed.startsWith("wireguard://", ignoreCase = true) -> parseStandardUri(trimmed, ProtocolType.WIREGUARD, sourceChannel)
            else -> null
        }
    }

    private fun parseVmess(uri: String, sourceChannel: String): ConfigEntity? {
        return try {
            val base64Part = uri.substring(8).trim()
            val decoded = decodeBase64Safe(base64Part)
            if (decoded.startsWith("{") && decoded.endsWith("}")) {
                val json = JSONObject(decoded)
                val server = json.optString("add", "").ifBlank { json.optString("host", "") }
                val port = json.optInt("port", 443)
                val remark = json.optString("ps", "VMess Server").let { decodeUrlSafe(it) }
                val net = json.optString("net", "tcp")
                val tls = json.optString("tls", "")

                if (server.isNotBlank() && port > 0) {
                    ConfigEntity(
                        rawUri = uri,
                        protocol = ProtocolType.VMESS,
                        remark = remark.ifBlank { "VMess $server:$port" },
                        server = server,
                        port = port,
                        sourceChannel = sourceChannel,
                        details = "Type: $net | TLS: ${tls.ifBlank { "none" }}"
                    )
                } else null
            } else {
                parseStandardUri(uri, ProtocolType.VMESS, sourceChannel)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseStandardUri(uri: String, protocol: ProtocolType, sourceChannel: String): ConfigEntity? {
        return try {
            var host = ""
            var port = 443
            var remark = ""
            var security = ""
            var type = ""
            var sni = ""

            val fragmentIdx = uri.indexOf('#')
            if (fragmentIdx != -1) {
                remark = decodeUrlSafe(uri.substring(fragmentIdx + 1))
            }

            val queryIdx = uri.indexOf('?')
            val mainPart = when {
                queryIdx != -1 -> uri.substring(0, queryIdx)
                fragmentIdx != -1 -> uri.substring(0, fragmentIdx)
                else -> uri
            }

            if (mainPart.contains("@")) {
                val afterAt = mainPart.substringAfter("@")
                val hp = afterAt.split(":")
                host = hp[0].trim()
                port = hp.getOrNull(1)?.toIntOrNull() ?: 443
            } else {
                val noScheme = mainPart.substringAfter("://")
                val hp = noScheme.split(":")
                host = hp[0].trim()
                port = hp.getOrNull(1)?.toIntOrNull() ?: 443
            }

            if (queryIdx != -1) {
                val queryString = if (fragmentIdx != -1 && fragmentIdx > queryIdx) {
                    uri.substring(queryIdx + 1, fragmentIdx)
                } else {
                    uri.substring(queryIdx + 1)
                }
                for (param in queryString.split("&")) {
                    val kv = param.split("=")
                    if (kv.size == 2) {
                        val k = kv[0].lowercase()
                        val v = decodeUrlSafe(kv[1])
                        when (k) {
                            "security", "tls" -> security = v
                            "type", "net" -> type = v
                            "sni", "host" -> sni = v
                        }
                    }
                }
            }

            if (remark.isBlank()) {
                remark = "${protocol.name} $host:$port"
            }

            if (host.isNotBlank() && port > 0) {
                ConfigEntity(
                    rawUri = uri,
                    protocol = protocol,
                    remark = remark,
                    server = host,
                    port = port,
                    sourceChannel = sourceChannel,
                    details = listOfNotNull(
                        if (security.isNotBlank()) "Security: $security" else null,
                        if (type.isNotBlank()) "Type: $type" else null,
                        if (sni.isNotBlank()) "SNI: $sni" else null
                    ).joinToString(" | ")
                )
            } else null
        } catch (e: Exception) {
            parseWithRegex(uri, protocol, sourceChannel)
        }
    }

    private fun parseShadowsocks(uri: String, sourceChannel: String): ConfigEntity? {
        return try {
            val noPrefix = uri.substring(5)
            val fragmentIndex = noPrefix.indexOf('#')
            val rawRemark = if (fragmentIndex != -1) noPrefix.substring(fragmentIndex + 1) else ""
            val remark = decodeUrlSafe(rawRemark).ifBlank { "Shadowsocks Server" }
            val mainPart = if (fragmentIndex != -1) noPrefix.substring(0, fragmentIndex) else noPrefix

            if (mainPart.contains("@")) {
                val atSplit = mainPart.split("@")
                val hostPort = atSplit.getOrNull(1) ?: return null
                val hpSplit = hostPort.split(":")
                val host = hpSplit[0]
                val port = hpSplit.getOrNull(1)?.toIntOrNull() ?: 8388
                ConfigEntity(
                    rawUri = uri,
                    protocol = ProtocolType.SHADOWSOCKS,
                    remark = remark,
                    server = host,
                    port = port,
                    sourceChannel = sourceChannel,
                    details = "SS Standard"
                )
            } else {
                val decoded = decodeBase64Safe(mainPart)
                if (decoded.contains("@")) {
                    val atSplit = decoded.split("@")
                    val hostPort = atSplit.getOrNull(1) ?: return null
                    val hpSplit = hostPort.split(":")
                    val host = hpSplit[0]
                    val port = hpSplit.getOrNull(1)?.toIntOrNull() ?: 8388
                    val method = atSplit[0].split(":").firstOrNull() ?: "aes-256-gcm"
                    ConfigEntity(
                        rawUri = uri,
                        protocol = ProtocolType.SHADOWSOCKS,
                        remark = remark,
                        server = host,
                        port = port,
                        sourceChannel = sourceChannel,
                        details = "Cipher: $method"
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWithRegex(uri: String, protocol: ProtocolType, sourceChannel: String): ConfigEntity? {
        return try {
            val regex = Regex("""@([a-zA-Z0-9.\-_]+):(\d+)""")
            val match = regex.find(uri)
            if (match != null) {
                val host = match.groupValues[1]
                val port = match.groupValues[2].toIntOrNull() ?: 443
                val fragment = uri.substringAfter('#', "").let { decodeUrlSafe(it) }
                ConfigEntity(
                    rawUri = uri,
                    protocol = protocol,
                    remark = fragment.ifBlank { "${protocol.name} $host:$port" },
                    server = host,
                    port = port,
                    sourceChannel = sourceChannel
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun parseProxyUri(rawUri: String, sourceChannel: String = ""): ProxyEntity? {
        return try {
            val uri = cleanUri(rawUri)
            val queryParams = mutableMapOf<String, String>()
            val queryIdx = uri.indexOf('?')
            if (queryIdx != -1) {
                val qStr = uri.substring(queryIdx + 1)
                for (param in qStr.split("&")) {
                    val kv = param.split("=")
                    if (kv.size == 2) {
                        queryParams[kv[0].lowercase()] = decodeUrlSafe(kv[1])
                    }
                }
            }

            val server = queryParams["server"] ?: queryParams["ip"] ?: ""
            val portStr = queryParams["port"]
            val port = portStr?.toIntOrNull() ?: 443
            val secret = queryParams["secret"] ?: ""
            val user = queryParams["user"] ?: ""
            val pass = queryParams["pass"] ?: ""

            val type = if (uri.contains("socks", ignoreCase = true)) ProxyType.SOCKS5 else ProxyType.MTPROTO

            if (server.isNotBlank() && port > 0) {
                val canonicalUri = if (type == ProxyType.MTPROTO) {
                    "tg://proxy?server=$server&port=$port&secret=$secret"
                } else {
                    if (user.isNotBlank()) "tg://socks?server=$server&port=$port&user=$user&pass=$pass"
                    else "tg://socks?server=$server&port=$port"
                }

                ProxyEntity(
                    rawUri = canonicalUri,
                    type = type,
                    server = server,
                    port = port,
                    secret = secret,
                    username = user,
                    password = pass,
                    sourceChannel = sourceChannel
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBase64Safe(input: String): String {
        return try {
            val clean = input.replace("-", "+").replace("_", "/")
            val padded = clean.padEnd((clean.length + 3) / 4 * 4, '=')
            val bytes = Base64.decode(padded, Base64.DEFAULT or Base64.NO_WRAP)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val bytes = java.util.Base64.getDecoder().decode(input)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun tryDecodeBase64(input: String): String {
        if (input.contains(" ") || input.contains("\n") || input.contains("://")) return ""
        return decodeBase64Safe(input)
    }

    private fun decodeUrlSafe(input: String): String {
        return try {
            URLDecoder.decode(input, "UTF-8")
        } catch (e: Exception) {
            input
        }
    }
}
