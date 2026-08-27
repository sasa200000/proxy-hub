package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.fetcher.ShadowmereFetcher
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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
    var allCountries by remember { mutableStateOf<List<ShadowmereFetcher.CountryInfo>>(emptyList()) }
    var proxyCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Load countries on startup
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val countries = ShadowmereFetcher.fetchCountryCodes()
            allCountries = countries
            errorMessage = null

            // Fetch proxy counts in background
            scope.launch {
                try {
                    val counts = ShadowmereFetcher.fetchProxyCountByCountry()
                    proxyCounts = counts
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "خطا"
        }
        isLoading = false
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
                        text = "${allCountries.size} کشور فعال",
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
                    Text("در حال دریافت لیست کشورها...", color = TextSecondary)
                }
            }
        }

        // Error
        if (errorMessage != null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❌ خطا", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                allCountries = ShadowmereFetcher.fetchCountryCodes()
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage
                            }
                            isLoading = false
                        }
                    }) {
                        Text("تلاش مجدد")
                    }
                }
            }
        }

        // Country List
        if (!isLoading && errorMessage == null) {
            val filtered = allCountries.filter { country ->
                searchQuery.isBlank() ||
                country.name.contains(searchQuery, ignoreCase = true) ||
                country.code.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered.size) { index ->
                    val country = filtered[index]
                    val proxyCount = proxyCounts[country.code] ?: 0
                    val flag = COUNTRY_FLAGS[country.code] ?: "🏳️"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFetchProxies(country.code)
                            },
                        colors = CardDefaults.cardColors(containerColor = DarkBgMain),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flag
                            Text(flag, fontSize = 28.sp)

                            Spacer(modifier = Modifier.width(12.dp))

                            // Country name and count
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = country.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (proxyCount > 0) "$proxyCount پروکسی فعال" else country.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Download button
                            Button(
                                onClick = { onFetchProxies(country.code) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
