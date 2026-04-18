package com.example.iitpbusschedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iitpbusschedule.performHaptic
import com.example.iitpbusschedule.repository.SettingsManager
import com.example.iitpbusschedule.ui.components.*

@Composable
fun SettingsScreen(settings: SettingsManager, onShowSupport: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Appearance Section
        SectionHeader("Appearance")
        
        SettingRow("Dark Mode", "Choose app appearance") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "on" to "Dark", "off" to "Light").forEach { (value, label) ->
                    val isSelected = settings.darkMode.value == value
                    FilterChip(
                        selected = isSelected,
                        onClick = { settings.saveDarkMode(value) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IITPBlue,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
        
        SettingRow("Colour Tinting", "Highlight past/future trips") {
            Switch(checked = settings.showTints.value, onCheckedChange = { settings.saveShowTints(it) })
        }

        HorizontalDivider(color = DividerClr)

        // 2. Schedule Options Section
        SectionHeader("Schedule Options")
        
        SettingRow("Use 12-Hour Format", "Show AM/PM instead of 24h") {
            Switch(checked = settings.use12Hour.value, onCheckedChange = { settings.saveUse12Hour(it) })
        }
        
        SettingRow("Auto-Scroll to Present", "Jump to current time on open") {
            Switch(checked = settings.enableAutoScroll.value, onCheckedChange = { settings.saveEnableAutoScroll(it) })
        }

        if (settings.enableAutoScroll.value) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    "Scroll buffer: ${settings.scrollBufferMins.floatValue.toInt()} mins past",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.scrollBufferMins.floatValue,
                    onValueChange = { settings.saveScrollBuffer(it) },
                    valueRange = 0f..45f,
                    colors = SliderDefaults.colors(thumbColor = IITPGold, activeTrackColor = IITPBlue)
                )
            }
        }

        HorizontalDivider(color = DividerClr)

        // 3. Interactions Section
        SectionHeader("Interactions")
        
        SettingRow("Swap Swipe Actions", if (settings.swapSwipeActions.value) "Swipe Right: Share, Swipe Left: Call" else "Swipe Right: Call, Swipe Left: Share") {
            Switch(checked = settings.swapSwipeActions.value, onCheckedChange = { settings.saveSwipeSwap(it) })
        }
        
        SettingRow("Haptic Feedback", "Vibrate on interactions") {
            Switch(checked = settings.enableHaptic.value, onCheckedChange = { settings.saveEnableHaptic(it) })
        }

        HorizontalDivider(color = DividerClr)

        // 4. Widget Customization Section
        SectionHeader("Widget Styling")
        
        Column {
            Text("Background Colour", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("#000000", "#1A3A6B", "#1C1C1E", "#06402B").forEach { colorHex ->
                    val color = Color(android.graphics.Color.parseColor(colorHex))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .clickable { settings.saveWidgetBgColor(colorHex) }
                            .padding(2.dp)
                    ) {
                        if (settings.widgetBgColor.value == colorHex) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.3f)))
                        }
                    }
                }
            }
        }

        Column {
            Text(
                "Background Opacity: ${(settings.widgetOpacity.floatValue * 100).toInt()}%",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium
            )
            Slider(
                value = settings.widgetOpacity.floatValue,
                onValueChange = { settings.saveWidgetOpacity(it) },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = IITPGold, activeTrackColor = IITPBlue)
            )
        }

        HorizontalDivider(color = DividerClr)
 
        Text(
            "With love, for M, from T.",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center
        )

    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = IITPBlue,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
