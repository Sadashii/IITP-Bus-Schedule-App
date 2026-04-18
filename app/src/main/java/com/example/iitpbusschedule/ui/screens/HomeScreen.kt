package com.example.iitpbusschedule.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.performHaptic
import com.example.iitpbusschedule.ui.components.*
import com.example.iitpbusschedule.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trips = viewModel.filteredTrips
    val listState = rememberLazyListState()

    val currentMinutes = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    val listItems = remember(trips) {
        val items = mutableListOf<Any>()
        var lastHour = -1
        trips.forEach { trip ->
            val hour = trip.sortKey / 60
            if (hour != lastHour) { lastHour = hour; items.add(hour) }
            items.add(trip)
        }
        items
    }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    // Unified Target Index Calculation
    val targetIndex by remember(listItems, currentMinutes, viewModel.settingsManager.scrollBufferMins.floatValue) {
        derivedStateOf {
            val buffer = viewModel.settingsManager.scrollBufferMins.floatValue.toInt()
            val targetTime = currentMinutes - buffer
            listItems.indexOfFirst { it is BusTrip && it.sortKey >= targetTime }
        }
    }

    // Auto-Scroll to Highlighted Trip (from Widget)
    LaunchedEffect(viewModel.highlightedTripId, listItems) {
        if (viewModel.highlightedTripId != null && listItems.isNotEmpty()) {
            val idx = listItems.indexOfFirst { it is BusTrip && "${it.departureTime}-${it.from}-${it.to}-${it.busName}" == viewModel.highlightedTripId }
            if (idx != -1) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    // Auto-Scroll on Launch
    LaunchedEffect(listItems, viewModel.settingsManager.enableAutoScroll.value) {
        if (viewModel.settingsManager.enableAutoScroll.value && listItems.isNotEmpty() && viewModel.highlightedTripId == null) {
            if (targetIndex != -1) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Weekday / Weekend Tabs
            Row(
                Modifier.fillMaxWidth().background(IITPBlue).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Weekdays", "Weekends").forEachIndexed { idx, label ->
                    val selected = viewModel.selectedTab == idx
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                            .background(if (selected) IITPGold else Color.White.copy(alpha = 0.15f))
                            .clickable { viewModel.selectedTab = idx; performHaptic(context) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, color = if (selected) Color.Black else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp
                        )
                    }
                }
            }

            // Stale data banner
            AnimatedVisibility(viewModel.isStale && !viewModel.isRefreshing) {
                Box(
                    Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "⚠️ Schedule may be outdated — pull down to refresh",
                        fontSize = 12.sp, color = Color(0xFF856404)
                    )
                }
            }


            when {
                viewModel.isLoading -> {
                    Column(Modifier.fillMaxSize()) {
                        repeat(10) { ShimmerTripItem() }
                    }
                }
                viewModel.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${viewModel.errorMessage}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = viewModel.isRefreshing,
                        onRefresh = { viewModel.refreshSchedule(); performHaptic(context) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 80.dp), // Extra padding for FAB
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (trips.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No trips match this filter", color = TextSecondary, fontSize = 15.sp)
                                    }
                                }
                            } else {
                                items(listItems.size) { index ->
                                    if (item is Int) {
                                        HourHeader(item, viewModel.settingsManager.use12Hour.value)
                                    } else if (item is BusTrip) {
                                        val tripId = "${item.departureTime}-${item.from}-${item.to}-${item.busName}"
                                        val isHighlighted = tripId == viewModel.highlightedTripId
                                        
                                        TripCard(
                                            trip = item,
                                            highlighted = isHighlighted,
                                            currentMinutes = currentMinutes,
                                            settings = viewModel.settingsManager
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // QUICK NAV FAB: Scroll to Current Time
        AnimatedVisibility(
            visible = !viewModel.isLoading && targetIndex != -1,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(targetIndex)
                        performHaptic(context)
                    }
                },
                containerColor = IITPBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = "Scroll to Now")
            }
        }
    }
}
