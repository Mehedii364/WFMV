package com.example.core.security

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val GCM_IV_LENGTH = 12 // bytes
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256

    // Salt specifically bound to the vault system
    private val VAULT_SALT = "WFMV_OBSIDIAN_VAULT_SALT_SECURE_2026".toByteArray(Charsets.UTF_8)

    fun deriveKey(pinOrPassword: String, salt: ByteArray = VAULT_SALT): SecretKey {
        val spec = PBEKeySpec(pinOrPassword.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun base64Encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (t: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    fun base64Decode(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (t: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }
    }

    /**
     * Encrypts plaintext string into AES-GCM ciphertext + Base64
     */
    fun encryptString(plainText: String, secretKey: SecretKey): Pair<String, String> {
        val iv = generateIv()
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val cipherBase64 = base64Encode(cipherBytes)
        val ivBase64 = base64Encode(iv)
        return Pair(cipherBase64, ivBase64)
    }

    /**
     * Decrypts AES-GCM ciphertext string using key and IV
     */
    fun decryptString(cipherBase64: String, ivBase64: String, secretKey: SecretKey): String {
        val cipherBytes = base64Decode(cipherBase64)
        val iv = base64Decode(ivBase64)
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Streams file encryption with AES-GCM
     */
    fun encryptFile(sourceFile: File, destFile: File, secretKey: SecretKey): ByteArray {
        val iv = generateIv()
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        FileInputStream(sourceFile).use { inStream ->
            FileOutputStream(destFile).use { outStream ->
                CipherOutputStream(outStream, cipher).use { cipherOut ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                    }
                    cipherOut.flush()
                }
            }
        }
        return iv
    }

    /**
     * Streams file decryption with AES-GCM
     */
    fun decryptFile(sourceFile: File, destFile: File, secretKey: SecretKey, iv: ByteArray) {
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        FileInputStream(sourceFile).use { inStream ->
            CipherInputStream(inStream, cipher).use { cipherIn ->
                FileOutputStream(destFile).use { outStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                    outStream.flush()
                }
            }
        }
    }

    /**
     * Computes cryptographic checksum (SHA-256, SHA-1, MD5) via streaming
     */
    fun computeHash(file: File, algorithm: String = "SHA-256"): String {
        if (!file.exists() || !file.canRead() || file.isDirectory) return ""
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val bytes = digest.digest()
        val sb = java.lang.StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Generates a 16-character secure recovery key formatted as WFMV-XXXX-XXXX-XXXX
     */
    fun generateRecoveryKey(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude confusing chars O, 0, 1, I
        val random = SecureRandom()
        val part1 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
        val part3 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return "WFMV-$part1-$part2-$part3"
    }
}
