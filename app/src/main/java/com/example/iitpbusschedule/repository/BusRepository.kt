package com.example.iitpbusschedule.repository

import android.content.Context
import com.example.iitpbusschedule.data.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class BusRepository(private val context: Context) {
    private val sheetUrl = "https://docs.google.com/spreadsheets/d/1p0WTx2O5rUEatdvpVtoQwnPEhv86_nZf5F-LMPwEe_s/export?format=csv&gid=0"
    private val prefs = context.getSharedPreferences("bus_cache", Context.MODE_PRIVATE)

    suspend fun getSchedule(): FetchResult = withContext(Dispatchers.IO) {
        var csvData = ""
        var isLive = false
        var lastUpdated = prefs.getLong("last_updated", 0L)

        try {
            // Try fetching from internet
            csvData = URL(sheetUrl).readText()
            isLive = true
            lastUpdated = System.currentTimeMillis()
            // Save to cache
            prefs.edit().putString("cached_csv", csvData).putLong("last_updated", lastUpdated).apply()
        } catch (e: Exception) {
            // Fallback to cache
            csvData = prefs.getString("cached_csv", "") ?: ""
            if (csvData.isEmpty()) {
                throw Exception("No internet connection and no offline cache available.")
            }
        }

        val trips = BusParser.parseCsv(csvData)
        FetchResult(trips, isLive, lastUpdated)
    }

    fun getCachedSchedule(): FetchResult {
        val csvData = prefs.getString("cached_csv", "") ?: ""
        val lastUpdated = prefs.getLong("last_updated", 0L)
        val trips = if (csvData.isNotEmpty()) BusParser.parseCsv(csvData) else emptyList()
        return FetchResult(trips, false, lastUpdated)
    }
}
