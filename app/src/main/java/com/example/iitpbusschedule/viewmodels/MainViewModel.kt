package com.example.iitpbusschedule.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.repository.BusRepository
import com.example.iitpbusschedule.repository.SettingsManager
import com.example.iitpbusschedule.BusWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BusRepository,
    val settingsManager: SettingsManager
) : ViewModel() {


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
        // First load whatever we have in cache to show it instantly
        viewModelScope.launch {
            val cached = repository.getCachedSchedule()
            if (cached.trips.isNotEmpty()) {
                allTrips = cached.trips
                isLive = cached.isLive
                lastUpdated = cached.lastUpdated
                isLoading = false // Data is available, skip the main loading screen
            }
        }
        
        // Then start background refresh
        refreshSchedule()
    }

    fun refreshSchedule(isManual: Boolean = false) {
        viewModelScope.launch {
            // If we already have data (from cache), don't show the full-screen loader
            // Only show the refresh indicator if it's a manual refresh
            if (allTrips.isEmpty()) {
                isLoading = true
            } else if (isManual) {
                isRefreshing = true
            }

            try {
                val result = repository.getSchedule()
                allTrips = result.trips
                isLive = result.isLive
                lastUpdated = result.lastUpdated
                errorMessage = null
                // Notify widget that data has been refreshed
                BusWidgetProvider.triggerUpdate(context)
            } catch (e: Exception) {
                // If we already have cached data, don't show error message as a full screen error
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
        viewModelScope.launch {
            val cached = repository.getCachedSchedule()
            if (cached.trips.isNotEmpty()) {
                allTrips = cached.trips
                isLive = cached.isLive
                lastUpdated = cached.lastUpdated
                isLoading = false
                errorMessage = null
            }
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
