package com.immich.autodownloader.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE
import androidx.work.ExistingWorkPolicy.KEEP
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
            queueNextRun(
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

        fun ensureScheduled(context: Context, wifiOnly: Boolean, chargeOnly: Boolean) {
            enqueue(
                context = context,
                wifiOnly = wifiOnly,
                chargeOnly = chargeOnly,
                policy = KEEP,
            )
        }

        fun queueNextRun(context: Context, wifiOnly: Boolean, chargeOnly: Boolean) {
            enqueue(
                context = context,
                wifiOnly = wifiOnly,
                chargeOnly = chargeOnly,
                policy = APPEND_OR_REPLACE,
            )
        }

        private fun enqueue(
            context: Context,
            wifiOnly: Boolean,
            chargeOnly: Boolean,
            policy: ExistingWorkPolicy,
        ) {
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
                policy,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
