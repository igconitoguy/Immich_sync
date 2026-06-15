package com.immich.autodownloader.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_assets")
data class DownloadedAssetEntity(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    val checksum: String?,
    @ColumnInfo(name = "original_file_name")
    val originalFileName: String,
    @ColumnInfo(name = "local_path")
    val localPath: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long,
    val status: String,
)
