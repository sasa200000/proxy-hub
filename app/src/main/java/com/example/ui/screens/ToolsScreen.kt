package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProxyConfig
import com.example.data.model.ProxyProtocol
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
fun ToolsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val pingTimeout by viewModel.pingTimeoutMs.collectAsStateWithLifecycle()
    val concurrency by viewModel.concurrency.collectAsStateWithLifecycle()
    val proxyConfig by viewModel.proxyConfig.collectAsStateWithLifecycle()

    var manualText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgMain)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: Proxy Settings
        ProxySettingsCard(
            proxyConfig = proxyConfig,
            onSave = { viewModel.setProxyConfig(it) }
        )

        // Section 1: Manual Extractor
        Card(
            modifier = Modifier.fillMaxWidth().testTag("manual_extractor_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استخراج هوشمند از متن یا پیام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "هر متنی شامل کانفیگ‌های vless, vmess, trojan, ss یا لینک پروکسی‌های تلگرام را در کادر زیر قرار دهید تا استخراج و تفکیک شوند:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("manual_extractor_input"),
                    placeholder = {
                        Text(
                            "متن، پیام‌های کانال، یا کد Base64 را اینجا پیست کنید...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()
                            if (clip != null) {
                                manualText = clip.text
                            }
                        },
                        modifier = Modifier.testTag("paste_clipboard_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جای‌گذاری از کلیپ‌بورد", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (manualText.isNotBlank()) {
                                viewModel.extractAndSaveFromText(manualText)
                                manualText = ""
                            }
                        },
                        enabled = manualText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("extract_and_save_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBgMain)
                    ) {
                        Text("استخراج و ذخیره", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Batch Export Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "خروجی و اشتراک‌گذاری سریع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.copyAllAliveConfigs(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = EmeraldAccent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("کپی همه کانفیگ‌های سالم (V2Ray / VLESS)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = { viewModel.copyAllAliveProxies(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = CyanPrimary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("کپی همه پروکسی‌های آنلاین تلگرام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = { viewModel.exportBase64Subscription(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = PurpleAccent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تولید لینک سابسکریپشن Base64", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Section 3: Ping & Tester Engine Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنظیمات موتور تست پینگ و سلامت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Timeout Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "حداکثر زمان انتظار (Timeout)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(text = "${pingTimeout} ms", style = MaterialTheme.typography.labelLarge, color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = pingTimeout.toFloat(),
                    onValueChange = { viewModel.setPingTimeout(it.toInt()) },
                    valueRange = 1000f..5000f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                // Concurrency Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "تعداد پردازش همزمان تست (Threads)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(text = "$concurrency رشته", style = MaterialTheme.typography.labelLarge, color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = concurrency.toFloat(),
                    onValueChange = { viewModel.setConcurrency(it.toInt()) },
                    valueRange = 4f..24f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }

        // Section 4: Clear Database
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, RedDead.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = RedDead,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پاکسازی و مدیریت حافظه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RedDead
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.deleteAllConfigs() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDead),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDead.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حذف همه کانفیگ‌ها", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.deleteAllProxies() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDead),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDead.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حذف همه پروکسی‌ها", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun ProxySettingsCard(
    proxyConfig: ProxyConfig,
    onSave: (ProxyConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(proxyConfig.enabled) }
    var host by remember { mutableStateOf(proxyConfig.host) }
    var portText by remember { mutableStateOf(proxyConfig.port.toString()) }
    var selectedType by remember { mutableIntStateOf(
        when (proxyConfig.type) {
            ProxyProtocol.SOCKS5 -> 0
            ProxyProtocol.HTTP -> 1
            ProxyProtocol.HTTPS -> 2
        }
    ) }

    Card(
        modifier = modifier.fillMaxWidth().testTag("proxy_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "تنظیمات پروکسی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "برای عبور از فیلتر، آدرس پروکسی VPN خود را وارد کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBgMain,
                        checkedTrackColor = CyanPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("proxy_enabled_switch")
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(14.dp))

                // Proxy Type Selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("SOCKS5", "HTTP", "HTTPS")
                    types.forEachIndexed { index, label ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                            color = if (selectedType == index) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedType == index) CyanPrimary else DarkBorder
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (selectedType == index) CyanPrimary else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedType == index) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .padding(1.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Host
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth().testTag("proxy_host_input"),
                    label = { Text("آدرس سرور پروکسی", color = TextSecondary) },
                    placeholder = { Text("مثال: 127.0.0.1", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Port
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth().testTag("proxy_port_input"),
                    label = { Text("پورت", color = TextSecondary) },
                    placeholder = { Text("1080", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Save Button
                Button(
                    onClick = {
                        val port = portText.toIntOrNull() ?: 1080
                        val type = when (selectedType) {
                            0 -> ProxyProtocol.SOCKS5
                            1 -> ProxyProtocol.HTTP
                            else -> ProxyProtocol.HTTPS
                        }
                        onSave(ProxyConfig(enabled = true, type = type, host = host.trim(), port = port))
                    },
                    enabled = host.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("save_proxy_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBgMain)
                ) {
                    Text("ذخیره تنظیمات پروکسی", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 مثال: اگر از v2rayNG استفاده می‌کنید، پورت SOCKS5 معمولاً 10808 و HTTP معمولاً 10809 است",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
