package com.immich.autodownloader.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.immich.autodownloader.ImmichSyncApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ImmichSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ImmichSyncApp
        val settings = app.container.preferences.settings.first()
        if (!settings.autoSync) {
            return Result.success()
        }

        return try {
            app.container.repository.runSync(trigger = "background")
            schedule(
                context = applicationContext,
                wifiOnly = settings.wifiOnly,
                chargeOnly = settings.chargeOnly,
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "immich-periodic-sync"
        const val FIXED_INTERVAL_MINUTES = 1L

        fun schedule(context: Context, wifiOnly: Boolean, chargeOnly: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(chargeOnly)
                .build()

            val request = OneTimeWorkRequestBuilder<ImmichSyncWorker>()
                .setInitialDelay(FIXED_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
