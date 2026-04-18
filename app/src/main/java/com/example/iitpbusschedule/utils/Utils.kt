package com.example.iitpbusschedule

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

fun performHaptic(context: Context) {
    try {
        val prefs = context.getSharedPreferences("bus_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enable_haptic", true)) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {}
}

fun getTimeAgo(timeMillis: Long): String {
    if (timeMillis == 0L) return "Never"
    val diff = System.currentTimeMillis() - timeMillis
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes mins ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}
