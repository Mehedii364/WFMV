package com.example.core.viewmodels

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.FavoriteEntity
import com.example.core.database.RecentEntity
import com.example.core.database.WfmvDatabase
import com.example.core.storage.ArchiveManager
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import com.example.core.storage.SortMode
import com.example.core.storage.StorageScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ClipboardAction {
    COPY,
    CUT
}

data class ClipboardState(
    val items: List<FileItem> = emptyList(),
    val action: ClipboardAction = ClipboardAction.COPY
)

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WfmvDatabase.getInstance(application)
    private val dao = db.wfmvDao()
    private val scanner = StorageScanner(application)

    private val rootDir = scanner.getDefaultStorageRoot()

    private val _currentDir = MutableStateFlow<File>(rootDir)
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _filterCategory = MutableStateFlow<FileCategory?>(null)
    val filterCategory: StateFlow<FileCategory?> = _filterCategory.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Multi-selection
    private val _selectedItems = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedItems: StateFlow<Set<FileItem>> = _selectedItems.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = MutableStateFlow(false)

    // Clipboard
    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    // Favorites & Recents
    val favorites: StateFlow<List<FavoriteEntity>> = dao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<RecentEntity>> = dao.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadDirectory(rootDir)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun loadDirectory(directory: File) {
        if (!directory.exists() || !directory.canRead()) {
            _snackbarMessage.value = "Cannot access ${directory.name}"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _currentDir.value = directory
            _selectedItems.value = emptySet()
            _files.value = scanner.listFiles(
                directory = directory,
                showHidden = _showHidden.value,
                sortMode = _sortMode.value,
                filterCategory = _filterCategory.value,
                searchQuery = _searchQuery.value
            )
            _isLoading.value = false
        }
    }

    fun refreshCurrentDirectory() {
        loadDirectory(_currentDir.value)
    }

    fun navigateUp(): Boolean {
        val current = _currentDir.value
        val parent = current.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadDirectory(parent)
            return true
        }
        return false
    }

    fun openItem(item: FileItem) {
        if (item.isDirectory) {
            loadDirectory(item.file)
        } else {
            // Record in recents
            viewModelScope.launch {
                dao.insertRecent(
                    RecentEntity(
                        path = item.path,
                        name = item.name,
                        sizeBytes = item.sizeBytes,
                        mimeType = item.mimeType,
                        isDirectory = false
                    )
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        refreshCurrentDirectory()
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        refreshCurrentDirectory()
    }

    fun setFilterCategory(category: FileCategory?) {
        _filterCategory.value = category
        refreshCurrentDirectory()
    }

    fun toggleHidden() {
        _showHidden.value = !_showHidden.value
        refreshCurrentDirectory()
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    // Selection
    fun toggleSelectItem(item: FileItem) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _selectedItems.value = current
    }

    fun selectAll() {
        _selectedItems.value = _files.value.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    // Clipboard
    fun cutSelection() {
        val items = _selectedItems.value.toList()
        if (items.isNotEmpty()) {
            _clipboard.value = ClipboardState(items, ClipboardAction.CUT)
            _snackbarMessage.value = "${items.size} item(s) cut to clipboard"
            clearSelection()
        }
    }

    fun copySelection() {
        val items = _selectedItems.value.toList()
        if (items.isNotEmpty()) {
            _clipboard.value = ClipboardState(items, ClipboardAction.COPY)
            _snackbarMessage.value = "${items.size} item(s) copied to clipboard"
            clearSelection()
        }
    }

    fun pasteClipboard() {
        val clip = _clipboard.value ?: return
        val destDir = _currentDir.value
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            for (item in clip.items) {
                val ok = if (clip.action == ClipboardAction.CUT) {
                    scanner.moveFileOrDirectory(item.file, destDir)
                } else {
                    scanner.copyFileOrDirectory(item.file, destDir)
                }
                if (ok) successCount++
            }
            if (clip.action == ClipboardAction.CUT) {
                _clipboard.value = null
            }
            _snackbarMessage.value = "Pasted $successCount item(s)"
            refreshCurrentDirectory()
        }
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    // File Actions
    fun createFolder(name: String) {
        viewModelScope.launch {
            val ok = scanner.createFolder(_currentDir.value, name)
            if (ok) {
                _snackbarMessage.value = "Folder created: $name"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to create folder"
            }
        }
    }

    fun createTextFile(name: String, content: String = "") {
        viewModelScope.launch {
            val ok = scanner.createTextFile(_currentDir.value, name, content)
            if (ok) {
                _snackbarMessage.value = "File created: $name"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to create file"
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val ok = scanner.renameFile(item.file, newName)
            if (ok) {
                _snackbarMessage.value = "Renamed to $newName"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to rename file"
            }
        }
    }

    fun deleteSelectedItems() {
        val items = _selectedItems.value.toList()
        if (items.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            var count = 0
            for (item in items) {
                if (scanner.deleteFileOrDirectory(item.file)) {
                    count++
                    dao.deleteFavoriteByPath(item.path)
                    dao.deleteRecent(item.path)
                }
            }
            clearSelection()
            _snackbarMessage.value = "Deleted $count item(s)"
            refreshCurrentDirectory()
        }
    }

    fun deleteSingleItem(item: FileItem) {
        viewModelScope.launch {
            if (scanner.deleteFileOrDirectory(item.file)) {
                dao.deleteFavoriteByPath(item.path)
                dao.deleteRecent(item.path)
                _snackbarMessage.value = "Deleted ${item.name}"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to delete ${item.name}"
            }
        }
    }

    fun zipSelectedItems(zipName: String) {
        val items = _selectedItems.value.map { it.file }
        if (items.isEmpty()) return
        val finalName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
        val zipFile = File(_currentDir.value, finalName)

        viewModelScope.launch {
            _isLoading.value = true
            val ok = ArchiveManager.createZip(items, zipFile)
            _isLoading.value = false
            clearSelection()
            if (ok) {
                _snackbarMessage.value = "Archive created: $finalName"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to create archive"
            }
        }
    }

    fun extractArchive(archiveItem: FileItem) {
        val targetName = archiveItem.file.nameWithoutExtension
        val targetDir = File(archiveItem.file.parentFile, targetName)

        viewModelScope.launch {
            _isLoading.value = true
            val ok = ArchiveManager.extractZip(archiveItem.file, targetDir)
            _isLoading.value = false
            if (ok) {
                _snackbarMessage.value = "Extracted to $targetName"
                refreshCurrentDirectory()
            } else {
                _snackbarMessage.value = "Failed to extract archive"
            }
        }
    }

    fun toggleFavorite(item: FileItem) {
        viewModelScope.launch {
            val exists = dao.isFavorite(item.path)
            if (exists) {
                dao.deleteFavoriteByPath(item.path)
                _snackbarMessage.value = "Removed from Favorites"
            } else {
                dao.insertFavorite(
                    FavoriteEntity(
                        path = item.path,
                        name = item.name,
                        isDirectory = item.isDirectory,
                        sizeBytes = item.sizeBytes,
                        mimeType = item.mimeType
                    )
                )
                _snackbarMessage.value = "Added to Favorites"
            }
        }
    }

    fun shareFile(item: FileItem) {
        val context = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share ${item.name} via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            _snackbarMessage.value = "Unable to share file: ${e.localizedMessage}"
        }
    }
}
