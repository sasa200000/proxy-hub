package com.example.data.model

import android.content.Context
import android.content.SharedPreferences

object ProxySettings {
    private const val PREFS_NAME = "proxy_settings"
    private const val KEY_ENABLED = "proxy_enabled"
    private const val KEY_TYPE = "proxy_type"
    private const val KEY_HOST = "proxy_host"
    private const val KEY_PORT = "proxy_port"
    private const val KEY_USERNAME = "proxy_username"
    private const val KEY_PASSWORD = "proxy_password"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, config: ProxyConfig) {
        prefs(context).edit().apply {
            putBoolean(KEY_ENABLED, config.enabled)
            putString(KEY_TYPE, config.type.name)
            putString(KEY_HOST, config.host)
            putInt(KEY_PORT, config.port)
            putString(KEY_USERNAME, config.username)
            putString(KEY_PASSWORD, config.password)
            apply()
        }
    }

    fun load(context: Context): ProxyConfig {
        val p = prefs(context)
        return ProxyConfig(
            enabled = p.getBoolean(KEY_ENABLED, false),
            type = try { ProxyProtocol.valueOf(p.getString(KEY_TYPE, "SOCKS5")!!) } catch (e: Exception) { ProxyProtocol.SOCKS5 },
            host = p.getString(KEY_HOST, "") ?: "",
            port = p.getInt(KEY_PORT, 1080),
            username = p.getString(KEY_USERNAME, "") ?: "",
            password = p.getString(KEY_PASSWORD, "") ?: ""
        )
    }
}
