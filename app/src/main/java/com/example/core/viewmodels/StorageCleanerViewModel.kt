package com.example.core.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.CleanHistoryEntity
import com.example.core.database.ScanHistoryEntity
import com.example.core.database.WfmvDatabase
import com.example.core.storage.DuplicateGroup
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import com.example.core.storage.JunkItem
import com.example.core.storage.StorageScanner
import com.example.core.storage.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StorageCleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WfmvDatabase.getInstance(application)
    private val dao = db.wfmvDao()
    private val scanner = StorageScanner(application)

    // Storage Overview
    private val _storageStats = MutableStateFlow(StorageStats(0L, 0L, 0L, 0f))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _categoryBytes = MutableStateFlow<Map<FileCategory, Long>>(emptyMap())
    val categoryBytes: StateFlow<Map<FileCategory, Long>> = _categoryBytes.asStateFlow()

    // Junk Cleaner
    private val _junkItems = MutableStateFlow<List<JunkItem>>(emptyList())
    val junkItems: StateFlow<List<JunkItem>> = _junkItems.asStateFlow()

    private val _isJunkScanning = MutableStateFlow(false)
    val isJunkScanning: StateFlow<Boolean> = _isJunkScanning.asStateFlow()

    private val _junkStatus = MutableStateFlow("")
    val junkStatus: StateFlow<String> = _junkStatus.asStateFlow()

    // Duplicates
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _selectedDuplicates = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedDuplicates: StateFlow<Set<FileItem>> = _selectedDuplicates.asStateFlow()

    private val _isDuplicateScanning = MutableStateFlow(false)
    val isDuplicateScanning: StateFlow<Boolean> = _isDuplicateScanning.asStateFlow()

    private val _duplicateProgress = MutableStateFlow(0)
    val duplicateProgress: StateFlow<Int> = _duplicateProgress.asStateFlow()

    private val _duplicateStatus = MutableStateFlow("")
    val duplicateStatus: StateFlow<String> = _duplicateStatus.asStateFlow()

    // Large files
    private val _largeFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val largeFiles: StateFlow<List<FileItem>> = _largeFiles.asStateFlow()

    // Clean History
    val cleanHistory: StateFlow<List<CleanHistoryEntity>> = dao.getCleanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            _storageStats.value = scanner.getStorageStats()
            _categoryBytes.value = scanner.scanCategoryBytes()
            _largeFiles.value = scanner.scanLargeFiles()
        }
    }

    // Junk Cleaner
    fun startJunkScan() {
        viewModelScope.launch {
            _isJunkScanning.value = true
            _junkStatus.value = "Analyzing storage caches and temporary files..."
            val items = scanner.scanJunk { status ->
                _junkStatus.value = status
            }
            _junkItems.value = items
            _isJunkScanning.value = false
            _junkStatus.value = "Found ${items.size} cleanable items"
        }
    }

    fun toggleJunkSelection(item: JunkItem) {
        val updated = _junkItems.value.map {
            if (it == item) it.copy(isSelected = !it.isSelected) else it
        }
        _junkItems.value = updated
    }

    fun selectAllJunk(selected: Boolean) {
        val updated = _junkItems.value.map { it.copy(isSelected = selected) }
        _junkItems.value = updated
    }

    fun performCleanJunk() {
        val selected = _junkItems.value.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var freedBytes = 0L
            var count = 0
            for (junk in selected) {
                val len = junk.fileItem.sizeBytes
                if (scanner.deleteFileOrDirectory(junk.fileItem.file)) {
                    freedBytes += len
                    count++
                }
            }
            dao.insertCleanRecord(
                CleanHistoryEntity(
                    bytesCleaned = freedBytes,
                    itemCount = count,
                    categoryLabel = "Junk & Cache Clean"
                )
            )
            _toastMessage.value = "Freed ${FileItem.formatBytes(freedBytes)} across $count items"
            _junkItems.value = _junkItems.value.filter { !it.isSelected }
            refreshStorageInfo()
        }
    }

    // Duplicate Finder
    fun startDuplicateScan() {
        viewModelScope.launch {
            _isDuplicateScanning.value = true
            _duplicateProgress.value = 0
            _duplicateStatus.value = "Analyzing candidate files by byte size..."
            val groups = scanner.scanDuplicates { progress, file ->
                _duplicateProgress.value = progress
                _duplicateStatus.value = "SHA-256 Hashing: $file"
            }
            _duplicateGroups.value = groups
            _isDuplicateScanning.value = false
            _duplicateStatus.value = "Found ${groups.size} duplicate clusters"

            val waste = groups.sumOf { it.sizeBytes * (it.files.size - 1) }
            dao.insertScanRecord(
                ScanHistoryEntity(
                    duplicatesFound = groups.sumOf { it.files.size - 1 },
                    duplicateWasteBytes = waste,
                    scanType = "Exact SHA-256"
                )
            )

            // By default, smart select duplicates keeping original (oldest) untouched
            smartSelectDuplicates(keepOldest = true)
        }
    }

    fun smartSelectDuplicates(keepOldest: Boolean) {
        val toSelect = mutableSetOf<FileItem>()
        for (group in _duplicateGroups.value) {
            val sorted = if (keepOldest) {
                group.files.sortedBy { it.lastModified }
            } else {
                group.files.sortedByDescending { it.lastModified }
            }
            // Skip the first one (the original to keep), select all subsequent copies
            toSelect.addAll(sorted.drop(1))
        }
        _selectedDuplicates.value = toSelect
    }

    fun toggleDuplicateSelection(item: FileItem) {
        val current = _selectedDuplicates.value.toMutableSet()
        if (current.contains(item)) current.remove(item) else current.add(item)
        _selectedDuplicates.value = current
    }

    fun deleteSelectedDuplicates() {
        val selected = _selectedDuplicates.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var freed = 0L
            var count = 0
            for (item in selected) {
                val len = item.sizeBytes
                if (scanner.deleteFileOrDirectory(item.file)) {
                    freed += len
                    count++
                }
            }
            _toastMessage.value = "Removed $count duplicate file(s), saved ${FileItem.formatBytes(freed)}"
            _selectedDuplicates.value = emptySet()
            // Re-filter remaining groups
            val updatedGroups = _duplicateGroups.value.mapNotNull { group ->
                val remaining = group.files.filter { it.file.exists() }
                if (remaining.size > 1) group.copy(files = remaining) else null
            }
            _duplicateGroups.value = updatedGroups
            refreshStorageInfo()
        }
    }
}
