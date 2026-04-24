package com.example.iitpbusschedule.repository

import android.content.Context
import com.example.iitpbusschedule.data.BusDao
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.data.FetchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

class BusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val busDao: BusDao
) {
    private val sheetUrl = "https://docs.google.com/spreadsheets/d/1p0WTx2O5rUEatdvpVtoQwnPEhv86_nZf5F-LMPwEe_s/export?format=csv&gid=0"
    private val prefs = context.getSharedPreferences("bus_cache", Context.MODE_PRIVATE)

    suspend fun getSchedule(): FetchResult = withContext(Dispatchers.IO) {
        var csvData = ""
        var isLive = false
        var lastUpdated = prefs.getLong("last_updated", 0L)

        try {
            val connection = URL(sheetUrl).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            csvData = connection.getInputStream().bufferedReader().use { it.readText() }
            
            if (csvData.isNotEmpty()) {
                val trips = BusParser.parseCsv(csvData)
                if (trips.isNotEmpty()) {
                    busDao.deleteAllTrips()
                    busDao.insertTrips(trips)
                    isLive = true
                    lastUpdated = System.currentTimeMillis()
                    prefs.edit().putLong("last_updated", lastUpdated).apply()
                    return@withContext FetchResult(trips, isLive, lastUpdated)
                }
            }
        } catch (e: Exception) {
            // Fallback to cache handled below
        }

        return@withContext getCachedSchedule()
    }

    suspend fun getCachedSchedule(): FetchResult = withContext(Dispatchers.IO) {
        val lastUpdated = prefs.getLong("last_updated", 0L)
        // We can't use Flow here directly if we want a one-shot result for this method
        // But Dao has Flow, we can use a separate suspend method if needed or just query once.
        // I'll add a one-shot query to Dao.
        val trips = busDao.getAllTripsOneShot() 
        FetchResult(trips, false, lastUpdated)
    }
}

