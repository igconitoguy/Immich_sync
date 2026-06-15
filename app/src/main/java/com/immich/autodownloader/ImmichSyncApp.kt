package com.immich.autodownloader

import android.app.Application
import androidx.room.Room
import com.immich.autodownloader.data.local.AppDatabase
import com.immich.autodownloader.data.remote.ImmichApiFactory
import com.immich.autodownloader.data.repo.ImmichRepository
import com.immich.autodownloader.data.settings.SyncPreferences

class ImmichSyncApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "immich_sync.db",
        ).build()

        val preferences = SyncPreferences(applicationContext)
        container = AppContainer(
            preferences = preferences,
            repository = ImmichRepository(
                context = applicationContext,
                apiFactory = ImmichApiFactory(),
                database = database,
                preferences = preferences,
            ),
        )
    }
}

data class AppContainer(
    val preferences: SyncPreferences,
    val repository: ImmichRepository,
)
