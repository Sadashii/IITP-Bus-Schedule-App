package com.example.iitpbusschedule.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.example.iitpbusschedule.BusWidgetProvider

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bus_prefs", Context.MODE_PRIVATE)

    var use12Hour = mutableStateOf(prefs.getBoolean("use_12h", false))
    var swapSwipeActions = mutableStateOf(prefs.getBoolean("swap_swipe", false))
    var enableAutoScroll = mutableStateOf(prefs.getBoolean("enable_auto_scroll", true))
    var scrollBufferMins = mutableFloatStateOf(prefs.getInt("scroll_buffer", 10).toFloat())
    var showTints = mutableStateOf(prefs.getBoolean("show_tints", true))
    var darkMode = mutableStateOf(prefs.getString("dark_mode", "system") ?: "system")
    var enableHaptic = mutableStateOf(prefs.getBoolean("enable_haptic", true))

    // Widget Background Settings
    var widgetBgColor = mutableStateOf(prefs.getString("widget_bg_color", "#000000") ?: "#000000")
    var widgetOpacity = mutableFloatStateOf(prefs.getFloat("widget_opacity", 0.6f))
    
    val hasSeenOnboarding get() = prefs.getBoolean("has_seen_onboarding", false)
    fun markOnboardingSeen() { prefs.edit().putBoolean("has_seen_onboarding", true).apply() }

    private fun triggerWidgetUpdate() {
        BusWidgetProvider.triggerUpdate(context)
    }

    fun saveUse12Hour(value: Boolean) { use12Hour.value = value; prefs.edit().putBoolean("use_12h", value).apply(); triggerWidgetUpdate() }
    fun saveSwipeSwap(value: Boolean) { swapSwipeActions.value = value; prefs.edit().putBoolean("swap_swipe", value).apply() }
    fun saveEnableAutoScroll(value: Boolean) { enableAutoScroll.value = value; prefs.edit().putBoolean("enable_auto_scroll", value).apply() }
    fun saveScrollBuffer(value: Float) { scrollBufferMins.floatValue = value; prefs.edit().putInt("scroll_buffer", value.toInt()).apply(); triggerWidgetUpdate() }
    fun saveShowTints(value: Boolean) { showTints.value = value; prefs.edit().putBoolean("show_tints", value).apply(); triggerWidgetUpdate() }
    fun saveDarkMode(value: String) { darkMode.value = value; prefs.edit().putString("dark_mode", value).apply() }
    fun saveEnableHaptic(value: Boolean) { enableHaptic.value = value; prefs.edit().putBoolean("enable_haptic", value).apply() }
    
    fun saveWidgetBgColor(value: String) { widgetBgColor.value = value; prefs.edit().putString("widget_bg_color", value).apply(); triggerWidgetUpdate() }
    fun saveWidgetOpacity(value: Float) { widgetOpacity.value = value; prefs.edit().putFloat("widget_opacity", value).apply(); triggerWidgetUpdate() }
}
