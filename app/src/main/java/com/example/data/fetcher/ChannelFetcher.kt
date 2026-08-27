package com.example.data.fetcher

import com.example.data.model.ConfigEntity
import com.example.data.model.ProxyConfig
import com.example.data.model.ProxyEntity
import com.example.data.model.ProxyProtocol
import com.example.data.parser.ConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ChannelFetcher {

    // Cloudflare Worker URL - بدون فیلتر کار می‌کنه
    private const val WORKER_URL = "https://proxyhub-worker.sasa200000.workers.dev"

    // منابع رایگان GitHub - بدون VPN قابل دسترس!
    val FREE_GITHUB_SOURCES = listOf(
        GitHubSource(
            name = "V2Ray Config (Barry)",
            url = "https://raw.githubusercontent.com/barry-far/V2ray-config/main/Sub1.txt",
            description = "بیش از ۱۰۰۰ کانفیگ V2Ray - آپدیت هر ۱۵ دقیقه"
        ),
        GitHubSource(
            name = "Free V2Ray Public List",
            url = "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/refs/heads/main/all_extracted_configs.txt",
            description = "لیست کانفیگ‌های رایگان VLESS/VMess/Trojan/SS"
        ),
        GitHubSource(
            name = "0xRadikal Free Configs",
            url = "https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/main/Sub1.txt",
            description = "کانفیگ‌های رایگان تست‌شده - VLESS/VMess/Trojan"
        ),
        GitHubSource(
            name = "Port-Based Configs",
            url = "https://raw.githubusercontent.com/hamedcode/port-based-v2ray-configs/main/Subs/All/All.txt",
            description = "بیش از ۱۱۰۰۰ کانفیگ بر اساس پورت"
        ),
        GitHubSource(
            name = "Epodonios Configs",
            url = "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/All_Configs_Sub.txt",
            description = "کانفیگ‌های رایگان V2Ray - آپدیت هر ۵ دقیقه"
        ),
        GitHubSource(
            name = "MatinGhanbari Configs",
            url = "https://raw.githubusercontent.com/MatinGhanbari/v2ray-configs/main/sub_list/All_Configs_Sub.txt",
            description = "کانفیگ‌های V2Ray رایگان - آپدیت هر ۱۵ دقیقه"
        )
    )

    data class GitHubSource(
        val name: String,
        val url: String,
        val description: String
    )

    private var currentProxyConfig: ProxyConfig? = null
    private var cachedClient: OkHttpClient? = null

    fun updateProxy(proxyConfig: ProxyConfig?) {
        currentProxyConfig = proxyConfig
        cachedClient = null
    }

    private fun buildClient(proxyConfig: ProxyConfig?): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }

        if (proxyConfig != null && proxyConfig.enabled && proxyConfig.host.isNotBlank()) {
            val javaProxy = when (proxyConfig.type) {
                ProxyProtocol.SOCKS5 -> Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyConfig.host, proxyConfig.port))
                ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyConfig.host, proxyConfig.port))
            }
            builder.proxy(javaProxy)
        }

        return builder.build()
    }

    private fun getClient(): OkHttpClient {
        if (cachedClient == null) {
            cachedClient = buildClient(currentProxyConfig)
        }
        return cachedClient!!
    }

    data class FetchResult(
        val username: String,
        val title: String,
        val description: String,
        val configs: List<ConfigEntity>,
        val proxies: List<ProxyEntity>,
        val isSuccess: Boolean,
        val error: String? = null
    )

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
     * دریافت کانفیگ‌ها از یک منبع GitHub
     * این روش بدون VPN کار می‌کنه!
     */
    suspend fun fetchGitHubSource(source: GitHubSource): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", "v2rayNG/1.8.19")
                .header("Accept", "*/*")
                .build()

            val response = getClient().newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext FetchResult(
                        username = source.name, title = source.name,
                        description = source.description,
                        configs = emptyList(), proxies = emptyList(),
                        isSuccess = false, error = "خطای HTTP: ${resp.code}"
                    )
                }

                val content = resp.body?.string() ?: ""
                if (content.isBlank()) {
                    return@withContext FetchResult(
                        username = source.name, title = source.name,
                        description = source.description,
                        configs = emptyList(), proxies = emptyList(),
                        isSuccess = false, error = "محتوای خالی"
                    )
                }

                val parseResult = ConfigParser.extractAll(content, sourceChannel = source.name)
                return@withContext FetchResult(
                    username = source.name,
                    title = source.name,
                    description = source.description,
                    configs = parseResult.configs,
                    proxies = parseResult.proxies,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            return@withContext FetchResult(
                username = source.name, title = source.name,
                description = source.description,
                configs = emptyList(), proxies = emptyList(),
                isSuccess = false, error = e.localizedMessage ?: "خطا"
            )
        }
    }

    /**
     * دریافت از تمام منابع GitHub رایگان
     * @return لیست نتایج هر منبع
     */
    suspend fun fetchAllGitHubSources(): List<FetchResult> {
        val results = mutableListOf<FetchResult>()
        for (source in FREE_GITHUB_SOURCES) {
            val result = fetchGitHubSource(source)
            results.add(result)
        }
        return results
    }

    suspend fun fetchChannel(rawInput: String): FetchResult = withContext(Dispatchers.IO) {
        val cleanInput = rawInput.trim()

        // Direct URL subscription (GitHub raw, subscription links, etc)
        if (cleanInput.startsWith("http://") || cleanInput.startsWith("https://")) {
            if (!cleanInput.contains("t.me")) {
                return@withContext fetchDirectUrl(cleanInput)
            }
        }

        val username = sanitizeChannelInput(cleanInput)
        if (username.isBlank()) {
            return@withContext FetchResult(
                username = "", title = "", description = "",
                configs = emptyList(), proxies = emptyList(), isSuccess = false, error = "نام کانال نامعتبر است"
            )
        }

        var lastError: String? = null

        // اول از Worker (بدون فیلتر) استفاده کن
        try {
            val workerResult = fetchViaWorker(username)
            if (workerResult.isSuccess) {
                return@withContext workerResult
            }
            lastError = workerResult.error
        } catch (e: Exception) {
            lastError = e.localizedMessage
        }

        // اگر Worker کار نکرد، مستقیم تلاش کن
        val directUrls = listOf("https://t.me/s/$username", "https://t.me/$username")
        for (url in directUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,fa;q=0.8")
                    .build()

                getClient().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = "خطای HTTP: ${response.code}"
                        return@use
                    }

                    val html = response.body?.string() ?: ""
                    if (html.isBlank()) {
                        lastError = "پاسخ خالی"
                        return@use
                    }

                    val title = extractTitle(html).ifBlank { "@$username" }
                    val description = extractDescription(html)
                    val parseResult = ConfigParser.extractAll(html, sourceChannel = username)

                    return@withContext FetchResult(
                        username = username, title = title, description = description,
                        configs = parseResult.configs, proxies = parseResult.proxies,
                        isSuccess = true
                    )
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "خطای اتصال"
            }
        }

        FetchResult(
            username = username, title = "@$username", description = "",
            configs = emptyList(), proxies = emptyList(),
            isSuccess = false, error = lastError ?: "خطای ناشناخته"
        )
    }

    private fun fetchViaWorker(username: String): FetchResult {
        val url = "$WORKER_URL/api/channel/$username"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        getClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return FetchResult(
                    username = username, title = "@$username", description = "",
                    configs = emptyList(), proxies = emptyList(),
                    isSuccess = false, error = "Worker HTTP ${response.code}"
                )
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)

            if (!json.optBoolean("success", false)) {
                return FetchResult(
                    username = username, title = "@$username", description = "",
                    configs = emptyList(), proxies = emptyList(),
                    isSuccess = false, error = json.optString("error", "Worker error")
                )
            }

            val title = json.optString("title", "@$username")
            val description = json.optString("description", "")
            val html = json.optString("html", "")

            val parseResult = ConfigParser.extractAll(html, sourceChannel = username)

            return FetchResult(
                username = username, title = title, description = description,
                configs = parseResult.configs, proxies = parseResult.proxies,
                isSuccess = true
            )
        }
    }

    private suspend fun fetchDirectUrl(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            // اول تلاش از Worker
            val workerUrl = "$WORKER_URL/api/fetch?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            val request = Request.Builder()
                .url(workerUrl)
                .header("Accept", "application/json")
                .build()

            getClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) {
                        val content = json.optString("body", "")
                        val parseResult = ConfigParser.extractAll(content, sourceChannel = "Subscription")
                        return@withContext FetchResult(
                            username = "sub_link", title = "لینک سابسکریپشن", description = url,
                            configs = parseResult.configs, proxies = parseResult.proxies,
                            isSuccess = true
                        )
                    }
                }
            }

            // Fallback: مستقیم
            val directRequest = Request.Builder()
                .url(url)
                .header("User-Agent", "v2rayNG/1.8.19")
                .build()

            getClient().newCall(directRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult(
                        username = "sub", title = "لینک اشتراک", description = url,
                        configs = emptyList(), proxies = emptyList(),
                        isSuccess = false, error = "کد خطا: ${response.code}"
                    )
                }
                val content = response.body?.string() ?: ""
                val parseResult = ConfigParser.extractAll(content, sourceChannel = "Subscription")
                FetchResult(
                    username = "sub_link", title = "لینک سابسکریپشن", description = url,
                    configs = parseResult.configs, proxies = parseResult.proxies,
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            FetchResult(
                username = "sub", title = "لینک سابسکریپشن", description = url,
                configs = emptyList(), proxies = emptyList(),
                isSuccess = false, error = e.localizedMessage ?: "خطا"
            )
        }
    }

    private fun extractTitle(html: String): String {
        return try {
            val pattern = Pattern.compile("<div class=\"tgme_channel_info_header_title\"><span dir=\"auto\">(.*?)</span></div>")
            val matcher = pattern.matcher(html)
            if (matcher.find()) cleanHtml(matcher.group(1) ?: "")
            else {
                val ogTitle = Pattern.compile("<meta property=\"og:title\" content=\"(.*?)\">")
                val ogMatcher = ogTitle.matcher(html)
                if (ogMatcher.find()) cleanHtml(ogMatcher.group(1) ?: "") else ""
            }
        } catch (e: Exception) { "" }
    }

    private fun extractDescription(html: String): String {
        return try {
            val pattern = Pattern.compile("<div class=\"tgme_channel_info_description\">(.*?)</div>", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) cleanHtml(matcher.group(1) ?: "") else ""
        } catch (e: Exception) { "" }
    }

    private fun cleanHtml(text: String): String {
        return text.replace("<br/>", "\n").replace("<br>", "\n")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'")
            .replace(Regex("<.*?>"), "").trim()
    }
}
