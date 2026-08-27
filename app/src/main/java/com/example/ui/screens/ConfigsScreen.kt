package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConfigEntity
import com.example.data.model.FilterStatus
import com.example.data.model.ProtocolType
import com.example.ui.components.PingBadge
import com.example.ui.components.ProtocolBadge
import com.example.ui.components.QrCodeDialog
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBgMain
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedDead
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ConfigsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configs by viewModel.filteredConfigs.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalConfigsCount.collectAsStateWithLifecycle()
    val aliveCount by viewModel.aliveConfigsCount.collectAsStateWithLifecycle()
    val deadCount by viewModel.deadConfigsCount.collectAsStateWithLifecycle()

    val searchQuery by viewModel.configSearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.configFilterStatus.collectAsStateWithLifecycle()
    val protocolFilter by viewModel.configProtocolFilter.collectAsStateWithLifecycle()
    val testProgress by viewModel.configTestProgress.collectAsStateWithLifecycle()

    var qrDialogConfig by remember { mutableStateOf<ConfigEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgMain)
    ) {
        // Quick Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "کل کانفیگ‌ها",
                count = totalCount,
                icon = Icons.Default.Dns,
                accentColor = CyanPrimary,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setConfigFilterStatus(FilterStatus.ALL) }
            )
            StatCard(
                title = "سالم و فعال",
                count = aliveCount,
                icon = Icons.Default.CheckCircle,
                accentColor = EmeraldAccent,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setConfigFilterStatus(FilterStatus.ALIVE_ONLY) }
            )
            StatCard(
                title = "خراب / قطع",
                count = deadCount,
                icon = Icons.Default.Delete,
                accentColor = RedDead,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setConfigFilterStatus(FilterStatus.DEAD_ONLY) }
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
                onClick = { viewModel.testAllConfigs() },
                enabled = !testProgress.isTesting && totalCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBgMain),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("test_all_configs_button")
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (testProgress.isTesting) "در حال تست..." else "تست پینگ همه", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.deleteDeadConfigs() },
                enabled = deadCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDead),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedDead.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("delete_dead_configs_button")
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حذف غیرسالم‌ها ($deadCount)")
            }

            OutlinedButton(
                onClick = { viewModel.copyAllAliveConfigs(context) },
                enabled = aliveCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("copy_alive_configs_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("کپی سالم‌ها ($aliveCount)")
            }

            OutlinedButton(
                onClick = { viewModel.exportBase64Subscription(context) },
                enabled = aliveCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("export_sub_button")
            ) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("لینک سابسکریپشن")
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setConfigSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("config_search_input"),
            placeholder = { Text("جستجو بر اساس نام، آدرس سرور، یا کانال...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setConfigSearchQuery("") }) {
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
                Pair(FilterStatus.ALIVE_ONLY, "✅ فقط سالم‌ها ($aliveCount)"),
                Pair(FilterStatus.FAST_ONLY, "⚡ پرسرعت (<400ms)"),
                Pair(FilterStatus.UNTESTED, "⏳ تست نشده"),
                Pair(FilterStatus.DEAD_ONLY, "❌ خراب / قطع ($deadCount)"),
                Pair(FilterStatus.FAVORITES, "⭐ نشان‌شده‌ها")
            )

            filters.forEach { (status, title) ->
                val selected = filterStatus == status
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setConfigFilterStatus(status) },
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

        // Protocol Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val protocols = listOf(
                Pair(null, "همه پروتکل‌ها"),
                Pair(ProtocolType.VLESS, "VLESS"),
                Pair(ProtocolType.VMESS, "VMess"),
                Pair(ProtocolType.TROJAN, "Trojan"),
                Pair(ProtocolType.SHADOWSOCKS, "Shadowsocks"),
                Pair(ProtocolType.HYSTERIA2, "Hysteria 2"),
                Pair(ProtocolType.TUIC, "TUIC"),
                Pair(ProtocolType.WIREGUARD, "WireGuard")
            )

            protocols.forEach { (proto, label) ->
                val selected = protocolFilter == proto
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setConfigProtocolFilter(proto) },
                    color = if (selected) PurpleAccent.copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) PurpleAccent else DarkBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) PurpleAccent else TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Configs List
        if (configs.isEmpty()) {
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
                        text = if (totalCount == 0) "هنوز هیچ کانفیگی اضافه نشده است" else "هیچ کانفیگی با فیلترهای انتخابی مطابقت ندارد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (totalCount == 0) "از تب «کانال‌ها» روی اسکن کانال‌ها بزنید یا از تب «ابزارها» متن را وارد کنید." else "فیلترها را تغییر داده یا جستجو را پاک کنید.",
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
                    .testTag("configs_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(configs, key = { it.id }) { config ->
                    ConfigItemCard(
                        config = config,
                        onTest = { viewModel.testSingleConfig(config) },
                        onCopy = { viewModel.copyToClipboard(context, config.rawUri, config.remark) },
                        onQrCode = { qrDialogConfig = config },
                        onToggleFavorite = { viewModel.toggleFavoriteConfig(config.id) },
                        onDelete = { viewModel.deleteConfig(config.id) }
                    )
                }
            }
        }
    }

    // QR Code Modal Dialog
    qrDialogConfig?.let { cfg ->
        QrCodeDialog(
            title = cfg.remark,
            content = cfg.rawUri,
            onDismiss = { qrDialogConfig = null },
            onCopy = {
                viewModel.copyToClipboard(context, cfg.rawUri, cfg.remark)
                qrDialogConfig = null
            }
        )
    }
}

@Composable
fun ConfigItemCard(
    config: ConfigEntity,
    onTest: () -> Unit,
    onCopy: () -> Unit,
    onQrCode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("config_card_${config.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (config.isAlive == true) EmeraldAccent.copy(alpha = 0.35f) else DarkBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Protocol Badge, Source Channel, Favorite, Ping Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProtocolBadge(protocol = config.protocol)
                    if (config.sourceChannel.isNotBlank()) {
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "@${config.sourceChannel}",
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
                            imageVector = if (config.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "علاقه‌مندی",
                            tint = if (config.isFavorite) AmberWarning else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    PingBadge(
                        pingMs = config.pingMs,
                        isAlive = config.isAlive,
                        onClick = onTest
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Server Remark / Title
            Text(
                text = config.remark,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Server Address & Port
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${config.server}:${config.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (config.details.isNotBlank()) {
                    Text(
                        text = "• ${config.details}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_config_btn_${config.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary.copy(alpha = 0.15f),
                        contentColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("کپی کانفیگ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                IconButton(
                    onClick = onQrCode,
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .size(36.dp)
                        .testTag("qr_config_btn_${config.id}")
                ) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = "بارکد", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .size(36.dp)
                        .testTag("delete_config_btn_${config.id}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = RedDead.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
