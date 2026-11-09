package com.example

import com.example.core.security.CryptoEngine
import com.example.core.storage.FileCategory
import com.example.core.storage.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun testByteFormatting() {
        assertEquals("0 B", FileItem.formatBytes(0L))
        assertEquals("1.0 KB", FileItem.formatBytes(1024L))
        assertEquals("5.0 MB", FileItem.formatBytes(5 * 1024 * 1024L))
        assertEquals("1.5 GB", FileItem.formatBytes((1.5 * 1024 * 1024 * 1024L).toLong()))
    }

    @Test
    fun testFileCategoryResolution() {
        val imgFile = File("photo.png")
        assertEquals(FileCategory.IMAGE, FileItem.resolveCategory(imgFile))

        val vidFile = File("movie.mp4")
        assertEquals(FileCategory.VIDEO, FileItem.resolveCategory(vidFile))

        val audioFile = File("song.mp3")
        assertEquals(FileCategory.AUDIO, FileItem.resolveCategory(audioFile))

        val docFile = File("notes.pdf")
        assertEquals(FileCategory.DOCUMENT, FileItem.resolveCategory(docFile))

        val zipFile = File("backup.zip")
        assertEquals(FileCategory.ARCHIVE, FileItem.resolveCategory(zipFile))

        val apkFile = File("app.apk")
        assertEquals(FileCategory.APK, FileItem.resolveCategory(apkFile))
    }

    @Test
    fun testRecoveryKeyFormat() {
        val key = CryptoEngine.generateRecoveryKey()
        assertTrue("Key should start with WFMV-", key.startsWith("WFMV-"))
        val parts = key.split("-")
        assertEquals("Should have 4 segments", 4, parts.size)
        assertEquals("WFMV", parts[0])
        assertEquals(4, parts[1].length)
        assertEquals(4, parts[2].length)
        assertEquals(4, parts[3].length)
    }

    @Test
    fun testCryptoEngineStringRoundTrip() {
        val masterKey = CryptoEngine.deriveKey("1234")
        val secretMessage = "WFMV Confidential Vault Data 2026"

        val (cipherBase64, ivBase64) = CryptoEngine.encryptString(secretMessage, masterKey)
        assertNotEquals(secretMessage, cipherBase64)

        val decrypted = CryptoEngine.decryptString(cipherBase64, ivBase64, masterKey)
        assertEquals(secretMessage, decrypted)
    }

    @Test
    fun testCryptoEngineStreamingFileHash() {
        val temp = File.createTempFile("test_hash", ".txt")
        temp.writeText("WFMV Ecosystem Test File")
        temp.deleteOnExit()

        val sha256 = CryptoEngine.computeHash(temp, "SHA-256")
        assertTrue("SHA-256 should be 64 hex characters", sha256.length == 64)

        val md5 = CryptoEngine.computeHash(temp, "MD5")
        assertTrue("MD5 should be 32 hex characters", md5.length == 32)
    }
}
