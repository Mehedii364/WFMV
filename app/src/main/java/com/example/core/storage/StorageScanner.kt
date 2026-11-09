package com.example.core.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.core.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC,
    TYPE
}

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercentage: Float
)

data class DuplicateGroup(
    val sizeBytes: Long,
    val hashSha256: String,
    val files: List<FileItem>
)

data class JunkItem(
    val fileItem: FileItem,
    val reason: String, // "Empty Folder", "Zero-byte File", "Temporary Cache", "Old Log", "Unused APK"
    var isSelected: Boolean = true
)

class StorageScanner(private val context: Context) {

    fun getDefaultStorageRoot(): File {
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists() && ext.canRead()) {
            return ext
        }
        val appExt = context.getExternalFilesDir(null)
        if (appExt != null && appExt.exists()) {
            return appExt
        }
        return context.filesDir
    }

    fun getStorageStats(): StorageStats {
        val root = getDefaultStorageRoot()
        return try {
            val stat = StatFs(root.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = (total - free).coerceAtLeast(0L)
            val percentage = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
            StorageStats(total, used, free, percentage)
        } catch (e: Exception) {
            StorageStats(0L, 0L, 0L, 0f)
        }
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortMode: SortMode = SortMode.NAME_ASC,
        filterCategory: FileCategory? = null,
        searchQuery: String = ""
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val filtered = rawFiles.asSequence()
            .filter { file ->
                if (!showHidden && (file.isHidden || file.name.startsWith("."))) false
                else true
            }
            .map { FileItem.fromFile(it) }
            .filter { item ->
                if (filterCategory != null && filterCategory != FileCategory.FOLDER) {
                    item.category == filterCategory
                } else true
            }
            .filter { item ->
                if (searchQuery.isNotBlank()) {
                    item.name.contains(searchQuery, ignoreCase = true)
                } else true
            }
            .toList()

        // Separate directories and files so directories always appear first (standard file manager UX)
        val dirs = filtered.filter { it.isDirectory }
        val files = filtered.filter { !it.isDirectory }

        val comparator = when (sortMode) {
            SortMode.NAME_ASC -> compareBy<FileItem> { it.name.lowercase() }
            SortMode.NAME_DESC -> compareByDescending<FileItem> { it.name.lowercase() }
            SortMode.DATE_DESC -> compareByDescending<FileItem> { it.lastModified }
            SortMode.DATE_ASC -> compareBy<FileItem> { it.lastModified }
            SortMode.SIZE_DESC -> compareByDescending<FileItem> { it.sizeBytes }
            SortMode.SIZE_ASC -> compareBy<FileItem> { it.sizeBytes }
            SortMode.TYPE -> compareBy<FileItem> { it.extension }
        }

        dirs.sortedWith(comparator) + files.sortedWith(comparator)
    }

    suspend fun scanCategory(category: FileCategory, maxResults: Int = 200): List<FileItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FileItem>()
            val root = getDefaultStorageRoot()

            fun walk(dir: File) {
                if (results.size >= maxResults) return
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (results.size >= maxResults) break
                    if (f.name.startsWith(".")) continue
                    if (f.isDirectory) {
                        walk(f)
                    } else {
                        val item = FileItem.fromFile(f)
                        if (item.category == category) {
                            results.add(item)
                        }
                    }
                }
            }

            walk(root)
            results.sortedByDescending { it.lastModified }
        }

    suspend fun scanCategoryBytes(): Map<FileCategory, Long> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<FileCategory, Long>()
        for (cat in FileCategory.values()) {
            map[cat] = 0L
        }
        val root = getDefaultStorageRoot()
        var count = 0

        fun walk(dir: File) {
            if (count > 2000) return // Performance limit for interactive scan
            val children = dir.listFiles() ?: return
            for (f in children) {
                if (count > 2000) break
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    walk(f)
                } else {
                    count++
                    val cat = FileItem.resolveCategory(f)
                    map[cat] = (map[cat] ?: 0L) + f.length()
                }
            }
        }

        walk(root)
        map
    }

    suspend fun scanLargeFiles(minSizeBytes: Long = 10 * 1024 * 1024L, limit: Int = 40): List<FileItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FileItem>()
            val root = getDefaultStorageRoot()

            fun walk(dir: File) {
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (f.name.startsWith(".")) continue
                    if (f.isDirectory) {
                        walk(f)
                    } else if (f.length() >= minSizeBytes) {
                        results.add(FileItem.fromFile(f))
                    }
                }
            }

            walk(root)
            results.sortedByDescending { it.sizeBytes }.take(limit)
        }

    suspend fun scanJunk(onProgress: (String) -> Unit = {}): List<JunkItem> = withContext(Dispatchers.IO) {
        val junkList = mutableListOf<JunkItem>()
        val root = getDefaultStorageRoot()
        var scannedCount = 0

        fun walk(dir: File) {
            val children = dir.listFiles() ?: return
            if (children.isEmpty() && dir != root) {
                junkList.add(JunkItem(FileItem.fromFile(dir), "Empty Folder"))
                return
            }

            for (f in children) {
                scannedCount++
                if (scannedCount % 50 == 0) {
                    onProgress("Scanning: ${f.name}")
                }
                if (f.isDirectory) {
                    walk(f)
                } else {
                    val ext = f.extension.lowercase()
                    val len = f.length()
                    if (len == 0L) {
                        junkList.add(JunkItem(FileItem.fromFile(f), "Zero-byte File"))
                    } else if (ext in listOf("tmp", "temp", "cache", "bak", "swp", "~")) {
                        junkList.add(JunkItem(FileItem.fromFile(f), "Temporary Cache"))
                    } else if (ext == "log" && len > 500 * 1024L) {
                        junkList.add(JunkItem(FileItem.fromFile(f), "Large Log File"))
                    } else if (ext == "apk" && f.name.contains("download", ignoreCase = true)) {
                        junkList.add(JunkItem(FileItem.fromFile(f), "Downloaded APK"))
                    }
                }
            }
        }

        walk(root)
        junkList
    }

    suspend fun scanDuplicates(
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val root = getDefaultStorageRoot()
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        var filesFound = 0

        // Step 1: Fast size grouping
        fun collectFiles(dir: File) {
            val children = dir.listFiles() ?: return
            for (f in children) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    collectFiles(f)
                } else {
                    val len = f.length()
                    if (len > 1024L) { // Ignore empty or tiny files under 1KB
                        filesFound++
                        val list = sizeMap.getOrPut(len) { mutableListOf() }
                        list.add(f)
                    }
                }
            }
        }

        collectFiles(root)

        // Step 2: Candidates with same size (> 1 file with identical byte size)
        val candidates = sizeMap.filter { it.value.size > 1 }
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        var processed = 0
        val totalCandidates = candidates.values.sumOf { it.size }

        for ((size, fileList) in candidates) {
            // Group by actual SHA-256 content hash
            val hashMap = mutableMapOf<String, MutableList<File>>()
            for (f in fileList) {
                processed++
                onProgress((processed * 100 / totalCandidates.coerceAtLeast(1)), f.name)
                val hash = CryptoEngine.computeHash(f, "SHA-256")
                if (hash.isNotEmpty()) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(f)
                }
            }

            for ((hash, matchingFiles) in hashMap) {
                if (matchingFiles.size > 1) {
                    duplicateGroups.add(
                        DuplicateGroup(
                            sizeBytes = size,
                            hashSha256 = hash,
                            files = matchingFiles.map { FileItem.fromFile(it) }
                        )
                    )
                }
            }
        }

        duplicateGroups.sortedByDescending { it.sizeBytes * (it.files.size - 1) }
    }

    // File manipulation methods
    suspend fun createFolder(parent: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val sanitized = name.trim().replace("/", "").replace("\\", "")
        if (sanitized.isEmpty()) return@withContext false
        val newFolder = File(parent, sanitized)
        if (newFolder.exists()) return@withContext false
        newFolder.mkdirs()
    }

    suspend fun createTextFile(parent: File, name: String, content: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val sanitized = name.trim()
            if (sanitized.isEmpty()) return@withContext false
            val newFile = File(parent, sanitized)
            if (newFile.exists()) return@withContext false
            try {
                newFile.writeText(content)
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun renameFile(target: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val sanitized = newName.trim()
        if (sanitized.isEmpty() || sanitized == target.name) return@withContext false
        val dest = File(target.parentFile, sanitized)
        if (dest.exists()) return@withContext false
        target.renameTo(dest)
    }

    suspend fun deleteFileOrDirectory(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext true
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    suspend fun copyFileOrDirectory(source: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists() || !targetDir.exists() || !targetDir.isDirectory) return@withContext false
        val dest = File(targetDir, source.name)
        try {
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveFileOrDirectory(source: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists() || !targetDir.exists() || !targetDir.isDirectory) return@withContext false
        val dest = File(targetDir, source.name)
        if (source.renameTo(dest)) return@withContext true
        // Fallback for cross-filesystem moves
        if (copyFileOrDirectory(source, targetDir)) {
            deleteFileOrDirectory(source)
            true
        } else {
            false
        }
    }
}
