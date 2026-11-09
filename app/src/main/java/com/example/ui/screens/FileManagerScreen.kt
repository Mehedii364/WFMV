package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import com.example.core.storage.SortMode
import com.example.core.viewmodels.FileManagerViewModel
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.FileListItemView
import com.example.ui.components.getCategoryBackgroundColor
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewers.DocumentViewerDialog
import com.example.ui.viewers.FilePropertiesDialog

@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel
) {
    val currentDir by viewModel.currentDir.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val isSelectionMode = selectedItems.isNotEmpty()

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var activeViewerFile by remember { mutableStateOf<FileItem?>(null) }
    var activePropertiesFile by remember { mutableStateOf<FileItem?>(null) }
    var showSearchRow by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    // Intercept back button for navigating up directories
    BackHandler(enabled = true) {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            val handled = viewModel.navigateUp()
            if (!handled) {
                // At root directory
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("file_manager_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GoldPrimary
                    )
                }

                Text(
                    text = if (isSelectionMode) "${selectedItems.size} Selected" else currentDir.name.ifEmpty { "Storage" },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (isSelectionMode) {
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Select All",
                            tint = GoldPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Selection",
                            tint = TextSecondary
                        )
                    }
                } else {
                    IconButton(onClick = { showSearchRow = !showSearchRow }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearchRow) GoldPrimary else TextSecondary
                        )
                    }

                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(ObsidianSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)", color = TextPrimary) },
                                onClick = {
                                    viewModel.setSortMode(SortMode.NAME_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)", color = TextPrimary) },
                                onClick = {
                                    viewModel.setSortMode(SortMode.NAME_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)", color = TextPrimary) },
                                onClick = {
                                    viewModel.setSortMode(SortMode.DATE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Largest First)", color = TextPrimary) },
                                onClick = {
                                    viewModel.setSortMode(SortMode.SIZE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Search input field
            AnimatedVisibility(visible = showSearchRow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search in this directory...", color = TextTertiary) },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Breadcrumbs Navigation Bar
            BreadcrumbBar(
                currentDir = currentDir,
                onNavigateToDir = { viewModel.loadDirectory(it) }
            )

            // Multi-selection Action Bar
            AnimatedVisibility(visible = isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionIcon(icon = Icons.Default.ContentCut, label = "Cut") {
                        viewModel.cutSelection()
                    }
                    ActionIcon(icon = Icons.Default.ContentCopy, label = "Copy") {
                        viewModel.copySelection()
                    }
                    ActionIcon(icon = Icons.Default.Archive, label = "Zip") {
                        showZipDialog = true
                    }
                    ActionIcon(icon = Icons.Default.Delete, label = "Delete", tint = CrimsonError) {
                        showDeleteConfirmDialog = true
                    }
                }
            }

            // Clipboard Paste Bar
            clipboard?.let { clip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, GoldPrimary, RoundedCornerShape(0.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${clip.items.size} item(s) in clipboard (${clip.action.name})",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        TextButton(onClick = { viewModel.pasteClipboard() }) {
                            Text("Paste Here", color = GoldPrimary, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { viewModel.cancelClipboard() }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            }

            // Files Content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Empty Directory",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No files or folders found here",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.path }) { item ->
                        val isSelected = selectedItems.contains(item)
                        GridItemCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelectItem(item)
                                } else if (item.isDirectory) {
                                    viewModel.openItem(item)
                                } else {
                                    activeViewerFile = item
                                }
                            },
                            onLongClick = { viewModel.toggleSelectItem(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.path }) { item ->
                        val isSelected = selectedItems.contains(item)
                        val isFav = favorites.any { it.path == item.path }
                        FileListItemView(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isFavorite = isFav,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.openItem(item)
                                } else {
                                    activeViewerFile = item
                                }
                            },
                            onLongClick = { viewModel.toggleSelectItem(item) },
                            onToggleSelect = { viewModel.toggleSelectItem(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onShare = { viewModel.shareFile(item) },
                            onDelete = { viewModel.deleteSingleItem(item) },
                            onProperties = { activePropertiesFile = item }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = fabMenuExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                fabMenuExpanded = false
                                showCreateFileDialog = true
                            },
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = GoldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "New File")
                        }

                        FloatingActionButton(
                            onClick = {
                                fabMenuExpanded = false
                                showCreateFolderDialog = true
                            },
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = GoldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { fabMenuExpanded = !fabMenuExpanded },
                    containerColor = GoldPrimary,
                    contentColor = ObsidianBg,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (fabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Actions"
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create Text File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("e.g. notes.txt") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            viewModel.createTextFile(fileName)
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showZipDialog) {
        var zipName by remember { mutableStateOf("archive.zip") }
        AlertDialog(
            onDismissRequest = { showZipDialog = false },
            title = { Text("Create ZIP Archive", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (zipName.isNotBlank()) {
                            viewModel.zipSelectedItems(zipName)
                            showZipDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = ObsidianBg)
                ) {
                    Text("Compress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showZipDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Confirmation", color = CrimsonError) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${selectedItems.size} selected item(s)?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedItems()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = TextPrimary)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Active Viewers
    activeViewerFile?.let { fileItem ->
        DocumentViewerDialog(
            fileItem = fileItem,
            onDismiss = { activeViewerFile = null }
        )
    }

    activePropertiesFile?.let { fileItem ->
        FilePropertiesDialog(
            fileItem = fileItem,
            onDismiss = { activePropertiesFile = null }
        )
    }
}

@Composable
fun GridItemCard(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ObsidianSurfaceElevated else ObsidianSurface)
            .border(1.dp, if (isSelected) GoldPrimary else ObsidianBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getCategoryBackgroundColor(item.category)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = item.category.name,
                    tint = getCategoryColor(item.category),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (item.isDirectory) "${item.childCount} items" else item.formattedSize,
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = GoldPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
