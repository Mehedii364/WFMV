package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        VaultItemEntity::class,
        VaultNoteEntity::class,
        CleanHistoryEntity::class,
        ScanHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WfmvDatabase : RoomDatabase() {
    abstract fun wfmvDao(): WfmvDao

    companion object {
        @Volatile
        private var INSTANCE: WfmvDatabase? = null

        fun getInstance(context: Context): WfmvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WfmvDatabase::class.java,
                    "wfmv_ecosystem.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
