package com.example.data.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ShadowmereFetcher {

    private const val BASE_URL = "https://shadowmere.xyz"

    data class ShadowmereProxy(
        val id: Long,
        val url: String,
        val location: String,
        val countryCode: String,
        val country: String,
        val ipAddress: String,
        val isActive: Boolean,
        val port: Int,
        val lastChecked: String,
        val successRate: Float
    )

    data class CountryInfo(
        val code: String,
        val name: String,
        val proxyCount: Int = 0
    )

    data class ShadowmereResult(
        val proxies: List<ShadowmereProxy>,
        val totalCount: Int,
        val countries: List<CountryInfo>,
        val isSuccess: Boolean,
        val error: String? = null
    )

    private fun buildClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /**
     * دریافت لیست تمام کشورها از API
     */
    suspend fun fetchCountryCodes(): List<CountryInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/country-codes/")
                .header("Accept", "application/json")
                .build()

            val client = buildClient()
            val response = client.newCall(request).execute()

            response.use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()

                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                val countries = mutableListOf<CountryInfo>()

                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    countries.add(
                        CountryInfo(
                            code = item.optString("code"),
                            name = item.optString("name")
                        )
                    )
                }

                countries
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * دریافت تعداد پروکسی‌های فعال هر کشور
     */
    suspend fun fetchProxyCountByCountry(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val result = fetchProxiesInternal(pageSize = 1000)
            result.groupBy { it.countryCode }.mapValues { it.value.size }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * دریافت لیست پروکسی‌های فعال از shadowmere.xyz
     * @param countryCode کد کشور (مثلاً DE, US, NL) - خالی برای همه
     * @param page شماره صفحه
     * @param pageSize تعداد در هر صفحه
     */
    suspend fun fetchProxies(
        countryCode: String = "",
        page: Int = 1,
        pageSize: Int = 200
    ): ShadowmereResult = withContext(Dispatchers.IO) {
        try {
            val allProxies = fetchProxiesInternal(countryCode, page, pageSize)

            // Group by country to get country list
            val countryGroups = allProxies.groupBy { it.countryCode }
            val countries = countryGroups.map { (code, proxies) ->
                val name = proxies.firstOrNull()?.country ?: code
                CountryInfo(code = code, name = name, proxyCount = proxies.size)
            }.sortedBy { it.name }

            ShadowmereResult(
                proxies = allProxies,
                totalCount = allProxies.size,
                countries = countries,
                isSuccess = true
            )
        } catch (e: Exception) {
            ShadowmereResult(
                proxies = emptyList(), totalCount = 0,
                countries = emptyList(), isSuccess = false,
                error = e.localizedMessage ?: "خطا"
            )
        }
    }

    private fun fetchProxiesInternal(
        countryCode: String = "",
        page: Int = 1,
        pageSize: Int = 200
    ): List<ShadowmereProxy> {
        var url = "$BASE_URL/api/proxies/?page=$page&page_size=$pageSize&is_active=true"
        if (countryCode.isNotBlank()) {
            url += "&location_country_code=$countryCode"
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        val client = buildClient()
        val response = client.newCall(request).execute()

        response.use { resp ->
            if (!resp.isSuccessful) return emptyList()

            val body = resp.body?.string() ?: ""
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: JSONArray()

            val proxies = mutableListOf<ShadowmereProxy>()

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val successRate = if (item.optInt("times_checked", 0) > 0) {
                    item.optInt("times_check_succeeded", 0).toFloat() / item.optInt("times_checked", 1).toFloat()
                } else 0f

                proxies.add(
                    ShadowmereProxy(
                        id = item.optLong("id"),
                        url = item.optString("url"),
                        location = item.optString("location"),
                        countryCode = item.optString("location_country_code"),
                        country = item.optString("location_country"),
                        ipAddress = item.optString("ip_address"),
                        isActive = item.optBoolean("is_active"),
                        port = item.optInt("port"),
                        lastChecked = item.optString("last_checked"),
                        successRate = successRate
                    )
                )
            }

            return proxies
        }
    }

    /**
     * دریافت لینک سابسکریپشن Shadowsocks
     */
    suspend fun fetchSubscription(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/sub/")
                .header("Accept", "text/plain")
                .build()

            val client = buildClient()
            val response = client.newCall(request).execute()

            response.use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string() ?: ""
                } else ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
