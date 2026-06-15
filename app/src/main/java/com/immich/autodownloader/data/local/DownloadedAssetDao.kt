package com.immich.autodownloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedAssetDao {
    @Query("SELECT * FROM downloaded_assets WHERE asset_id = :assetId LIMIT 1")
    suspend fun findByAssetId(assetId: String): DownloadedAssetEntity?

    @Query("SELECT COUNT(*) FROM downloaded_assets")
    fun observeDownloadedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: DownloadedAssetEntity)

    @Query("DELETE FROM downloaded_assets")
    suspend fun clear()
}
