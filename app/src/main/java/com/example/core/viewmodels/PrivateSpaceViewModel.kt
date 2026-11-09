package com.example.core.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.VaultItemEntity
import com.example.core.database.VaultNoteEntity
import com.example.core.database.WfmvDatabase
import com.example.core.security.CryptoEngine
import com.example.core.storage.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.crypto.SecretKey

data class DecryptedNote(
    val entity: VaultNoteEntity,
    val decryptedContent: String
)

class PrivateSpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WfmvDatabase.getInstance(application)
    private val dao = db.wfmvDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("wfmv_vault_prefs", Context.MODE_PRIVATE)

    private val vaultStorageDir = File(application.filesDir, "vault_files").apply { mkdirs() }

    // Vault Authentication State
    private val _isPinConfigured = MutableStateFlow(prefs.contains("vault_pin_hash"))
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _currentVaultId = MutableStateFlow("personal_sanctuary")
    val currentVaultId: StateFlow<String> = _currentVaultId.asStateFlow()

    private var activeSecretKey: SecretKey? = null

    // Recovery Key
    private val _recoveryKey = MutableStateFlow(prefs.getString("vault_recovery_key", "") ?: "")
    val recoveryKey: StateFlow<String> = _recoveryKey.asStateFlow()

    // Vault Items
    val vaultItems: StateFlow<List<VaultItemEntity>> = _currentVaultId.flatMapLatest { id ->
        dao.getVaultItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Notes
    val vaultNotes: StateFlow<List<VaultNoteEntity>> = _currentVaultId.flatMapLatest { id ->
        dao.getVaultNotes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Decrypted Note Cache for viewing/editing
    private val _activeDecryptedNote = MutableStateFlow<DecryptedNote?>(null)
    val activeDecryptedNote: StateFlow<DecryptedNote?> = _activeDecryptedNote.asStateFlow()

    // Feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun switchVault(vaultId: String) {
        _currentVaultId.value = vaultId
    }

    /**
     * Set up a new PIN and generate the vault recovery key
     */
    fun setupPin(pin: String): Boolean {
        if (pin.length < 4) {
            _statusMessage.value = "PIN must be at least 4 digits"
            return false
        }
        val key = CryptoEngine.deriveKey(pin)
        activeSecretKey = key

        // Save hashed PIN
        val hash = CryptoEngine.computeHash(
            File.createTempFile("pin", ".tmp").apply {
                writeText(pin)
                deleteOnExit()
            }
        )
        val recKey = CryptoEngine.generateRecoveryKey()

        prefs.edit()
            .putString("vault_pin_hash", hash)
            .putString("vault_recovery_key", recKey)
            .apply()

        _isPinConfigured.value = true
        _recoveryKey.value = recKey
        _isUnlocked.value = true
        _statusMessage.value = "Private Space secured. Save your recovery key!"
        return true
    }

    /**
     * Unlock vault using PIN
     */
    fun unlockWithPin(pin: String): Boolean {
        val storedHash = prefs.getString("vault_pin_hash", null) ?: return false
        val temp = File.createTempFile("chk", ".tmp").apply {
            writeText(pin)
            deleteOnExit()
        }
        val currentHash = CryptoEngine.computeHash(temp)
        temp.delete()

        return if (storedHash == currentHash) {
            activeSecretKey = CryptoEngine.deriveKey(pin)
            _isUnlocked.value = true
            _statusMessage.value = "Welcome to Wafa Private Space"
            true
        } else {
            _statusMessage.value = "Incorrect PIN. Access Denied."
            false
        }
    }

    /**
     * Recover vault access using 16-character recovery key
     */
    fun recoverWithKey(enteredKey: String, newPin: String): Boolean {
        val storedKey = prefs.getString("vault_recovery_key", "") ?: ""
        val sanitized = enteredKey.trim().uppercase()
        if (sanitized == storedKey && newPin.length >= 4) {
            return setupPin(newPin)
        }
        _statusMessage.value = "Invalid Recovery Key"
        return false
    }

    fun lockVault() {
        activeSecretKey = null
        _isUnlocked.value = false
        _activeDecryptedNote.value = null
        _statusMessage.value = "Private Space Locked"
    }

    /**
     * Encrypts and imports an external file into the private vault.
     */
    fun importFileToVault(sourceFile: File, category: String = "PHOTO") {
        val key = activeSecretKey
        if (key == null) {
            _statusMessage.value = "Vault is locked"
            return
        }

        viewModelScope.launch {
            try {
                val encFileName = "enc_${UUID.randomUUID()}.bin"
                val destFile = File(vaultStorageDir, encFileName)
                val iv = CryptoEngine.encryptFile(sourceFile, destFile, key)
                val ivBase64 = CryptoEngine.base64Encode(iv)

                val entity = VaultItemEntity(
                    vaultId = _currentVaultId.value,
                    originalName = sourceFile.name,
                    encryptedFileName = encFileName,
                    mimeType = FileItem.resolveMimeType(sourceFile),
                    sizeBytes = sourceFile.length(),
                    ivBase64 = ivBase64,
                    category = category
                )
                dao.insertVaultItem(entity)

                // Safely delete original
                sourceFile.delete()
                _statusMessage.value = "Imported & encrypted: ${entity.originalName}"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to encrypt file: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Decrypts and exports a vault item back to public/accessible storage.
     */
    fun exportFileFromVault(item: VaultItemEntity, exportDir: File) {
        val key = activeSecretKey
        if (key == null) {
            _statusMessage.value = "Vault is locked"
            return
        }

        viewModelScope.launch {
            try {
                val encFile = File(vaultStorageDir, item.encryptedFileName)
                val destFile = File(exportDir, item.originalName)
                val iv = CryptoEngine.base64Decode(item.ivBase64)

                CryptoEngine.decryptFile(encFile, destFile, key, iv)
                encFile.delete()
                dao.deleteVaultItem(item)
                _statusMessage.value = "Restored: ${item.originalName}"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to decrypt file: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Permanently deletes a vault item
     */
    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            val encFile = File(vaultStorageDir, item.encryptedFileName)
            if (encFile.exists()) encFile.delete()
            dao.deleteVaultItem(item)
            _statusMessage.value = "Permanently deleted: ${item.originalName}"
        }
    }

    // Vault Notes
    fun createOrUpdateNote(id: Long = 0, title: String, content: String, category: String = "General") {
        val key = activeSecretKey
        if (key == null) {
            _statusMessage.value = "Vault is locked"
            return
        }

        viewModelScope.launch {
            try {
                val (cipherBase64, ivBase64) = CryptoEngine.encryptString(content, key)
                val note = VaultNoteEntity(
                    id = id,
                    vaultId = _currentVaultId.value,
                    title = title.ifBlank { "Untitled Note" },
                    encryptedContentBase64 = cipherBase64,
                    ivBase64 = ivBase64,
                    category = category,
                    updatedAt = System.currentTimeMillis()
                )
                if (id == 0L) {
                    dao.insertVaultNote(note)
                    _statusMessage.value = "Encrypted Note Created"
                } else {
                    dao.updateVaultNote(note)
                    _statusMessage.value = "Encrypted Note Updated"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error saving note: ${e.localizedMessage}"
            }
        }
    }

    fun openNote(note: VaultNoteEntity) {
        val key = activeSecretKey ?: return
        try {
            val plain = CryptoEngine.decryptString(note.encryptedContentBase64, note.ivBase64, key)
            _activeDecryptedNote.value = DecryptedNote(note, plain)
        } catch (e: Exception) {
            _statusMessage.value = "Decryption error for note"
        }
    }

    fun closeActiveNote() {
        _activeDecryptedNote.value = null
    }

    fun deleteNote(note: VaultNoteEntity) {
        viewModelScope.launch {
            dao.deleteVaultNote(note)
            if (_activeDecryptedNote.value?.entity?.id == note.id) {
                _activeDecryptedNote.value = null
            }
            _statusMessage.value = "Deleted note: ${note.title}"
        }
    }
}
