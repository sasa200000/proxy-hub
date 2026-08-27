package com.example.data.fetcher

import com.example.data.model.ConfigEntity
import com.example.data.model.ProxyEntity
import com.example.data.parser.ConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object ChannelFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class FetchResult(
        val username: String,
        val title: String,
        val description: String,
        val configs: List<ConfigEntity>,
        val proxies: List<ProxyEntity>,
        val isSuccess: Boolean,
        val error: String? = null
    )

    /**
     * Cleans username input (e.g., "@channel", "https://t.me/s/channel", "https://t.me/channel" -> "channel")
     */
    fun sanitizeChannelInput(input: String): String {
        var clean = input.trim()
        clean = clean.replace("https://t.me/s/", "")
            .replace("http://t.me/s/", "")
            .replace("https://t.me/", "")
            .replace("http://t.me/", "")
            .replace("t.me/s/", "")
            .replace("t.me/", "")
            .replace("@", "")
            .trim()
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    /**
     * Fetches public posts from a Telegram Channel or direct subscription URL.
     */
    suspend fun fetchChannel(rawInput: String): FetchResult = withContext(Dispatchers.IO) {
        val cleanInput = rawInput.trim()

        // Check if it's a direct full URL (e.g. raw subscription or pastebin link)
        if (cleanInput.startsWith("http://") || cleanInput.startsWith("https://")) {
            if (!cleanInput.contains("t.me")) {
                return@withContext fetchDirectUrl(cleanInput)
            }
        }

        val username = sanitizeChannelInput(cleanInput)
        if (username.isBlank()) {
            return@withContext FetchResult(
                username = "",
                title = "",
                description = "",
                configs = emptyList(),
                proxies = emptyList(),
                isSuccess = false,
                error = "نام کانال نامعتبر است"
            )
        }

        val url = "https://t.me/s/$username"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,fa;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult(
                        username = username,
                        title = "@$username",
                        description = "",
                        configs = emptyList(),
                        proxies = emptyList(),
                        isSuccess = false,
                        error = "خطای دریافت: کد ${response.code}"
                    )
                }

                val html = response.body?.string() ?: ""
                val title = extractTitle(html).ifBlank { "@$username" }
                val description = extractDescription(html)

                val parseResult = ConfigParser.extractAll(html, sourceChannel = username)

                FetchResult(
                    username = username,
                    title = title,
                    description = description,
                    configs = parseResult.configs,
                    proxies = parseResult.proxies,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            FetchResult(
                username = username,
                title = "@$username",
                description = "",
                configs = emptyList(),
                proxies = emptyList(),
                isSuccess = false,
                error = e.localizedMessage ?: "خطای اتصال به شبکه"
            )
        }
    }

    private suspend fun fetchDirectUrl(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "v2rayNG/1.8.19")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult(
                        username = "sub",
                        title = "لینک اشتراک",
                        description = url,
                        configs = emptyList(),
                        proxies = emptyList(),
                        isSuccess = false,
                        error = "کد خطا: ${response.code}"
                    )
                }

                val body = response.body?.string() ?: ""
                val parseResult = ConfigParser.extractAll(body, sourceChannel = "Subscription")

                FetchResult(
                    username = "sub_link",
                    title = "لینک سابسکریپشن",
                    description = url,
                    configs = parseResult.configs,
                    proxies = parseResult.proxies,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            FetchResult(
                username = "sub",
                title = "لینک سابسکریپشن",
                description = url,
                configs = emptyList(),
                proxies = emptyList(),
                isSuccess = false,
                error = e.localizedMessage ?: "خطا در دریافت سابسکریپشن"
            )
        }
    }

    private fun extractTitle(html: String): String {
        return try {
            val pattern = Pattern.compile("<div class=\"tgme_channel_info_header_title\"><span dir=\"auto\">(.*?)</span></div>")
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                cleanHtml(matcher.group(1) ?: "")
            } else {
                val ogTitle = Pattern.compile("<meta property=\"og:title\" content=\"(.*?)\">")
                val ogMatcher = ogTitle.matcher(html)
                if (ogMatcher.find()) cleanHtml(ogMatcher.group(1) ?: "") else ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDescription(html: String): String {
        return try {
            val pattern = Pattern.compile("<div class=\"tgme_channel_info_description\">(.*?)</div>", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                cleanHtml(matcher.group(1) ?: "")
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun cleanHtml(text: String): String {
        return text.replace("<br/>", "\n")
            .replace("<br>", "\n")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("<.*?>"), "")
            .trim()
    }
}
