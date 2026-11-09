package com.example.core.storage

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.core.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

data class HexRow(
    val offset: String,
    val hexPart: String,
    val asciiPart: String
)

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>
)

data class FileDiagnostics(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val detectedSignature: String,
    val extensionMatchesSignature: Boolean,
    val sha256: String,
    val sha1: String,
    val md5: String,
    val lineCount: Int?,
    val wordCount: Int?,
    val isJsonValid: Boolean?,
    val isXmlValid: Boolean?
)

object DeveloperToolsHelper {

    suspend fun analyzeFile(file: File): FileDiagnostics = withContext(Dispatchers.IO) {
        val sha256 = CryptoEngine.computeHash(file, "SHA-256")
        val sha1 = CryptoEngine.computeHash(file, "SHA-1")
        val md5 = CryptoEngine.computeHash(file, "MD5")
        val mime = FileItem.resolveMimeType(file)
        val sig = detectMagicSignature(file)
        val extMatch = checkExtensionMatch(file.extension.lowercase(Locale.ROOT), sig)

        var lineCount: Int? = null
        var wordCount: Int? = null
        var jsonValid: Boolean? = null
        var xmlValid: Boolean? = null

        // Check if text/json/xml readable
        if (file.length() < 2 * 1024 * 1024L) { // Only read up to 2MB for text checks
            try {
                val text = file.readText(StandardCharsets.UTF_8)
                val lines = text.lines()
                lineCount = lines.size
                wordCount = text.split("\\s+".toRegex()).count { it.isNotBlank() }

                val trimmed = text.trim()
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    jsonValid = try {
                        if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONArray(trimmed)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                    xmlValid = try {
                        val factory = DocumentBuilderFactory.newInstance()
                        val builder = factory.newDocumentBuilder()
                        builder.parse(org.xml.sax.InputSource(StringReader(trimmed)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            } catch (e: Exception) {
                // Not UTF-8 or binary
            }
        }

        FileDiagnostics(
            fileName = file.name,
            absolutePath = file.absolutePath,
            sizeBytes = file.length(),
            mimeType = mime,
            detectedSignature = sig,
            extensionMatchesSignature = extMatch,
            sha256 = sha256,
            sha1 = sha1,
            md5 = md5,
            lineCount = lineCount,
            wordCount = wordCount,
            isJsonValid = jsonValid,
            isXmlValid = xmlValid
        )
    }

    suspend fun generateHexDump(file: File, maxBytes: Int = 1024): List<HexRow> =
        withContext(Dispatchers.IO) {
            val rows = mutableListOf<HexRow>()
            if (!file.exists() || file.isDirectory) return@withContext emptyList()

            val buffer = ByteArray(maxBytes)
            val bytesRead = try {
                FileInputStream(file).use { it.read(buffer) }
            } catch (e: Exception) {
                -1
            }
            if (bytesRead <= 0) return@withContext emptyList()

            for (i in 0 until bytesRead step 16) {
                val chunkLen = Math.min(16, bytesRead - i)
                val offset = String.format(Locale.US, "%08X", i)

                val hexSb = java.lang.StringBuilder()
                val asciiSb = java.lang.StringBuilder()

                for (j in 0 until 16) {
                    if (j < chunkLen) {
                        val b = buffer[i + j].toInt() and 0xFF
                        hexSb.append(String.format(Locale.US, "%02X ", b))
                        if (b in 32..126) {
                            asciiSb.append(b.toChar())
                        } else {
                            asciiSb.append('.')
                        }
                    } else {
                        hexSb.append("   ")
                    }
                    if (j == 7) hexSb.append(" ")
                }

                rows.add(HexRow(offset, hexSb.toString(), asciiSb.toString()))
            }
            rows
        }

    fun inspectApk(context: Context, apkFile: File): ApkMetadata? {
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS
            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return null
            val appInfo = pkgInfo.applicationInfo ?: return null

            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21
            val targetSdk = appInfo.targetSdkVersion
            val perms = pkgInfo.requestedPermissions?.toList() ?: emptyList()

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            ApkMetadata(
                packageName = pkgInfo.packageName ?: "Unknown",
                versionName = pkgInfo.versionName ?: "1.0",
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = targetSdk,
                permissions = perms
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun detectMagicSignature(file: File): String {
        if (!file.exists() || file.isDirectory || file.length() < 4) return "Unknown / Empty"
        val header = ByteArray(8)
        val read = try {
            FileInputStream(file).use { it.read(header) }
        } catch (e: Exception) {
            return "Unreadable"
        }
        if (read < 4) return "Unknown"

        // Check well-known magic numbers
        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
            return "PNG Image (89 50 4E 47)"
        }
        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "JPEG Image (FF D8 FF)"
        }
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
            return "ZIP / APK / DOCX Archive (50 4B 03 04)"
        }
        if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
            return "PDF Document (%PDF / 25 50 44 46)"
        }
        if (header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte()) {
            return "GIF Image (GIF87a / GIF89a)"
        }
        if (header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
            return "GZIP Compressed (1F 8B)"
        }
        if (header[0] == 0x37.toByte() && header[1] == 0x7A.toByte() && header[2] == 0xBC.toByte() && header[3] == 0xAF.toByte()) {
            return "7-Zip Archive (37 7A BC AF)"
        }
        if (header[0] == 0x52.toByte() && header[1] == 0x61.toByte() && header[2] == 0x72.toByte() && header[3] == 0x21.toByte()) {
            return "RAR Archive (52 61 72 21)"
        }
        if (header[0] == 0x49.toByte() && header[1] == 0x44.toByte() && header[2] == 0x33.toByte()) {
            return "MP3 with ID3v2 tag (49 44 33)"
        }
        if (header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && header[6] == 0x79.toByte() && header[7] == 0x70.toByte()) {
            return "MP4 / ISO Base Media (ftyp)"
        }
        return "Generic Binary / Plain Text"
    }

    private fun checkExtensionMatch(ext: String, signature: String): Boolean {
        return when {
            signature.contains("PNG") -> ext in listOf("png")
            signature.contains("JPEG") -> ext in listOf("jpg", "jpeg")
            signature.contains("ZIP") -> ext in listOf("zip", "apk", "docx", "xlsx", "pptx", "jar")
            signature.contains("PDF") -> ext in listOf("pdf")
            signature.contains("GIF") -> ext in listOf("gif")
            signature.contains("GZIP") -> ext in listOf("gz", "tgz")
            signature.contains("7-Zip") -> ext in listOf("7z")
            signature.contains("RAR") -> ext in listOf("rar")
            signature.contains("MP3") -> ext in listOf("mp3")
            signature.contains("MP4") -> ext in listOf("mp4", "m4a", "mov")
            else -> true
        }
    }
}
