package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.ConfigEntity
import com.example.data.model.FilterStatus
import com.example.data.model.ProtocolType
import com.example.data.model.ProxyEntity
import com.example.data.model.ProxyType
import com.example.data.model.ScanProgress
import com.example.data.model.TestProgress
import com.example.data.repository.ProxyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

enum class AppTab {
    CONFIGS,
    PROXIES,
    CHANNELS,
    TOOLS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "proxy_hub.db"
    ).build()

    private val repository = ProxyRepository(db)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.CONFIGS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Configs Filter & Search
    private val _configSearchQuery = MutableStateFlow("")
    val configSearchQuery: StateFlow<String> = _configSearchQuery.asStateFlow()

    private val _configFilterStatus = MutableStateFlow(FilterStatus.ALL)
    val configFilterStatus: StateFlow<FilterStatus> = _configFilterStatus.asStateFlow()

    private val _configProtocolFilter = MutableStateFlow<ProtocolType?>(null)
    val configProtocolFilter: StateFlow<ProtocolType?> = _configProtocolFilter.asStateFlow()

    // Proxies Filter & Search
    private val _proxySearchQuery = MutableStateFlow("")
    val proxySearchQuery: StateFlow<String> = _proxySearchQuery.asStateFlow()

    private val _proxyFilterStatus = MutableStateFlow(FilterStatus.ALL)
    val proxyFilterStatus: StateFlow<FilterStatus> = _proxyFilterStatus.asStateFlow()

    // Progress States
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _configTestProgress = MutableStateFlow(TestProgress())
    val configTestProgress: StateFlow<TestProgress> = _configTestProgress.asStateFlow()

    private val _proxyTestProgress = MutableStateFlow(TestProgress())
    val proxyTestProgress: StateFlow<TestProgress> = _proxyTestProgress.asStateFlow()

    // Settings
    private val _pingTimeoutMs = MutableStateFlow(2500)
    val pingTimeoutMs: StateFlow<Int> = _pingTimeoutMs.asStateFlow()

    private val _concurrency = MutableStateFlow(12)
    val concurrency: StateFlow<Int> = _concurrency.asStateFlow()

    // Snackbar / Toast events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Raw Data Flows
    val rawConfigs = repository.allConfigs
    val rawProxies = repository.allProxies
    val allChannels = repository.allChannels.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered Configs List
    val filteredConfigs: StateFlow<List<ConfigEntity>> = combine(
        rawConfigs,
        _configSearchQuery,
        _configFilterStatus,
        _configProtocolFilter
    ) { configs, query, status, protocol ->
        configs.filter { config ->
            val matchesQuery = query.isBlank() ||
                    config.remark.contains(query, ignoreCase = true) ||
                    config.server.contains(query, ignoreCase = true) ||
                    config.sourceChannel.contains(query, ignoreCase = true)

            val matchesProtocol = protocol == null || config.protocol == protocol

            val matchesStatus = when (status) {
                FilterStatus.ALL -> true
                FilterStatus.ALIVE_ONLY -> config.isAlive == true
                FilterStatus.FAST_ONLY -> config.isAlive == true && config.pingMs in 1..400
                FilterStatus.UNTESTED -> config.isAlive == null
                FilterStatus.DEAD_ONLY -> config.isAlive == false || config.pingMs == -2L
                FilterStatus.FAVORITES -> config.isFavorite
            }

            matchesQuery && matchesProtocol && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Proxies List
    val filteredProxies: StateFlow<List<ProxyEntity>> = combine(
        rawProxies,
        _proxySearchQuery,
        _proxyFilterStatus
    ) { proxies, query, status ->
        proxies.filter { proxy ->
            val matchesQuery = query.isBlank() ||
                    proxy.server.contains(query, ignoreCase = true) ||
                    proxy.sourceChannel.contains(query, ignoreCase = true) ||
                    proxy.port.toString().contains(query)

            val matchesStatus = when (status) {
                FilterStatus.ALL -> true
                FilterStatus.ALIVE_ONLY -> proxy.isAlive == true
                FilterStatus.FAST_ONLY -> proxy.isAlive == true && proxy.pingMs in 1..400
                FilterStatus.UNTESTED -> proxy.isAlive == null
                FilterStatus.DEAD_ONLY -> proxy.isAlive == false || proxy.pingMs == -2L
                FilterStatus.FAVORITES -> proxy.isFavorite
            }

            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stat Counters
    val totalConfigsCount = repository.totalConfigsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val aliveConfigsCount = repository.aliveConfigsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val deadConfigsCount = repository.deadConfigsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProxiesCount = repository.totalProxiesCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val aliveProxiesCount = repository.aliveProxiesCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val deadProxiesCount = repository.deadProxiesCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.initDefaultChannelsIfNeeded()
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setConfigSearchQuery(query: String) {
        _configSearchQuery.value = query
    }

    fun setConfigFilterStatus(status: FilterStatus) {
        _configFilterStatus.value = status
    }

    fun setConfigProtocolFilter(protocol: ProtocolType?) {
        _configProtocolFilter.value = protocol
    }

    fun setProxySearchQuery(query: String) {
        _proxySearchQuery.value = query
    }

    fun setProxyFilterStatus(status: FilterStatus) {
        _proxyFilterStatus.value = status
    }

    fun setPingTimeout(ms: Int) {
        _pingTimeoutMs.value = ms
    }

    fun setConcurrency(count: Int) {
        _concurrency.value = count
    }

    // Channel Actions
    fun scanAllChannels() {
        if (_scanProgress.value.isScanning) return
        viewModelScope.launch {
            val result = repository.scanChannels { progress ->
                _scanProgress.value = progress
            }
            _toastEvent.emit("اسکن انجام شد: ${result.first} کانفیگ و ${result.second} پروکسی جدید اضافه شد")
        }
    }

    fun addChannel(usernameInput: String) {
        viewModelScope.launch {
            val result = repository.addChannel(usernameInput)
            if (result.isSuccess) {
                _toastEvent.emit("کانال @${result.username} افزوده شد (${result.configs.size} کانفیگ، ${result.proxies.size} پروکسی)")
            } else {
                _toastEvent.emit("خطا در افزودن کانال: ${result.error}")
            }
        }
    }

    fun deleteChannel(id: Long) {
        viewModelScope.launch {
            repository.deleteChannel(id)
            _toastEvent.emit("کانال حذف شد")
        }
    }

    fun toggleChannel(id: Long) {
        viewModelScope.launch {
            repository.toggleChannel(id)
        }
    }

    // Config Actions
    fun testAllConfigs() {
        if (_configTestProgress.value.isTesting) return
        viewModelScope.launch {
            val currentConfigs = filteredConfigs.value.ifEmpty { rawConfigs.firstOrNull() ?: emptyList() }
            if (currentConfigs.isEmpty()) return@launch
            repository.testAllConfigs(
                configs = currentConfigs,
                timeoutMs = _pingTimeoutMs.value,
                concurrency = _concurrency.value,
                onProgress = { _configTestProgress.value = it }
            )
            _toastEvent.emit("تست کانفیگ‌ها با موفقیت پایان یافت")
        }
    }

    fun testSingleConfig(config: ConfigEntity) {
        viewModelScope.launch {
            val updated = repository.testSingleConfig(config, _pingTimeoutMs.value)
            val msg = if (updated.isAlive == true) "پینگ: ${updated.pingMs} میلی‌ثانیه" else "کانفیگ پاسخ نداد (قطع)"
            _toastEvent.emit(msg)
        }
    }

    fun deleteDeadConfigs() {
        viewModelScope.launch {
            val deleted = repository.deleteDeadConfigs()
            _toastEvent.emit("$deleted کانفیگ غیرفعال با موفقیت حذف شد")
        }
    }

    fun deleteConfig(id: Long) {
        viewModelScope.launch {
            repository.deleteConfig(id)
            _toastEvent.emit("کانفیگ حذف شد")
        }
    }

    fun deleteAllConfigs() {
        viewModelScope.launch {
            repository.deleteAllConfigs()
            _toastEvent.emit("همه کانفیگ‌ها پاکسازی شدند")
        }
    }

    fun toggleFavoriteConfig(id: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteConfig(id)
        }
    }

    // Proxy Actions
    fun testAllProxies() {
        if (_proxyTestProgress.value.isTesting) return
        viewModelScope.launch {
            val currentProxies = filteredProxies.value.ifEmpty { rawProxies.firstOrNull() ?: emptyList() }
            if (currentProxies.isEmpty()) return@launch
            repository.testAllProxies(
                proxies = currentProxies,
                timeoutMs = _pingTimeoutMs.value,
                concurrency = _concurrency.value,
                onProgress = { _proxyTestProgress.value = it }
            )
            _toastEvent.emit("تست پروکسی‌ها با موفقیت پایان یافت")
        }
    }

    fun testSingleProxy(proxy: ProxyEntity) {
        viewModelScope.launch {
            val updated = repository.testSingleProxy(proxy, _pingTimeoutMs.value)
            val msg = if (updated.isAlive == true) "پینگ پروکسی: ${updated.pingMs} ms" else "پروکسی قطع است"
            _toastEvent.emit(msg)
        }
    }

    fun deleteDeadProxies() {
        viewModelScope.launch {
            val deleted = repository.deleteDeadProxies()
            _toastEvent.emit("$deleted پروکسی غیرفعال با موفقیت حذف شد")
        }
    }

    fun deleteProxy(id: Long) {
        viewModelScope.launch {
            repository.deleteProxy(id)
            _toastEvent.emit("پروکسی حذف شد")
        }
    }

    fun deleteAllProxies() {
        viewModelScope.launch {
            repository.deleteAllProxies()
            _toastEvent.emit("همه پروکسی‌ها پاکسازی شدند")
        }
    }

    fun toggleFavoriteProxy(id: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteProxy(id)
        }
    }

    // Extractor
    fun extractAndSaveFromText(rawText: String) {
        viewModelScope.launch {
            val result = repository.extractAndSaveRawText(rawText, "استخراج دستی")
            _toastEvent.emit("استخراج شد: ${result.first} کانفیگ و ${result.second} پروکسی")
        }
    }

    // Utilities (Clipboard, Intents, Exports)
    fun copyToClipboard(context: Context, text: String, label: String = "ProxyHub") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "کپی شد در حافظه کلیپ‌بورد", Toast.LENGTH_SHORT).show()
    }

    fun copyAllAliveConfigs(context: Context) {
        val alive = filteredConfigs.value.filter { it.isAlive == true }
        if (alive.isEmpty()) {
            Toast.makeText(context, "هیچ کانفیگ سالمی در این لیست یافت نشد", Toast.LENGTH_SHORT).show()
            return
        }
        val joined = alive.joinToString("\n") { it.rawUri }
        copyToClipboard(context, joined, "Alive Configs")
        Toast.makeText(context, "${alive.size} کانفیگ سالم کپی شد", Toast.LENGTH_SHORT).show()
    }

    fun copyAllAliveProxies(context: Context) {
        val alive = filteredProxies.value.filter { it.isAlive == true }
        if (alive.isEmpty()) {
            Toast.makeText(context, "هیچ پروکسی سالمی در این لیست یافت نشد", Toast.LENGTH_SHORT).show()
            return
        }
        val joined = alive.joinToString("\n") { it.rawUri }
        copyToClipboard(context, joined, "Alive Proxies")
        Toast.makeText(context, "${alive.size} پروکسی سالم کپی شد", Toast.LENGTH_SHORT).show()
    }

    fun exportBase64Subscription(context: Context) {
        val alive = filteredConfigs.value.filter { it.isAlive == true }
        if (alive.isEmpty()) {
            Toast.makeText(context, "کانفیگ سالمی برای تولید سابسکریپشن وجود ندارد", Toast.LENGTH_SHORT).show()
            return
        }
        val joined = alive.joinToString("\n") { it.rawUri }
        val base64 = Base64.encodeToString(joined.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        copyToClipboard(context, base64, "V2Ray Subscription")
        Toast.makeText(context, "کد سابسکریپشن Base64 کپی شد (${alive.size} سرور)", Toast.LENGTH_SHORT).show()
    }

    fun openInTelegram(context: Context, proxyUri: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(proxyUri))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تلگرام بر روی دستگاه یافت نشد. لینک کپی شد", Toast.LENGTH_SHORT).show()
            copyToClipboard(context, proxyUri, "Telegram Proxy")
        }
    }

    private fun <T> StateFlow<T>.valueOrNull(): T? = try { value } catch (e: Exception) { null }
}
