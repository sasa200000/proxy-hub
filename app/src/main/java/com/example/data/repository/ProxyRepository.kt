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
            ChannelEntity(username = "dicodeir", title = "Dicode IR", description = "کانفیگ و پروکسی", isEnabled = true),
            ChannelEntity(username = "persianvpnhub", title = "Persian VPN Hub", description = "هاب VPN فارسی", isEnabled = true),
            ChannelEntity(username = "Free_MTProto_Proxy", title = "Free MTProto Proxy", description = "پروکسی‌های رایگان MTProto", isEnabled = true),
            ChannelEntity(username = "proxyir01", title = "Proxy IR", description = "پروکسی‌های ایرانی", isEnabled = true),
            ChannelEntity(username = "Spotify_Porteghali", title = "Spotify Porteghali", description = "اسپاتیفای پرتقالی", isEnabled = true),
            ChannelEntity(username = "lightning6", title = "Lightning6", description = "کانفیگ‌های لایتنینگ", isEnabled = true),
            ChannelEntity(username = "shaxhabb", title = "Shaxhabb", description = "کانفیگ و پروکسی", isEnabled = true),
            ChannelEntity(username = "meliproxyy", title = "Meli Proxy", description = "پروکسی ملی", isEnabled = true),
            ChannelEntity(username = "ProxyMTProto", title = "Proxy MTProto", description = "پروکسی‌های MTProto تلگرام", isEnabled = true),
            ChannelEntity(username = "LonUp_M", title = "LonUp M", description = "کانفیگ‌های LonUp", isEnabled = true),
            ChannelEntity(username = "sorenab2", title = "Sorena B2", description = "سرونا بی2", isEnabled = true),
            ChannelEntity(username = "ProxyDaemi", title = "Proxy Daemi", description = "پروکسی دایمی", isEnabled = true),
            ChannelEntity(username = "iMTProto", title = "iMTProto", description = "پروکسی‌های iMTProto", isEnabled = true),
            ChannelEntity(username = "v2rayngvpn", title = "V2RayNG VPN", description = "کانفیگ‌های V2RayNG", isEnabled = true),
            ChannelEntity(username = "ConfigX2ray", title = "Config X2ray", description = "کانفیگ‌های X2ray", isEnabled = true),
            ChannelEntity(username = "IraneAzad_Net", title = "Irane Azad Net", description = "ایران آزاد نت", isEnabled = true),
            ChannelEntity(username = "prrofile_purple", title = "Profile Purple", description = "پروفایل بنفش", isEnabled = true),
            ChannelEntity(username = "V2WRAY", title = "V2WRAY", description = "کانفیگ‌های V2WRAY", isEnabled = true),
            ChannelEntity(username = "TelMTProto", title = "Tel MTProto", description = "پروکسی تل MTProto", isEnabled = true),
            ChannelEntity(username = "v2ryNG01", title = "V2RayNG 01", description = "کانفیگ‌های V2RayNG", isEnabled = true),
            ChannelEntity(username = "V2ray_official", title = "V2Ray Official", description = "کانفیگ‌های رسمی V2Ray", isEnabled = true),
            ChannelEntity(username = "TheAnilad", title = "The Anilad", description = "کانفیگ و پروکسی", isEnabled = true),
            ChannelEntity(username = "ProxyDotNet", title = "Proxy DotNet", description = "پروکسی دات‌نت", isEnabled = true),
            ChannelEntity(username = "NPROXY", title = "NPROXY", description = "پروکسی‌های N", isEnabled = true),
            ChannelEntity(username = "mrsoulb", title = "MrSoulb", description = "کانفیگ و پروکسی", isEnabled = true),
            ChannelEntity(username = "ConfigsHUB", title = "Configs HUB", description = "هاب کانفیگ‌ها", isEnabled = true),
            ChannelEntity(username = "orange_vpns", title = "Orange VPNs", description = "وی‌پی‌ان‌های نارنجی", isEnabled = true),
            ChannelEntity(username = "BugFreeNet", title = "Bug Free Net", description = "نت بدون باگ", isEnabled = true),
            ChannelEntity(username = "TeleProxyTele", title = "Tele Proxy Tele", description = "پروکسی تلگرام", isEnabled = true),
            ChannelEntity(username = "iproxy_Meli", title = "iProxy Meli", description = "آی‌پروکسی ملی", isEnabled = true),
            ChannelEntity(username = "SimChin_ir", title = "SimChin IR", description = "سیم‌چین ایران", isEnabled = true),
            ChannelEntity(username = "V2rayEnglish", title = "V2Ray English", description = "کانفیگ‌های انگلیسی V2Ray", isEnabled = true),
            ChannelEntity(username = "v2nova8", title = "V2Nova 8", description = "کانفیگ‌های V2Nova", isEnabled = true),
            ChannelEntity(username = "NetAccount", title = "Net Account", description = "نت اکانت", isEnabled = true),
            ChannelEntity(username = "qpshow", title = "QP Show", description = "کیو‌پی شو", isEnabled = true),
            ChannelEntity(username = "DarkHub_VPN", title = "DarkHub VPN", description = "دارک‌هاب VPN", isEnabled = true),
            ChannelEntity(username = "configmax", title = "Config Max", description = "حداکثر کانفیگ", isEnabled = true),
            ChannelEntity(username = "nufilter", title = "Nu Filter", description = "نو فیلتر", isEnabled = true),
            ChannelEntity(username = "V2RAY_SPATIAL", title = "V2RAY Spatial", description = "فضای V2RAY", isEnabled = true),
            ChannelEntity(username = "shankamil", title = "Shankamil", description = "شانکامیل", isEnabled = true),
            ChannelEntity(username = "PulseStore_ir", title = "PulseStore IR", description = "پالس استور ایران", isEnabled = true),
            ChannelEntity(username = "NETMelliAnti", title = "NET Melli Anti", description = "نت ملی آنتی", isEnabled = true),
            ChannelEntity(username = "Blue_star_Vip", title = "Blue Star VIP", description = "ستاره آبی VIP", isEnabled = true),
            ChannelEntity(username = "Maznet", title = "Maznet", description = "مزنیت", isEnabled = true),
            ChannelEntity(username = "cpy_teeL", title = "CPY TeeL", description = "سی‌پی‌وای تیل", isEnabled = true),
            ChannelEntity(username = "beshcan", title = "Beshcan", description = "بش‌کن", isEnabled = true),
            ChannelEntity(username = "Parsashonam", title = "Parsashonam", description = "پرستشونام", isEnabled = true),
            ChannelEntity(username = "ProxySnipe", title = "Proxy Snipe", description = "پروکسی اسنایپ", isEnabled = true),
            ChannelEntity(username = "Merlin_ViP", title = "Merlin VIP", description = "مرلین VIP", isEnabled = true),
            ChannelEntity(username = "ghalagyann", title = "Ghalagyann", description = "قلعه‌یان", isEnabled = true),
            ChannelEntity(username = "Free_Nettm", title = "Free Net TM", description = "نت رایگان TM", isEnabled = true),
            ChannelEntity(username = "EzAccess1", title = "Ez Access", description = "دسترسی آسان", isEnabled = true),
            ChannelEntity(username = "ChinaPortGFW", title = "China Port GFW", description = "پورت چین GFW", isEnabled = true),
            ChannelEntity(username = "filshekan_vip", title = "Fil Shekan VIP", description = "فیل‌شکن VIP", isEnabled = true)
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

    /**
     * دریافت کانفیگ‌ها از منابع GitHub رایگان (بدون VPN)
     */
    suspend fun fetchFromGitHubSources(
        onProgress: (ScanProgress) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val sources = ChannelFetcher.FREE_GITHUB_SOURCES
        var totalNewConfigs = 0
        var totalNewProxies = 0

        sources.forEachIndexed { index, source ->
            onProgress(
                ScanProgress(
                    isScanning = true,
                    currentChannel = "GitHub: ${source.name}",
                    totalChannels = sources.size,
                    currentChannelIndex = index + 1,
                    configsFound = totalNewConfigs,
                    proxiesFound = totalNewProxies
                )
            )

            val result = ChannelFetcher.fetchGitHubSource(source)
            if (result.isSuccess) {
                if (result.configs.isNotEmpty()) {
                    val inserted = db.configDao().insertConfigs(result.configs)
                    totalNewConfigs += inserted.count { it != -1L }
                }
                if (result.proxies.isNotEmpty()) {
                    val inserted = db.proxyDao().insertProxies(result.proxies)
                    totalNewProxies += inserted.count { it != -1L }
                }
            }
        }

        onProgress(
            ScanProgress(
                isScanning = false,
                currentChannel = "",
                totalChannels = sources.size,
                currentChannelIndex = sources.size,
                configsFound = totalNewConfigs,
                proxiesFound = totalNewProxies
            )
        )

        Pair(totalNewConfigs, totalNewProxies)
    }
}
