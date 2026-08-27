package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ScanBanner
import com.example.ui.components.TestBanner
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBgMain
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val configTestProgress by viewModel.configTestProgress.collectAsStateWithLifecycle()
    val proxyTestProgress by viewModel.proxyTestProgress.collectAsStateWithLifecycle()
    val showShadowmere by viewModel.showShadowmere.collectAsStateWithLifecycle()

    val aliveConfigsCount by viewModel.aliveConfigsCount.collectAsStateWithLifecycle()
    val aliveProxiesCount by viewModel.aliveProxiesCount.collectAsStateWithLifecycle()

    // Listen for toasts
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBgMain,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "پروکسی هاب و تستر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "کانفیگ‌های V2Ray و پروکسی تلگرام",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 8.dp),
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(EmeraldAccent, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${aliveConfigsCount + aliveProxiesCount} سالم",
                                color = EmeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.scanAllChannels() },
                        modifier = Modifier.testTag("top_bar_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "دریافت کانفیگ‌ها",
                            tint = CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurfaceCard
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurfaceCard,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.CONFIGS,
                    onClick = { viewModel.setTab(AppTab.CONFIGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.CONFIGS) Icons.Default.Dns else Icons.Outlined.Dns,
                            contentDescription = "کانفیگ‌ها"
                        )
                    },
                    label = { Text("کانفیگ‌ها", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.CONFIGS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBgMain,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_configs")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.PROXIES,
                    onClick = { viewModel.setTab(AppTab.PROXIES) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.PROXIES) Icons.Default.VpnLock else Icons.Outlined.VpnLock,
                            contentDescription = "پروکسی تلگرام"
                        )
                    },
                    label = { Text("پروکسی تلگرام", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.PROXIES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBgMain,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_proxies")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.CHANNELS,
                    onClick = { viewModel.setTab(AppTab.CHANNELS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.CHANNELS) Icons.Default.Send else Icons.Outlined.Send,
                            contentDescription = "کانال‌ها"
                        )
                    },
                    label = { Text("کانال‌ها", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.CHANNELS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBgMain,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_channels")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.TOOLS,
                    onClick = { viewModel.setTab(AppTab.TOOLS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.TOOLS) Icons.Default.Tune else Icons.Outlined.Tune,
                            contentDescription = "ابزارها"
                        )
                    },
                    label = { Text("ابزارها", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.TOOLS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBgMain,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_tools")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Floating Banners for background tasks
            ScanBanner(progress = scanProgress)
            TestBanner(progress = configTestProgress)
            TestBanner(progress = proxyTestProgress)

            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    AppTab.CONFIGS -> ConfigsScreen(viewModel = viewModel)
                    AppTab.PROXIES -> ProxiesScreen(viewModel = viewModel)
                    AppTab.CHANNELS -> {
                        if (showShadowmere) {
                            ShadowmereScreen(
                                onBack = { viewModel.setShowShadowmere(false) },
                                onFetchProxies = { countryCode ->
                                    viewModel.fetchShadowmereByCountry(countryCode)
                                    viewModel.setShowShadowmere(false)
                                }
                            )
                        } else {
                            ChannelsScreen(
                                viewModel = viewModel,
                                onOpenShadowmere = { viewModel.setShowShadowmere(true) }
                            )
                        }
                    }
                    AppTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
