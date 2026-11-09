package com.example.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WfmvDao {
    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun deleteFavoriteByPath(path: String)

    // Recents
    @Query("SELECT * FROM recents ORDER BY openedAt DESC LIMIT 20")
    fun getRecentFiles(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentEntity)

    @Query("DELETE FROM recents WHERE path = :path")
    suspend fun deleteRecent(path: String)

    @Query("DELETE FROM recents")
    suspend fun clearRecents()

    // Vault Items
    @Query("SELECT * FROM vault_items WHERE vaultId = :vaultId ORDER BY addedTimestamp DESC")
    fun getVaultItems(vaultId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE vaultId = :vaultId AND category = :category ORDER BY addedTimestamp DESC")
    fun getVaultItemsByCategory(vaultId: String, category: String): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity): Long

    @Delete
    suspend fun deleteVaultItem(item: VaultItemEntity)

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Long): VaultItemEntity?

    @Query("SELECT COUNT(*) FROM vault_items WHERE vaultId = :vaultId")
    suspend fun getVaultItemCount(vaultId: String): Int

    @Query("SELECT SUM(sizeBytes) FROM vault_items WHERE vaultId = :vaultId")
    suspend fun getVaultTotalBytes(vaultId: String): Long?

    // Vault Notes
    @Query("SELECT * FROM vault_notes WHERE vaultId = :vaultId ORDER BY updatedAt DESC")
    fun getVaultNotes(vaultId: String): Flow<List<VaultNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultNote(note: VaultNoteEntity): Long

    @Update
    suspend fun updateVaultNote(note: VaultNoteEntity)

    @Delete
    suspend fun deleteVaultNote(note: VaultNoteEntity)

    // Clean History
    @Query("SELECT * FROM clean_history ORDER BY timestamp DESC LIMIT 20")
    fun getCleanHistory(): Flow<List<CleanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanRecord(record: CleanHistoryEntity)

    @Query("SELECT SUM(bytesCleaned) FROM clean_history")
    suspend fun getTotalCleanedBytes(): Long?

    // Scan History
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun getScanHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: ScanHistoryEntity)
}
