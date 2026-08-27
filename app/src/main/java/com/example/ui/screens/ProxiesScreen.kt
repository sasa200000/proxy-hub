package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FilterStatus
import com.example.data.model.ProxyEntity
import com.example.ui.components.PingBadge
import com.example.ui.components.ProxyTypeBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBgMain
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedDead
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProxiesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val proxies by viewModel.filteredProxies.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalProxiesCount.collectAsStateWithLifecycle()
    val aliveCount by viewModel.aliveProxiesCount.collectAsStateWithLifecycle()
    val deadCount by viewModel.deadProxiesCount.collectAsStateWithLifecycle()

    val searchQuery by viewModel.proxySearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.proxyFilterStatus.collectAsStateWithLifecycle()
    val testProgress by viewModel.proxyTestProgress.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgMain)
    ) {
        // Stats Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "پروکسی‌های تلگرام",
                count = totalCount,
                icon = Icons.Default.VpnLock,
                accentColor = CyanPrimary,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setProxyFilterStatus(FilterStatus.ALL) }
            )
            StatCard(
                title = "آنلاین و متصل",
                count = aliveCount,
                icon = Icons.Default.CheckCircle,
                accentColor = EmeraldAccent,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setProxyFilterStatus(FilterStatus.ALIVE_ONLY) }
            )
            StatCard(
                title = "قطع / آفلاین",
                count = deadCount,
                icon = Icons.Default.Delete,
                accentColor = RedDead,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setProxyFilterStatus(FilterStatus.DEAD_ONLY) }
            )
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.testAllProxies() },
                enabled = !testProgress.isTesting && totalCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBgMain),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("test_all_proxies_button")
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (testProgress.isTesting) "در حال تست..." else "تست پینگ پروکسی‌ها", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.deleteDeadProxies() },
                enabled = deadCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDead),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedDead.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("delete_dead_proxies_button")
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حذف آفلاین‌ها ($deadCount)")
            }

            OutlinedButton(
                onClick = { viewModel.copyAllAliveProxies(context) },
                enabled = aliveCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("copy_alive_proxies_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("کپی همه پروکسی‌های سالم ($aliveCount)")
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setProxySearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("proxy_search_input"),
            placeholder = { Text("جستجوی آدرس، پورت یا کانال تلگرام...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setProxySearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceCard,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                Pair(FilterStatus.ALL, "همه ($totalCount)"),
                Pair(FilterStatus.ALIVE_ONLY, "✅ آنلاین و متصل ($aliveCount)"),
                Pair(FilterStatus.FAST_ONLY, "⚡ سریع‌ترین (<400ms)"),
                Pair(FilterStatus.UNTESTED, "⏳ تست نشده"),
                Pair(FilterStatus.DEAD_ONLY, "❌ قطع / آفلاین ($deadCount)"),
                Pair(FilterStatus.FAVORITES, "⭐ برگزیده‌ها")
            )

            filters.forEach { (status, title) ->
                val selected = filterStatus == status
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setProxyFilterStatus(status) },
                    label = { Text(title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) CyanPrimary else DarkBorder,
                        selectedBorderColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Proxies List
        if (proxies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (totalCount == 0) "هیچ پروکسی تلگرامی موجود نیست" else "پروکسی با این فیلتر یافت نشد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "می‌توانید کانال‌های پروکسی را در تب «کانال‌ها» بروزرسانی کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("proxies_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(proxies, key = { it.id }) { proxy ->
                    ProxyItemCard(
                        proxy = proxy,
                        onConnect = { viewModel.openInTelegram(context, proxy.rawUri) },
                        onTest = { viewModel.testSingleProxy(proxy) },
                        onCopy = { viewModel.copyToClipboard(context, proxy.rawUri, "MTProto Proxy") },
                        onToggleFavorite = { viewModel.toggleFavoriteProxy(proxy.id) },
                        onDelete = { viewModel.deleteProxy(proxy.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProxyItemCard(
    proxy: ProxyEntity,
    onConnect: () -> Unit,
    onTest: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("proxy_card_${proxy.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (proxy.isAlive == true) EmeraldAccent.copy(alpha = 0.35f) else DarkBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: MTProto Badge, Source Channel, Favorite, Ping Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProxyTypeBadge(type = proxy.type)
                    if (proxy.sourceChannel.isNotBlank()) {
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "@${proxy.sourceChannel}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (proxy.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "علاقه‌مندی",
                            tint = if (proxy.isFavorite) AmberWarning else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    PingBadge(
                        pingMs = proxy.pingMs,
                        isAlive = proxy.isAlive,
                        onClick = onTest
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Server Address & Port Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${proxy.server}:${proxy.port}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Secret preview (if available)
            if (proxy.secret.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Secret: ${proxy.secret.take(16)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row: Connect in Telegram (Big Button) + Copy + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("connect_tg_btn_${proxy.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = DarkBgMain
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال در تلگرام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .size(40.dp)
                        .testTag("copy_proxy_btn_${proxy.id}")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "کپی لینک", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .size(40.dp)
                        .testTag("delete_proxy_btn_${proxy.id}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = RedDead.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
