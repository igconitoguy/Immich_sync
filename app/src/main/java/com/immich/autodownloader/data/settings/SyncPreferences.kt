package com.immich.autodownloader.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.immich.autodownloader.data.repo.SyncSettingsInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sync_preferences")

class SyncPreferences(private val context: Context) {
    val settings: Flow<StoredSettings> = context.dataStore.data.map { prefs ->
        StoredSettings(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            downloadFolder = DownloadFolder.fromStorageValue(prefs[Keys.DOWNLOAD_FOLDER]),
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            chargeOnly = prefs[Keys.CHARGE_ONLY] ?: false,
            autoSync = prefs[Keys.AUTO_SYNC] ?: true,
            intervalMinutes = 1,
            lastSyncTime = prefs[Keys.LAST_SYNC_TIME],
            lastSyncMessage = prefs[Keys.LAST_SYNC_MESSAGE],
            lastKnownServerTotal = prefs[Keys.LAST_KNOWN_SERVER_TOTAL],
        )
    }

    suspend fun updateSettings(input: SyncSettingsInput) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = input.serverUrl.trim()
            prefs[Keys.API_KEY] = input.apiKey.trim()
            prefs[Keys.DOWNLOAD_FOLDER] = input.downloadFolder.storageValue
            prefs[Keys.WIFI_ONLY] = input.wifiOnly
            prefs[Keys.CHARGE_ONLY] = input.chargeOnly
            prefs[Keys.AUTO_SYNC] = input.autoSync
            prefs[Keys.INTERVAL_MINUTES] = 1
        }
    }

    suspend fun updateLastSync(timeMillis: Long?, message: String?) {
        context.dataStore.edit { prefs ->
            if (timeMillis == null) {
                prefs.remove(Keys.LAST_SYNC_TIME)
            } else {
                prefs[Keys.LAST_SYNC_TIME] = timeMillis
            }

            if (message.isNullOrBlank()) {
                prefs.remove(Keys.LAST_SYNC_MESSAGE)
            } else {
                prefs[Keys.LAST_SYNC_MESSAGE] = message
            }
        }
    }

    suspend fun updateServerTotal(total: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_KNOWN_SERVER_TOTAL] = total
        }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val DOWNLOAD_FOLDER = stringPreferencesKey("download_folder")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGE_ONLY = booleanPreferencesKey("charge_only")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
        val LAST_KNOWN_SERVER_TOTAL = intPreferencesKey("last_known_server_total")
    }
}

data class StoredSettings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val downloadFolder: DownloadFolder = DownloadFolder.PICTURES,
    val wifiOnly: Boolean = true,
    val chargeOnly: Boolean = false,
    val autoSync: Boolean = true,
    val intervalMinutes: Int = 1,
    val lastSyncTime: Long? = null,
    val lastSyncMessage: String? = null,
    val lastKnownServerTotal: Int? = null,
)

enum class DownloadFolder(val storageValue: String, val relativePath: String) {
    PICTURES("pictures", "Pictures/ImmichSync"),
    DCIM("dcim", "DCIM/Camera");

    companion object {
        fun fromStorageValue(value: String?): DownloadFolder {
            return entries.firstOrNull { it.storageValue == value } ?: PICTURES
        }
    }
}
