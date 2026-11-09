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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import com.example.core.viewmodels.FileManagerViewModel
import com.example.core.viewmodels.StorageCleanerViewModel
import com.example.ui.components.StorageOverviewCard
import com.example.ui.components.WfmvNavDestination
import com.example.ui.components.getCategoryColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    fileManagerViewModel: FileManagerViewModel,
    storageCleanerViewModel: StorageCleanerViewModel,
    onNavigate: (WfmvNavDestination) -> Unit,
    onCategoryClick: (FileCategory) -> Unit
) {
    val stats by storageCleanerViewModel.storageStats.collectAsState()
    val categoryBytes by storageCleanerViewModel.categoryBytes.collectAsState()
    val recents by fileManagerViewModel.recents.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header & Branding
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Logo",
                                tint = ObsidianBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WFMV",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Developed by Md. Mehedi Hasan",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .clickable { onNavigate(WfmvNavDestination.VAULT) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private Vault",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private Space",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Storage Overview Card
        item {
            StorageOverviewCard(
                stats = stats,
                onCleanClick = { onNavigate(WfmvNavDestination.STORAGE) },
                onDuplicatesClick = { onNavigate(WfmvNavDestination.STORAGE) }
            )
        }

        // Quick Category Cards
        item {
            Column {
                Text(
                    text = "CATEGORIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.IMAGE,
                        label = "Images",
                        icon = Icons.Default.Image,
                        bytes = categoryBytes[FileCategory.IMAGE] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.IMAGE) }
                    )
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.VIDEO,
                        label = "Videos",
                        icon = Icons.Default.VideoFile,
                        bytes = categoryBytes[FileCategory.VIDEO] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.VIDEO) }
                    )
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.AUDIO,
                        label = "Audio",
                        icon = Icons.Default.AudioFile,
                        bytes = categoryBytes[FileCategory.AUDIO] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.AUDIO) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.DOCUMENT,
                        label = "Documents",
                        icon = Icons.Default.PictureAsPdf,
                        bytes = categoryBytes[FileCategory.DOCUMENT] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.DOCUMENT) }
                    )
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.ARCHIVE,
                        label = "Archives",
                        icon = Icons.Default.Archive,
                        bytes = categoryBytes[FileCategory.ARCHIVE] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.ARCHIVE) }
                    )
                    CategoryChip(
                        modifier = Modifier.weight(1f),
                        category = FileCategory.APK,
                        label = "APKs",
                        icon = Icons.Default.Storage,
                        bytes = categoryBytes[FileCategory.APK] ?: 0L,
                        onClick = { onCategoryClick(FileCategory.APK) }
                    )
                }
            }
        }

        // Quick Ecosystem Tools Grid
        item {
            Column {
                Text(
                    text = "INTELLIGENCE & TOOLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EcosystemFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Duplicate Hunter",
                        subtitle = "SHA-256 Verifier",
                        icon = Icons.Default.ContentCopy,
                        accentColor = GoldPrimary,
                        onClick = { onNavigate(WfmvNavDestination.STORAGE) }
                    )
                    EcosystemFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Safe Junk Clean",
                        subtitle = "Cache & Temp Analyst",
                        icon = Icons.Default.CleaningServices,
                        accentColor = EmeraldSuccess,
                        onClick = { onNavigate(WfmvNavDestination.STORAGE) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EcosystemFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Private Vault",
                        subtitle = "AES-256-GCM Space",
                        icon = Icons.Default.Lock,
                        accentColor = Color(0xFFA78BFA),
                        onClick = { onNavigate(WfmvNavDestination.VAULT) }
                    )
                    EcosystemFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = "Developer Tools",
                        subtitle = "Hex & Signature",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFF60A5FA),
                        onClick = { onNavigate(WfmvNavDestination.TOOLS) }
                    )
                }
            }
        }

        // Recent Files
        if (recents.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT FILES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "View All",
                            color = GoldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigate(WfmvNavDestination.FILES) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recents.take(8).forEach { recent ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { onNavigate(WfmvNavDestination.FILES) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "File",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = recent.name,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FileItem.formatBytes(recent.sizeBytes),
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer Developer Note
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WFMV Ecosystem • Precision Engineered by Md. Mehedi Hasan",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    modifier: Modifier = Modifier,
    category: FileCategory,
    label: String,
    icon: ImageVector,
    bytes: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getCategoryColor(category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = getCategoryColor(category),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = if (bytes > 0) FileItem.formatBytes(bytes) else "Explore",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun EcosystemFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
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
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
