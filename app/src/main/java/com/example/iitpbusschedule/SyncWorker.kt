package com.example.iitpbusschedule

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.iitpbusschedule.repository.BusRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: BusRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            repository.getSchedule() // This fetches and caches automatically
            BusWidgetProvider.triggerUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
