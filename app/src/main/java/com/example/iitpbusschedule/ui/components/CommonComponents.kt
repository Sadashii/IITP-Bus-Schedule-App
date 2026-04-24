package com.example.iitpbusschedule.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.iitpbusschedule.data.AdminContact
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.repository.SettingsManager
import com.example.iitpbusschedule.performHaptic
import com.example.iitpbusschedule.utils.AlarmHelper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.NotificationsActive

// Constants from MainActivity
val IITPBlue   = Color(0xFF1A3A6B)
val IITPGold   = Color(0xFFF5A623)
val DividerClr = Color(0xFFE0E6EF)
val TextPrimary   = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF6B7280)

val ChipTextClr  = Color.White

fun getPlaceColor(place: String): Color {
    val p = place.lowercase()
    return when {
        p.contains("aryabhatta") -> Color(0xFF1A3A6B) // IITP Blue
        p.contains("tutorial")   -> Color(0xFF2E7D32) // Green
        p.contains("quarter")    -> Color(0xFFBF360C) // Deep Orange
        p.contains("patna")      -> Color(0xFF4527A0) // Deep Purple
        p.contains("bihta")      -> Color(0xFF004D40) // Teal
        p.contains("gate")       -> Color(0xFF37474F) // Blue Grey
        p.contains("admin")      -> Color(0xFFB71C1C) // Red
        p.contains("hostel")     -> Color(0xFF1565C0) // Blue
        else                     -> Color(0xFF546E7A) // Default Blue Grey
    }
}

val adminContacts = listOf(
    AdminContact("Mantu Ji", "Admin Staff", "+91 8986162721"),
    AdminContact("Rajeev Ji", "Bus Manager (BSRTC)", "+91 6201957967")
)

@Composable
fun HourHeader(hour: Int, use12h: Boolean) {
    val label = if (use12h) {
        when {
            hour == 0 -> "12:00 AM"
            hour < 12 -> "${hour}:00 AM"
            hour == 12 -> "12:00 PM"
            else -> "${hour - 12}:00 PM"
        }
    } else {
        "${hour.toString().padStart(2, '0')}:00"
    }

    val divColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = divColor)
        Text(" $label ", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f), color = divColor)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TripCard(trip: BusTrip, currentMinutes: Int, settings: SettingsManager) {
    val context = LocalContext.current
    val isPast = trip.sortKey < currentMinutes
    val routeKey = "${trip.from} → ${trip.to}"

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = when {
        !settings.showTints.value -> MaterialTheme.colorScheme.surface
        isPast -> if (isDark) Color(0xFF3B1A1A) else Color(0xFFFFEBEE)
        else -> if (isDark) Color(0xFF1A3B1A) else Color(0xFFE8F5E9)
    }

    val displayTime = if (settings.use12Hour.value) formatTo12Hour(trip.departureTime) else trip.departureTime

    SwipeableActionCard(
        trip = trip,
        displayTime = displayTime,
        routeKey = routeKey,
        settings = settings
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* Could show details or nothing */ },
                    onLongClick = {
                        AlarmHelper.scheduleAlarm(context, trip)
                        performHaptic(context)
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    val timeColor = if (isDark) Color(0xFF6FA8FF) else IITPBlue
                    Text(displayTime, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = timeColor)
                    val diff = trip.sortKey - currentMinutes
                    val relativeTime = when {
                        diff > 0 -> "in ${diff}m"
                        diff == 0 -> "now"
                        else -> "${-diff}m ago"
                    }
                    val relColor = if (isPast) {
                        if (isDark) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                    } else {
                        if (isDark) Color(0xFF69F0AE) else Color(0xFF2E7D32)
                    }
                    Text(relativeTime, fontSize = 10.sp, color = relColor, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RouteChip(trip.from, getPlaceColor(trip.from), ChipTextClr)
                        Text("→", fontSize = 12.sp, color = if (isDark) Color(0xFFAAAAAA) else TextSecondary)
                        RouteChip(trip.to, getPlaceColor(trip.to), ChipTextClr)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${trip.busName}  •  ${trip.driverName}",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableActionCard(
    trip: BusTrip,
    displayTime: String,
    routeKey: String,
    settings: SettingsManager,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val isSwapped = settings.swapSwipeActions.value
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isSwapped) shareTrip(context, trip, displayTime, routeKey) 
                    else callDriver(context, trip.contactNumber)
                    performHaptic(context)
                    false // Spring back
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isSwapped) callDriver(context, trip.contactNumber)
                    else shareTrip(context, trip, displayTime, routeKey)
                    performHaptic(context)
                    false // Spring back
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isSwapped = settings.swapSwipeActions.value
            
            val (color, icon, label) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isSwapped) Triple(IITPBlue, Icons.Default.Share, "Share")
                    else Triple(Color(0xFF2E7D32), Icons.Default.Phone, "Call")
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isSwapped) Triple(Color(0xFF2E7D32), Icons.Default.Phone, "Call")
                    else Triple(IITPBlue, Icons.Default.Share, "Share")
                }
                else -> Triple(Color.Transparent, Icons.Default.Clear, "")
            }

            Box(
                Modifier.fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(icon, contentDescription = label, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(icon, contentDescription = label, tint = Color.White)
                    }
                }
            }
        },
        content = { content() }
    )
}

fun callDriver(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
    }
}

fun shareTrip(context: Context, trip: BusTrip, displayTime: String, routeKey: String) {
    val shareText = "IITP Bus Trip: $displayTime\n$routeKey\n${trip.busName} (${trip.driverName})\nContact: ${trip.contactNumber}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Trip Details"))
}

fun openSheet(context: Context) {
    try {
        val url = "https://docs.google.com/spreadsheets/d/1p0WTx2O5rUEatdvpVtoQwnPEhv86_nZf5F-LMPwEe_s/edit#gid=0"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open sheet URL", Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun RouteChip(label: String, bg: Color, textColor: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        control()
    }
}

@Composable
fun AdminInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = IITPBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Support Contacts", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = IITPBlue)
                }
                Spacer(Modifier.height(8.dp))
                Text("For bus-related queries ONLY. Please do NOT call for schedule info — use the app or check the source sheet.", fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                Spacer(Modifier.height(20.dp))
                adminContacts.forEach { admin ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            copyToClipboard(context, admin.phone)
                            performHaptic(context)
                            Toast.makeText(context, "Copied: ${admin.phone}", Toast.LENGTH_SHORT).show()
                        }.background(IITPBlue.copy(alpha = 0.05f)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(admin.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(admin.role, fontSize = 12.sp, color = TextSecondary)
                            Text(admin.phone, fontSize = 14.sp, color = IITPBlue, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = IITPBlue.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = IITPBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterModal(
    viewModel: com.example.iitpbusschedule.viewmodels.MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Filters", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IITPBlue)
                TextButton(onClick = { viewModel.resetFilters(); performHaptic(context) }) {
                    Text("Reset", color = TextSecondary)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // CATEGORY: ALL
            Text("General", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = viewModel.filterAll,
                onClick = { viewModel.resetFilters(); performHaptic(context) },
                label = { Text("All Buses & Routes") },
                leadingIcon = { if (viewModel.filterAll) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IITPBlue, selectedLabelColor = Color.White)
            )

            Spacer(Modifier.height(24.dp))

            // CATEGORY: BUS
            Text("By Bus", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            CustomWrapRow(horizontalGap = 8.dp, verticalGap = 4.dp, modifier = Modifier.fillMaxWidth()) {
                viewModel.availableBuses.forEach { bus ->
                    val selected = viewModel.selectedBuses.contains(bus)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleBusFilter(bus); performHaptic(context) },
                        label = { Text(bus, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IITPBlue, selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // CATEGORY: ROUTE
            Text("By Route", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            CustomWrapRow(horizontalGap = 8.dp, verticalGap = 4.dp, modifier = Modifier.fillMaxWidth()) {
                viewModel.availableRoutes.forEach { route ->
                    val selected = viewModel.selectedRoutes.contains(route)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleRouteFilter(route); performHaptic(context) },
                        label = { Text(route, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IITPBlue, selectedLabelColor = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(Modifier.padding(vertical = 10.dp)) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(IITPBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IITPBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}

@Composable
fun WelcomeModal(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Welcome to IITP Bus! \uD83D\uDE8C",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = IITPBlue
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Get started with these premium features:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                FeatureItem(icon = Icons.Default.Dashboard, title = "Home Widgets", desc = "Check the schedule right from your home screen.")
                FeatureItem(icon = Icons.Default.NotificationsActive, title = "Bus Reminders", desc = "Set alarms for your bus and never miss a trip again!")
                FeatureItem(icon = Icons.AutoMirrored.Filled.CompareArrows, title = "Swipe Actions", desc = "Swipe on trips to call the driver or share information instantly.")
                FeatureItem(icon = Icons.Default.FilterList, title = "Smart Filters", desc = "Drill down to the exact bus route you need.")
                Spacer(Modifier.height(4.dp))
                Text("Find more customization in the Settings menu!", fontSize = 12.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = { performHaptic(context); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = IITPBlue)
            ) {
                Text("Let's Go", color = Color.White)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}


// Helpers
fun formatTo12Hour(time24: String): String {
    try {
        val cleanTime = time24.replace(Regex("[^0-9:]"), "")
        val parts = cleanTime.split(":")
        if (parts.size != 2) return time24

        val hour = parts[0].toInt()
        val minute = parts[1]

        val suffix = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$hour12:$minute $suffix"
    } catch (e: Exception) {
        return time24
    }
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Driver Contact", text))
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = ""
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}



@Composable
fun ShimmerTripItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
    }
}

@Composable
fun CustomWrapRow(
    modifier: Modifier = Modifier,
    horizontalGap: androidx.compose.ui.unit.Dp = 8.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val itemConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }
        
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentX = 0
        var currentY = 0
        var currentRowHeight = 0
        var maxWidth = 0
        
        val hGap = horizontalGap.roundToPx()
        val vGap = verticalGap.roundToPx()

        for (placeable in placeables) {
            if (currentRow.isNotEmpty() && currentX + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                maxWidth = maxOf(maxWidth, currentX - hGap)
                currentX = 0
                currentY += currentRowHeight + vGap
                currentRowHeight = 0
                currentRow = mutableListOf()
            }
            currentRow.add(placeable)
            currentX += placeable.width + hGap
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
            maxWidth = maxOf(maxWidth, currentX - hGap)
            currentY += currentRowHeight
        }
        
        layout(width = maxOf(maxWidth, constraints.minWidth), height = maxOf(currentY, constraints.minHeight)) {
            var y = 0
            rows.forEachIndexed { i, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGap
                }
                y += rowHeights[i] + vGap
            }
        }
    }
}
