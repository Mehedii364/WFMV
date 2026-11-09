package com.example.core.storage

import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    OTHER,
    FOLDER
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val sizeBytes: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val extension: String = file.extension.lowercase(Locale.ROOT),
    val mimeType: String = resolveMimeType(file),
    val category: FileCategory = resolveCategory(file),
    val childCount: Int = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedDate: String
        get() = formatDate(lastModified)

    companion object {
        fun fromFile(file: File): FileItem {
            return FileItem(file)
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val clamped = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, clamped.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[clamped])
        }

        fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        fun resolveMimeType(file: File): String {
            if (file.isDirectory) return "inode/directory"
            val ext = file.extension.lowercase(Locale.ROOT)
            val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!fromMap.isNullOrEmpty()) return fromMap
            return when (ext) {
                "json" -> "application/json"
                "xml" -> "text/xml"
                "md" -> "text/markdown"
                "apk" -> "application/vnd.android.package-archive"
                "zip" -> "application/zip"
                "tar", "gz", "tgz", "7z", "rar" -> "application/x-archive"
                "pdf" -> "application/pdf"
                "txt", "log", "kt", "java", "py", "c", "cpp", "h", "html", "css", "js", "ts" -> "text/plain"
                else -> "application/octet-stream"
            }
        }

        fun resolveCategory(file: File): FileCategory {
            if (file.isDirectory) return FileCategory.FOLDER
            val ext = file.extension.lowercase(Locale.ROOT)
            return when (ext) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> FileCategory.IMAGE
                "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts" -> FileCategory.VIDEO
                "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus" -> FileCategory.AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "html" -> FileCategory.DOCUMENT
                "zip", "tar", "gz", "tgz", "rar", "7z", "bz2" -> FileCategory.ARCHIVE
                "apk", "xapk", "apks" -> FileCategory.APK
                else -> FileCategory.OTHER
            }
        }
    }
}
