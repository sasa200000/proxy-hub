package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.fetcher.ChannelFetcher
import com.example.data.model.ChannelEntity
import com.example.data.model.ConfigEntity
import com.example.data.model.ProxyEntity
import com.example.data.model.ScanProgress
import com.example.data.model.TestProgress
import com.example.data.parser.ConfigParser
import com.example.data.tester.PingTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class ProxyRepository(private val db: AppDatabase) {

    val allConfigs: Flow<List<ConfigEntity>> = db.configDao().getAllConfigs()
    val allProxies: Flow<List<ProxyEntity>> = db.proxyDao().getAllProxies()
    val allChannels: Flow<List<ChannelEntity>> = db.channelDao().getAllChannels()

    val totalConfigsCount: Flow<Int> = db.configDao().getTotalCount()
    val aliveConfigsCount: Flow<Int> = db.configDao().getAliveCount()
    val deadConfigsCount: Flow<Int> = db.configDao().getDeadCount()

    val totalProxiesCount: Flow<Int> = db.proxyDao().getTotalCount()
    val aliveProxiesCount: Flow<Int> = db.proxyDao().getAliveCount()
    val deadProxiesCount: Flow<Int> = db.proxyDao().getDeadCount()

    /**
     * Prepopulates initial recommended channels if database is empty.
     */
    suspend fun initDefaultChannelsIfNeeded() = withContext(Dispatchers.IO) {
        val defaultChannels = listOf(
            ChannelEntity(
                username = "v2rayng_org",
                title = "V2Ray Configs Hub",
                description = "کانفیگ‌های فعال V2Ray، VLESS، VMess و Trojan",
                isEnabled = true
            ),
            ChannelEntity(
                username = "ProxyMTProto",
                title = "MTProto Proxies",
                description = "پروکسی‌های پرسرعت و فعال تلگرام ضد فیلتر",
                isEnabled = true
            ),
            ChannelEntity(
                username = "FalconProxy",
                title = "Falcon Telegram Proxy",
                description = "پروکسی‌های پینگ پایین و اختصاصی تلگرام",
                isEnabled = true
            ),
            ChannelEntity(
                username = "v2ray_configs_pool",
                title = "V2Ray Configs Pool",
                description = "کانفیگ‌های روزانه و تست شده vless/vmess",
                isEnabled = true
            ),
            ChannelEntity(
                username = "FreeV2rays",
                title = "Free V2Ray Channel",
                description = "سرورهای رایگان با اتصال مستقیم و کلودفلر",
                isEnabled = false
            )
        )
        db.channelDao().insertChannels(defaultChannels)
    }

    // Channels operations
    suspend fun addChannel(usernameInput: String): ChannelFetcher.FetchResult = withContext(Dispatchers.IO) {
        val fetchResult = ChannelFetcher.fetchChannel(usernameInput)
        val cleanUser = if (fetchResult.username.isNotBlank()) fetchResult.username else ChannelFetcher.sanitizeChannelInput(usernameInput)

        val channel = ChannelEntity(
            username = cleanUser,
            title = fetchResult.title.ifBlank { "@$cleanUser" },
            description = fetchResult.description,
            isEnabled = true,
            lastFetchTime = if (fetchResult.isSuccess) System.currentTimeMillis() else 0,
            fetchedConfigCount = fetchResult.configs.size,
            fetchedProxyCount = fetchResult.proxies.size
        )
        db.channelDao().insertChannel(channel)

        if (fetchResult.configs.isNotEmpty()) {
            db.configDao().insertConfigs(fetchResult.configs)
        }
        if (fetchResult.proxies.isNotEmpty()) {
            db.proxyDao().insertProxies(fetchResult.proxies)
        }

        fetchResult
    }

    suspend fun deleteChannel(id: Long) = withContext(Dispatchers.IO) {
        db.channelDao().deleteChannelById(id)
    }

    suspend fun toggleChannel(id: Long) = withContext(Dispatchers.IO) {
        db.channelDao().toggleChannel(id)
    }

    // Fetch from all enabled channels
    suspend fun scanChannels(
        onProgress: (ScanProgress) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val enabledChannels = db.channelDao().getEnabledChannels()
        if (enabledChannels.isEmpty()) return@withContext Pair(0, 0)

        var totalNewConfigs = 0
        var totalNewProxies = 0

        enabledChannels.forEachIndexed { index, channel ->
            onProgress(
                ScanProgress(
                    isScanning = true,
                    currentChannel = channel.username,
                    totalChannels = enabledChannels.size,
                    currentChannelIndex = index + 1,
                    configsFound = totalNewConfigs,
                    proxiesFound = totalNewProxies
                )
            )

            val result = ChannelFetcher.fetchChannel(channel.username)
            if (result.isSuccess) {
                if (result.configs.isNotEmpty()) {
                    val inserted = db.configDao().insertConfigs(result.configs)
                    totalNewConfigs += inserted.count { it != -1L }
                }
                if (result.proxies.isNotEmpty()) {
                    val inserted = db.proxyDao().insertProxies(result.proxies)
                    totalNewProxies += inserted.count { it != -1L }
                }
                db.channelDao().updateFetchStats(
                    id = channel.id,
                    time = System.currentTimeMillis(),
                    configs = result.configs.size,
                    proxies = result.proxies.size
                )
            }
        }

        onProgress(
            ScanProgress(
                isScanning = false,
                currentChannel = "",
                totalChannels = enabledChannels.size,
                currentChannelIndex = enabledChannels.size,
                configsFound = totalNewConfigs,
                proxiesFound = totalNewProxies
            )
        )

        Pair(totalNewConfigs, totalNewProxies)
    }

    // Config operations
    suspend fun testSingleConfig(config: ConfigEntity, timeoutMs: Int = 2500): ConfigEntity = withContext(Dispatchers.IO) {
        val ping = PingTester.testTcpPing(config.server, config.port, timeoutMs)
        val now = System.currentTimeMillis()
        db.configDao().updatePing(config.id, ping.pingMs, ping.isAlive, now)
        config.copy(pingMs = ping.pingMs, isAlive = ping.isAlive, lastTestedAt = now)
    }

    suspend fun testAllConfigs(
        configs: List<ConfigEntity>,
        timeoutMs: Int = 2500,
        concurrency: Int = 12,
        onProgress: (TestProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (configs.isEmpty()) return@withContext

        val semaphore = Semaphore(concurrency)
        var tested = 0
        var alive = 0
        var dead = 0

        onProgress(
            TestProgress(
                isTesting = true,
                totalCount = configs.size,
                testedCount = 0,
                aliveCount = 0,
                deadCount = 0,
                currentItemName = "شروع تست..."
            )
        )

        coroutineScope {
            val tasks = configs.map { config ->
                async {
                    semaphore.withPermit {
                        val result = PingTester.testTcpPing(config.server, config.port, timeoutMs)
                        val now = System.currentTimeMillis()
                        db.configDao().updatePing(config.id, result.pingMs, result.isAlive, now)

                        synchronized(this@ProxyRepository) {
                            tested++
                            if (result.isAlive) alive++ else dead++
                            onProgress(
                                TestProgress(
                                    isTesting = true,
                                    totalCount = configs.size,
                                    testedCount = tested,
                                    aliveCount = alive,
                                    deadCount = dead,
                                    currentItemName = config.remark
                                )
                            )
                        }
                    }
                }
            }
            tasks.awaitAll()
        }

        onProgress(
            TestProgress(
                isTesting = false,
                totalCount = configs.size,
                testedCount = tested,
                aliveCount = alive,
                deadCount = dead,
                currentItemName = "تست پایان یافت"
            )
        )
    }

    suspend fun deleteDeadConfigs(): Int = withContext(Dispatchers.IO) {
        db.configDao().deleteDeadConfigs()
    }

    suspend fun deleteConfig(id: Long) = withContext(Dispatchers.IO) {
        db.configDao().deleteConfigById(id)
    }

    suspend fun deleteAllConfigs() = withContext(Dispatchers.IO) {
        db.configDao().deleteAllConfigs()
    }

    suspend fun toggleFavoriteConfig(id: Long) = withContext(Dispatchers.IO) {
        db.configDao().toggleFavorite(id)
    }

    // Proxies operations
    suspend fun testSingleProxy(proxy: ProxyEntity, timeoutMs: Int = 2500): ProxyEntity = withContext(Dispatchers.IO) {
        val ping = PingTester.testTcpPing(proxy.server, proxy.port, timeoutMs)
        val now = System.currentTimeMillis()
        db.proxyDao().updatePing(proxy.id, ping.pingMs, ping.isAlive, now)
        proxy.copy(pingMs = ping.pingMs, isAlive = ping.isAlive, lastTestedAt = now)
    }

    suspend fun testAllProxies(
        proxies: List<ProxyEntity>,
        timeoutMs: Int = 2500,
        concurrency: Int = 12,
        onProgress: (TestProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (proxies.isEmpty()) return@withContext

        val semaphore = Semaphore(concurrency)
        var tested = 0
        var alive = 0
        var dead = 0

        onProgress(
            TestProgress(
                isTesting = true,
                totalCount = proxies.size,
                testedCount = 0,
                aliveCount = 0,
                deadCount = 0,
                currentItemName = "شروع تست پروکسی‌ها..."
            )
        )

        coroutineScope {
            val tasks = proxies.map { proxy ->
                async {
                    semaphore.withPermit {
                        val result = PingTester.testTcpPing(proxy.server, proxy.port, timeoutMs)
                        val now = System.currentTimeMillis()
                        db.proxyDao().updatePing(proxy.id, result.pingMs, result.isAlive, now)

                        synchronized(this@ProxyRepository) {
                            tested++
                            if (result.isAlive) alive++ else dead++
                            onProgress(
                                TestProgress(
                                    isTesting = true,
                                    totalCount = proxies.size,
                                    testedCount = tested,
                                    aliveCount = alive,
                                    deadCount = dead,
                                    currentItemName = "${proxy.server}:${proxy.port}"
                                )
                            )
                        }
                    }
                }
            }
            tasks.awaitAll()
        }

        onProgress(
            TestProgress(
                isTesting = false,
                totalCount = proxies.size,
                testedCount = tested,
                aliveCount = alive,
                deadCount = dead,
                currentItemName = "تست پروکسی‌ها پایان یافت"
            )
        )
    }

    suspend fun deleteDeadProxies(): Int = withContext(Dispatchers.IO) {
        db.proxyDao().deleteDeadProxies()
    }

    suspend fun deleteProxy(id: Long) = withContext(Dispatchers.IO) {
        db.proxyDao().deleteProxyById(id)
    }

    suspend fun deleteAllProxies() = withContext(Dispatchers.IO) {
        db.proxyDao().deleteAllProxies()
    }

    suspend fun toggleFavoriteProxy(id: Long) = withContext(Dispatchers.IO) {
        db.proxyDao().toggleFavorite(id)
    }

    // Manual Extractor
    suspend fun extractAndSaveRawText(rawText: String, sourceLabel: String = "Manual"): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val result = ConfigParser.extractAll(rawText, sourceChannel = sourceLabel)
        var addedConfigs = 0
        var addedProxies = 0

        if (result.configs.isNotEmpty()) {
            val inserted = db.configDao().insertConfigs(result.configs)
            addedConfigs = inserted.count { it != -1L }
        }
        if (result.proxies.isNotEmpty()) {
            val inserted = db.proxyDao().insertProxies(result.proxies)
            addedProxies = inserted.count { it != -1L }
        }

        Pair(addedConfigs, addedProxies)
    }
}
