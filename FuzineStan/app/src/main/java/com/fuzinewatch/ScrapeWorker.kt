package com.fuzinewatch

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fuzinewatch.data.ScrapeSettingsStore
import com.fuzinewatch.scraper.ScrapeRunner
import java.util.concurrent.TimeUnit

class ScrapeWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        if (ScrapeSettingsStore.get(applicationContext).read().isQuietNow()) {
            Log.i(ScrapeRunner.TAG, "periodic scrape skipped: quiet hours")
            return@runCatching
        }
        ScrapeRunner.scrapeAll(applicationContext)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )

    companion object {
        private const val WORK_NAME = "periodic_scrape"

        fun enqueue(context: Context) {
            val interval = ScrapeSettingsStore.get(context).read().intervalMinutes
            val request = PeriodicWorkRequestBuilder<ScrapeWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
