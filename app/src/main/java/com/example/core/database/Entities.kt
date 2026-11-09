package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val mimeType: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isDirectory: Boolean,
    val openedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultId: String = "default_vault",
    val originalName: String,
    val encryptedFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val ivBase64: String,
    val category: String, // "PHOTO", "VIDEO", "DOCUMENT", "AUDIO", "OTHER"
    val isFavorite: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_notes")
data class VaultNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultId: String = "default_vault",
    val title: String,
    val encryptedContentBase64: String,
    val ivBase64: String,
    val category: String = "General",
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clean_history")
data class CleanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bytesCleaned: Long,
    val itemCount: Int,
    val categoryLabel: String
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val duplicatesFound: Int,
    val duplicateWasteBytes: Long,
    val scanType: String
)
