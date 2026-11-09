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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.FileItem
import com.example.core.viewmodels.ToolsViewModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("App Insights", "Dev Diagnostics", "About WFMV")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("tools_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Advanced Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Developer Utilities & Storage Insights",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary
                )
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) GoldPrimary else TextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Content
        when (selectedTab) {
            0 -> AppInsightsTab(viewModel)
            1 -> DevDiagnosticsTab(viewModel)
            2 -> AboutWfmvTab()
        }
    }
}

@Composable
private fun AppInsightsTab(viewModel: ToolsViewModel) {
    val apps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()

    LaunchedEffect(Unit) {
        if (apps.isEmpty()) {
            viewModel.loadInstalledApps()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed Applications (${apps.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Button(
                onClick = { viewModel.loadInstalledApps() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No applications found", color = TextTertiary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps) { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = app.appName,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = app.packageName,
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "v${app.versionName} • ${app.formattedSize}",
                                    color = GoldSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(onClick = { viewModel.openAppSettings(app.packageName) }) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "App Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevDiagnosticsTab(viewModel: ToolsViewModel) {
    val diag by viewModel.diagnostics.collectAsState()
    val hexDump by viewModel.hexDump.collectAsState()
    val apkInfo by viewModel.apkInfo.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FILE DIAGNOSTICS & CRYPTO ENGINE",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Provides cryptographic checksums (SHA-256, SHA-1, MD5), MIME type detection, file magic byte signatures, JSON/XML syntax validation, and live Hex dumping.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (isAnalyzing) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
        } else if (diag != null) {
            val d = diag!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = d.fileName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Signature: ${d.detectedSignature}", color = EmeraldSuccess, fontSize = 12.sp)
                        Text(text = "MIME: ${d.mimeType}", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "SHA-256:", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = d.sha256, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "SHA-1:", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = d.sha1, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "MD5:", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = d.md5, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

                        d.lineCount?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Lines: $it • Words: ${d.wordCount}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (hexDump.isNotEmpty()) {
                item {
                    Text(text = "HEX DUMP (First 1KB)", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated)
                    ) {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .horizontalScroll(scroll)
                                .padding(10.dp)
                        ) {
                            hexDump.take(32).forEach { row ->
                                Text(
                                    text = "${row.offset}  ${row.hexPart} |${row.asciiPart}|",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select any file from the 'Files' tab and choose 'Properties' or 'Diagnostics' to inspect its checksums and hex stream.",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutWfmvTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WFMV — WAFA FILE MANAGER",
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Storage. Intelligent Files. Complete Privacy.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Developed by Md. Mehedi Hasan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Engineered as a native Android ecosystem combining advanced file browsing, storage analytics, exact SHA-256 duplicate verification, safe cache/junk cleaning, and the Wafa Private Space with AES-256-GCM authenticated encryption.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ECOSYSTEM ARCHITECTURE", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Jetpack Compose & Material 3 Obsidian Dark", color = TextPrimary, fontSize = 12.sp)
                    Text("• MVVM + Clean Architecture with Room & Coroutines", color = TextPrimary, fontSize = 12.sp)
                    Text("• AES-256-GCM Authenticated Encryption & PBKDF2", color = TextPrimary, fontSize = 12.sp)
                    Text("• Automated CI Build Pipeline with GitHub Actions", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}
