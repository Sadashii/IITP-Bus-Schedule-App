package com.example.iitpbusschedule.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.repository.BusRepository
import com.example.iitpbusschedule.repository.SettingsManager
import com.example.iitpbusschedule.BusWidgetProvider
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BusRepository(application)
    val settingsManager = SettingsManager(application)

    // UI State
    var allTrips by mutableStateOf<List<BusTrip>>(emptyList())
    var isLive by mutableStateOf(false)
    var lastUpdated by mutableStateOf(0L)
    var isLoading by mutableStateOf(true)
    var isRefreshing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    var selectedTab by mutableIntStateOf(0)
    var currentScreen by mutableStateOf("HOME")
    var showInfo by mutableStateOf(false)

    // Filtering State
    var filterAll by mutableStateOf(true)
    var selectedBuses by mutableStateOf(setOf<String>())
    var selectedRoutes by mutableStateOf(setOf<String>())
    var showFilterModal by mutableStateOf(false)


    init {
        refreshSchedule()
    }

    fun refreshSchedule() {
        viewModelScope.launch {
            if (allTrips.isEmpty()) isLoading = true
            else isRefreshing = true

            try {
                val result = repository.getSchedule()
                allTrips = result.trips
                isLive = result.isLive
                lastUpdated = result.lastUpdated
                errorMessage = null
                // Notify widget that data has been refreshed
                BusWidgetProvider.triggerUpdate(getApplication())
            } catch (e: Exception) {
                if (allTrips.isEmpty()) {
                    errorMessage = e.message
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun skipLoading() {
        val cached = repository.getCachedSchedule()
        if (cached.trips.isNotEmpty()) {
            allTrips = cached.trips
            isLive = cached.isLive
            lastUpdated = cached.lastUpdated
            isLoading = false
            errorMessage = null
        }
    }

    fun toggleBusFilter(bus: String) {
        val newSet = selectedBuses.toMutableSet()
        if (newSet.contains(bus)) newSet.remove(bus) else newSet.add(bus)
        selectedBuses = newSet
        filterAll = selectedBuses.isEmpty() && selectedRoutes.isEmpty()
    }

    fun toggleRouteFilter(route: String) {
        val newSet = selectedRoutes.toMutableSet()
        if (newSet.contains(route)) newSet.remove(route) else newSet.add(route)
        selectedRoutes = newSet
        filterAll = selectedBuses.isEmpty() && selectedRoutes.isEmpty()
    }

    fun resetFilters() {
        filterAll = true
        selectedBuses = emptySet()
        selectedRoutes = emptySet()
    }

    private val currentTabTrips: List<BusTrip>
        get() = allTrips.filter { it.isWeekend == (selectedTab == 1) }

    val availableBuses: List<String>
        get() = currentTabTrips.map { it.busName }.distinct().sorted()

    val availableRoutes: List<String>
        get() {
            val routeCounts = currentTabTrips.groupingBy { "${it.from} → ${it.to}" }.eachCount()
            return routeCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
        }

    val filteredTrips: List<BusTrip>
        get() {
            val base = if (filterAll) allTrips
            else allTrips.filter { trip ->
                val route = "${trip.from} → ${trip.to}"
                (selectedBuses.isEmpty() || selectedBuses.contains(trip.busName)) &&
                (selectedRoutes.isEmpty() || selectedRoutes.contains(route))
            }
            
            return base.filter { it.isWeekend == (selectedTab == 1) }.sortedBy { it.sortKey }
        }


    val isStale: Boolean
        get() = lastUpdated > 0L && (System.currentTimeMillis() - lastUpdated) > 24 * 60 * 60 * 1000
}
