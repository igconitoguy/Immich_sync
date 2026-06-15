package com.immich.autodownloader.data.repo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.immich.autodownloader.data.local.AppDatabase
import com.immich.autodownloader.data.local.DownloadedAssetEntity
import com.immich.autodownloader.data.local.SyncLogEntity
import com.immich.autodownloader.data.remote.ImmichApiFactory
import com.immich.autodownloader.data.remote.SearchAssetsRequest
import com.immich.autodownloader.data.settings.DownloadFolder
import com.immich.autodownloader.data.settings.SyncPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.IOException

class ImmichRepository(
    private val context: Context,
    private val apiFactory: ImmichApiFactory,
    private val database: AppDatabase,
    private val preferences: SyncPreferences,
) {
    val logs: Flow<List<SyncLogEntity>> = database.syncLogDao().observeLogs()

    fun observeHomeSummary(): Flow<HomeSummary> {
        return combine(
            database.downloadedAssetDao().observeDownloadedCount(),
            preferences.settings,
        ) { downloadedCount, settings ->
            HomeSummary(
                downloadedCount = downloadedCount,
                pendingCount = settings.lastKnownServerTotal?.minus(downloadedCount)?.coerceAtLeast(0) ?: 0,
                totalAssets = settings.lastKnownServerTotal ?: 0,
                lastSyncTime = settings.lastSyncTime,
                lastMessage = settings.lastSyncMessage,
            )
        }
    }

    suspend fun testConnection(serverUrl: String, apiKey: String): Result<Int> = runCatching {
        val response = apiFactory.create(serverUrl, apiKey).searchAssets(SearchAssetsRequest(size = 1))
        response.assets.total
    }

    suspend fun runSync(trigger: String = "manual"): SyncRunResult {
        return withContext(Dispatchers.IO) {
            val settings = preferences.settings.first()
            if (settings.serverUrl.isBlank() || settings.apiKey.isBlank()) {
                appendLog("Sync skipped: missing server URL or API key", "WARN")
                return@withContext SyncRunResult(skipped = true, reason = "Missing credentials")
            }

            val api = apiFactory.create(settings.serverUrl, settings.apiKey)
            appendLog("Starting $trigger sync", "INFO")

            val page = api.searchAssets(SearchAssetsRequest(size = 1000))
            val assets = page.assets.items
            preferences.updateServerTotal(page.assets.total)

            var downloaded = 0
            var failed = 0

            for (asset in assets) {
                val existing = database.downloadedAssetDao().findByAssetId(asset.id)
                if (existing != null && existing.status == STATUS_COMPLETE) {
                    continue
                }

                try {
                    val responseBody = api.downloadOriginal(asset.id)
                    val mimeType = guessMimeType(asset.originalFileName)
                    val target = saveToMediaStore(
                        displayName = asset.originalFileName,
                        relativeFolder = targetRelativeFolder(
                            preferredFolder = settings.downloadFolder.relativePath,
                            mimeType = mimeType,
                        ),
                        mimeType = mimeType,
                        body = responseBody,
                    )

                    database.downloadedAssetDao().upsert(
                        DownloadedAssetEntity(
                            assetId = asset.id,
                            checksum = asset.checksum,
                            originalFileName = asset.originalFileName,
                            localPath = target,
                            fileSize = asset.exifInfo?.fileSizeInByte ?: responseBody.contentLength().coerceAtLeast(0L),
                            downloadedAt = System.currentTimeMillis(),
                            status = STATUS_COMPLETE,
                        ),
                    )
                    scanFile(target)
                    downloaded += 1
                    appendLog("Downloaded ${asset.originalFileName}", "SUCCESS")
                } catch (error: Exception) {
                    failed += 1
                    database.downloadedAssetDao().upsert(
                        DownloadedAssetEntity(
                            assetId = asset.id,
                            checksum = asset.checksum,
                            originalFileName = asset.originalFileName,
                            localPath = "",
                            fileSize = asset.exifInfo?.fileSizeInByte ?: 0L,
                            downloadedAt = System.currentTimeMillis(),
                            status = STATUS_FAILED,
                        ),
                    )
                    appendLog("Failed ${asset.originalFileName}: ${error.message}", "ERROR")
                }
            }

            val message = "Sync finished: $downloaded downloaded, $failed failed"
            preferences.updateLastSync(System.currentTimeMillis(), message)
            appendLog(message, "INFO")

            SyncRunResult(
                totalAssets = page.assets.total,
                downloaded = downloaded,
                failed = failed,
            )
        }
    }

    suspend fun clearLocalDatabase() {
        database.downloadedAssetDao().clear()
        database.syncLogDao().clear()
        preferences.updateServerTotal(0)
        preferences.updateLastSync(null, "Local database cleared")
    }

    suspend fun saveSettings(newSettings: SyncSettingsInput) {
        preferences.updateSettings(newSettings)
    }

    suspend fun appendLog(message: String, level: String) {
        database.syncLogDao().insert(
            SyncLogEntity(
                message = message,
                level = level,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun saveToMediaStore(
        displayName: String,
        relativeFolder: String,
        mimeType: String,
        body: ResponseBody,
    ): String {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collection = mediaCollectionFor(mimeType)
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("Unable to create MediaStore item")

        try {
            body.use { responseBody ->
                resolver.openOutputStream(uri)?.use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                    }
                } ?: throw IOException("Unable to open output stream")
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return uri.toString()
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun mediaCollectionFor(mimeType: String): Uri {
        return when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.getContentUri("external")
            mimeType.startsWith("video/") -> MediaStore.Video.Media.getContentUri("external")
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
    }

    private fun targetRelativeFolder(preferredFolder: String, mimeType: String): String {
        return when {
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> preferredFolder
            else -> "Download/ImmichSync"
        }
    }

    private fun scanFile(uriString: String) {
        context.contentResolver.notifyChange(Uri.parse(uriString), null)
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".heic") -> "image/heic"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".mov") -> "video/quicktime"
            else -> "application/octet-stream"
        }
    }

    companion object {
        private const val STATUS_COMPLETE = "COMPLETED"
        private const val STATUS_FAILED = "FAILED"
    }
}

data class HomeSummary(
    val downloadedCount: Int = 0,
    val pendingCount: Int = 0,
    val totalAssets: Int = 0,
    val lastSyncTime: Long? = null,
    val lastMessage: String? = null,
)

data class SyncRunResult(
    val totalAssets: Int = 0,
    val downloaded: Int = 0,
    val failed: Int = 0,
    val skipped: Boolean = false,
    val reason: String? = null,
)

data class SyncSettingsInput(
    val serverUrl: String,
    val apiKey: String,
    val downloadFolder: DownloadFolder,
    val wifiOnly: Boolean,
    val chargeOnly: Boolean,
    val autoSync: Boolean,
    val intervalMinutes: Int,
)
