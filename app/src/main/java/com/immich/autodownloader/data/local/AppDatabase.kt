package com.immich.autodownloader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadedAssetEntity::class, SyncLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedAssetDao(): DownloadedAssetDao
    abstract fun syncLogDao(): SyncLogDao
}
