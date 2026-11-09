package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.DuplicateGroup
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import com.example.core.storage.JunkItem
import com.example.core.viewmodels.StorageCleanerViewModel
import com.example.ui.components.getCategoryBackgroundColor
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.CrimsonError
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
fun StorageScreen(
    viewModel: StorageCleanerViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Junk Cleaner", "Duplicate Hunter", "Large Files")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("storage_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Storage Intelligence",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Precision Storage & Content Analytics",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldSecondary
                )
            }

            IconButton(onClick = { viewModel.refreshStorageInfo() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = GoldPrimary
                )
            }
        }

        // Tab Navigation
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

        // Tab Content
        when (selectedTab) {
            0 -> StorageOverviewTab(viewModel)
            1 -> JunkCleanerTab(viewModel)
            2 -> DuplicateHunterTab(viewModel)
            3 -> LargeFilesTab(viewModel)
        }
    }
}

@Composable
private fun StorageOverviewTab(viewModel: StorageCleanerViewModel) {
    val stats by viewModel.storageStats.collectAsState()
    val categoryBytes by viewModel.categoryBytes.collectAsState()
    val cleanHistory by viewModel.cleanHistory.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Storage Meter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TOTAL DISK CAPACITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${FileItem.formatBytes(stats.usedBytes)} Used of ${FileItem.formatBytes(stats.totalBytes)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { stats.usedPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = GoldPrimary,
                        trackColor = ObsidianSurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Free: ${FileItem.formatBytes(stats.freeBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "${(stats.usedPercentage * 100).toInt()}% Allocated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Category-wise Breakdown
        item {
            Text(
                text = "STORAGE BY CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        val categories = listOf(
            FileCategory.IMAGE to "Images",
            FileCategory.VIDEO to "Videos",
            FileCategory.AUDIO to "Audio",
            FileCategory.DOCUMENT to "Documents",
            FileCategory.ARCHIVE to "Archives",
            FileCategory.APK to "APKs",
            FileCategory.OTHER to "Other"
        )

        items(categories) { (cat, label) ->
            val bytes = categoryBytes[cat] ?: 0L
            val catPercentage = if (stats.totalBytes > 0) (bytes.toFloat() / stats.totalBytes.toFloat()) else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(getCategoryColor(cat).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(cat),
                            contentDescription = label,
                            tint = getCategoryColor(cat),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = FileItem.formatBytes(bytes), color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { catPercentage.coerceIn(0.01f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = getCategoryColor(cat),
                            trackColor = ObsidianSurfaceElevated
                        )
                    }
                }
            }
        }

        // Clean History Summary
        if (cleanHistory.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT CLEANING AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(cleanHistory.take(5)) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = record.categoryLabel, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = FileItem.formatDate(record.timestamp), color = TextTertiary, fontSize = 10.sp)
                    }
                    Text(
                        text = "+${FileItem.formatBytes(record.bytesCleaned)}",
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JunkCleanerTab(viewModel: StorageCleanerViewModel) {
    val junkItems by viewModel.junkItems.collectAsState()
    val isScanning by viewModel.isJunkScanning.collectAsState()
    val status by viewModel.junkStatus.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    val selectedCount = junkItems.count { it.isSelected }
    val selectedBytes = junkItems.filter { it.isSelected }.sumOf { it.fileItem.sizeBytes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Safe Junk Analyzer",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isScanning) status else "${junkItems.size} items discovered",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { viewModel.startJunkScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isScanning) "Scanning..." else "Scan Now", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (junkItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceElevated)
                    .clip(RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedCount == junkItems.size,
                        onCheckedChange = { viewModel.selectAllJunk(it) },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text("Select All ($selectedCount)", color = TextPrimary, fontSize = 12.sp)
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clean ${FileItem.formatBytes(selectedBytes)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(junkItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleJunkSelection(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isSelected,
                                onCheckedChange = { viewModel.toggleJunkSelection(item) },
                                colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.fileItem.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Row {
                                    Text(text = item.reason, color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = " • ", color = TextTertiary, fontSize = 10.sp)
                                    Text(text = item.fileItem.formattedSize, color = TextTertiary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else if (!isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean", tint = GoldPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ready for Safe Junk Analysis", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap 'Scan Now' to detect zero-byte files, temporary logs, and caches.", color = TextTertiary, fontSize = 11.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoldPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(status, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Safe Cleanup Confirmation", color = GoldPrimary) },
            text = {
                Text(
                    text = "Are you sure you want to clean $selectedCount item(s) to free ${FileItem.formatBytes(selectedBytes)}? Personal documents and photos are preserved.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performCleanJunk()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Clean Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
private fun DuplicateHunterTab(viewModel: StorageCleanerViewModel) {
    val groups by viewModel.duplicateGroups.collectAsState()
    val isScanning by viewModel.isDuplicateScanning.collectAsState()
    val progress by viewModel.duplicateProgress.collectAsState()
    val status by viewModel.duplicateStatus.collectAsState()
    val selectedDuplicates by viewModel.selectedDuplicates.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val totalWaste = groups.sumOf { it.sizeBytes * (it.files.size - 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Exact Duplicate Finder",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isScanning) "Hashing candidates... $progress%" else "${groups.size} clusters (${FileItem.formatBytes(totalWaste)} recoverable)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { viewModel.startDuplicateScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isScanning) "$progress%" else "Find Duplicates", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (groups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceElevated)
                    .clip(RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    TextButton(onClick = { viewModel.smartSelectDuplicates(keepOldest = true) }) {
                        Text("Keep Oldest", color = GoldPrimary, fontSize = 11.sp)
                    }
                    TextButton(onClick = { viewModel.smartSelectDuplicates(keepOldest = false) }) {
                        Text("Keep Newest", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedDuplicates.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete (${selectedDuplicates.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cluster: ${group.files.size} identical files",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${FileItem.formatBytes(group.sizeBytes)} each",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "SHA-256: ${group.hashSha256.take(16)}...",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            group.files.forEachIndexed { idx, fileItem ->
                                val isSelected = selectedDuplicates.contains(fileItem)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CrimsonError.copy(alpha = 0.1f) else ObsidianSurfaceElevated)
                                        .clickable { viewModel.toggleDuplicateSelection(fileItem) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Select",
                                        tint = if (isSelected) CrimsonError else TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = fileItem.name, color = TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = fileItem.formattedDate, color = TextTertiary, fontSize = 10.sp)
                                    }
                                    if (idx == 0) {
                                        Text(
                                            text = "ORIGINAL",
                                            color = EmeraldSuccess,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        } else if (!isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicates", tint = GoldPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cryptographic Duplicate Detection", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Combines size matching with exact SHA-256 byte hashing.", color = TextTertiary, fontSize = 11.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoldPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(status, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Duplicate Copies", color = CrimsonError) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${selectedDuplicates.size} redundant duplicate copy/copies?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedDuplicates()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = TextPrimary)
                ) {
                    Text("Delete Duplicates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
private fun LargeFilesTab(viewModel: StorageCleanerViewModel) {
    val largeFiles by viewModel.largeFiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Largest Files Discovery (>10 MB)",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Identify top storage-consuming media, video files, and archives",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (largeFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No large files found (> 10MB)", color = TextTertiary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(largeFiles) { item ->
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
                                    .background(getCategoryBackgroundColor(item.category)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.category),
                                    contentDescription = item.category.name,
                                    tint = getCategoryColor(item.category),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(text = item.formattedDate, color = TextTertiary, fontSize = 10.sp)
                            }
                            Text(
                                text = item.formattedSize,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
