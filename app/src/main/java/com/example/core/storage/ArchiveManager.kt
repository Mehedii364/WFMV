package com.example.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ZipEntryItem(
    val name: String,
    val sizeBytes: Long,
    val compressedSizeBytes: Long,
    val isDirectory: Boolean
)

object ArchiveManager {

    /**
     * Creates a ZIP archive containing the specified files/directories.
     */
    suspend fun createZip(
        sources: List<File>,
        zipDestination: File,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (zipDestination.exists()) zipDestination.delete()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipDestination))).use { zos ->
                for (source in sources) {
                    compressRecursive(source, source.name, zos, onProgress)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compressRecursive(
        fileToZip: File,
        baseName: String,
        zos: ZipOutputStream,
        onProgress: (String) -> Unit
    ) {
        if (!fileToZip.exists()) return
        onProgress("Archiving: ${fileToZip.name}")

        if (fileToZip.isDirectory) {
            val entryName = if (baseName.endsWith("/")) baseName else "$baseName/"
            zos.putNextEntry(ZipEntry(entryName))
            zos.closeEntry()
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                compressRecursive(child, "$baseName/${child.name}", zos, onProgress)
            }
        } else {
            val entry = ZipEntry(baseName)
            zos.putNextEntry(entry)
            FileInputStream(fileToZip).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (bis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                }
            }
            zos.closeEntry()
        }
    }

    /**
     * Safely extracts a ZIP archive to a destination directory, guarding strictly against Zip-Slip path traversal.
     */
    suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (!zipFile.exists() || !zipFile.canRead()) return@withContext false
        if (!targetDir.exists()) targetDir.mkdirs()

        val canonicalDestDir = targetDir.canonicalFile

        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val destFile = File(targetDir, entry.name)

                    // Path traversal protection (Zip-Slip vulnerability safeguard)
                    if (!destFile.canonicalPath.startsWith(canonicalDestDir.canonicalPath + File.separator) &&
                        destFile.canonicalPath != canonicalDestDir.canonicalPath
                    ) {
                        throw SecurityException("Zip Slip detected: Entry ${entry.name} escapes target directory!")
                    }

                    onProgress("Extracting: ${entry.name}")

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { inStream ->
                            FileOutputStream(destFile).use { outStream ->
                                inStream.copyTo(outStream, 8192)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Previews the contents of a ZIP file without extracting it.
     */
    suspend fun previewZip(zipFile: File): List<ZipEntryItem> = withContext(Dispatchers.IO) {
        if (!zipFile.exists() || !zipFile.canRead()) return@withContext emptyList()
        val list = mutableListOf<ZipEntryItem>()
        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    list.add(
                        ZipEntryItem(
                            name = entry.name,
                            sizeBytes = entry.size,
                            compressedSizeBytes = entry.compressedSize,
                            isDirectory = entry.isDirectory
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }
}
