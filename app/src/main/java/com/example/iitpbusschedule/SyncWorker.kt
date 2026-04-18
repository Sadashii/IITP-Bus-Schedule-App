package com.example.iitpbusschedule

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.iitpbusschedule.repository.BusRepository

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = BusRepository(applicationContext)

        return try {
            repository.getSchedule() // This fetches and caches automatically
            BusWidgetProvider.triggerUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
