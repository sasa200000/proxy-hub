package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.fetcher.ShadowmereFetcher
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Country flags mapping
private val COUNTRY_FLAGS = mapOf(
    "US" to "🇺🇸", "GB" to "🇬🇧", "DE" to "🇩🇪", "FR" to "🇫🇷", "NL" to "🇳🇱",
    "CA" to "🇨🇦", "JP" to "🇯🇵", "SG" to "🇸🇬", "HK" to "🇭🇰", "KR" to "🇰🇷",
    "IN" to "🇮🇳", "AU" to "🇦🇺", "BR" to "🇧🇷", "RU" to "🇷🇺", "UA" to "🇺🇦",
    "PL" to "🇵🇱", "CZ" to "🇨🇿", "SE" to "🇸🇪", "FI" to "🇫🇮", "NO" to "🇳🇴",
    "CH" to "🇨🇭", "AT" to "🇦🇹", "BE" to "🇧🇪", "ES" to "🇪🇸", "IT" to "🇮🇹",
    "PT" to "🇵🇹", "RO" to "🇷🇴", "BG" to "🇧🇬", "HU" to "🇭🇺", "DK" to "🇩🇰",
    "IE" to "🇮🇪", "LT" to "🇱🇹", "LV" to "🇱🇻", "EE" to "🇪🇪", "IS" to "🇮🇸",
    "LU" to "🇱🇺", "MT" to "🇲🇹", "CY" to "🇨🇾", "SK" to "🇸🇰", "SI" to "🇸🇮",
    "HR" to "🇭🇷", "RS" to "🇷🇸", "BA" to "🇧🇦", "MK" to "🇲🇰", "AL" to "🇦🇱",
    "GR" to "🇬🇷", "TR" to "🇹🇷", "IL" to "🇮🇱", "AE" to "🇦🇪", "SA" to "🇸🇦",
    "ZA" to "🇿🇦", "EG" to "🇪🇬", "NG" to "🇳🇬", "KE" to "🇰🇪", "GH" to "🇬🇭",
    "MX" to "🇲🇽", "AR" to "🇦🇷", "CL" to "🇨🇱", "CO" to "🇨🇴", "PE" to "🇵🇪",
    "TH" to "🇹🇭", "VN" to "🇻🇳", "PH" to "🇵🇭", "ID" to "🇮🇩", "MY" to "🇲🇾",
    "TW" to "🇹🇼", "CN" to "🇨🇳", "PK" to "🇵🇰", "BD" to "🇧🇩", "LK" to "🇱🇰",
    "NP" to "🇳🇵", "MM" to "🇲🇲", "KH" to "🇰🇭", "LA" to "🇱🇦"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowmereScreen(
    onBack: () -> Unit,
    onFetchProxies: (String) -> Unit
) {
    var proxies by remember { mutableStateOf<List<ShadowmereFetcher.ShadowmereProxy>>(emptyList()) }
    var countries by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Country code to name mapping
    val countryCodeToName = remember(countries) {
        mutableMapOf<String, String>()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = ShadowmereFetcher.fetchProxies(pageSize = 200)
        isLoading = false
        if (result.isSuccess) {
            proxies = result.proxies
            totalCount = result.totalCount
            countries = result.countries
            errorMessage = null
        } else {
            errorMessage = result.error
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgMain)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "🌍 Shadowmere",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$totalCount پروکسی فعال از ${countries.size} کشور",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBgMain)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("جستجوی کشور...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                focusedContainerColor = DarkBgMain,
                unfocusedContainerColor = DarkBgMain,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("در حال دریافت پروکسی‌ها...", color = TextSecondary)
                }
            }
        }

        // Error
        if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❌ خطا", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
        }

        // Country List
        if (!isLoading && errorMessage == null) {
            val filteredProxies = proxies.filter { proxy ->
                searchQuery.isBlank() ||
                proxy.country.contains(searchQuery, ignoreCase = true) ||
                proxy.location.contains(searchQuery, ignoreCase = true) ||
                proxy.countryCode.contains(searchQuery, ignoreCase = true)
            }

            // Group by country
            val grouped = filteredProxies.groupBy { it.country }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All countries button
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCountry = ""
                                scope.launch {
                                    isLoading = true
                                    val result = ShadowmereFetcher.fetchProxies(pageSize = 200)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        proxies = result.proxies
                                        totalCount = result.totalCount
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCountry.isEmpty()) CyanPrimary.copy(alpha = 0.2f) else DarkBgMain
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌍", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("همه کشورها", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("$totalCount پروکسی فعال", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            if (selectedCountry.isEmpty()) {
                                Text("✓", color = CyanPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Country items
                grouped.forEach { (country, countryProxies) ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCountry = country
                                    proxies = countryProxies
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCountry == country) EmeraldAccent.copy(alpha = 0.2f) else DarkBgMain
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val countryCode = countryProxies.firstOrNull()?.countryCode ?: ""
                                Text(COUNTRY_FLAGS[countryCode] ?: "🏳️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(country, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${countryProxies.size} پروکسی", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Button(
                                    onClick = { onFetchProxies(countryCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("دریافت", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
