package com.example.iitpbusschedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.iitpbusschedule.repository.BusParser
import java.util.Calendar
import java.util.TimeZone

class BusWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "com.example.iitpbusschedule.ACTION_REFRESH"
        const val ACTION_AUTO_UPDATE = "com.example.iitpbusschedule.ACTION_AUTO_UPDATE"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, BusWidgetProvider::class.java).apply {
                action = ACTION_AUTO_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleMinuteAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.bus_list)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_AUTO_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, BusWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(cn)
                for (id in ids) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.bus_list)
                    updateAppWidget(context, appWidgetManager, id)
                }
                scheduleMinuteAlarm(context) // Re-schedule for the next minute
            }
        }
    }

    override fun onDisabled(context: Context) {
        cancelAlarm(context)
    }
}

internal fun scheduleMinuteAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BusWidgetProvider::class.java).apply {
        action = BusWidgetProvider.ACTION_AUTO_UPDATE
    }
    val pi = PendingIntent.getBroadcast(
        context, 9999, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    // Fire at the 0th second of the next minute precisely
    val cal = Calendar.getInstance().apply {
        add(Calendar.MINUTE, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, cal.timeInMillis, pi)
    } else {
        alarmManager.setExact(AlarmManager.RTC, cal.timeInMillis, pi)
    }
}

internal fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BusWidgetProvider::class.java).apply {
        action = BusWidgetProvider.ACTION_AUTO_UPDATE
    }
    val pi = PendingIntent.getBroadcast(
        context, 9999, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pi)
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_bus)

    // 1. Read Preferences
    val cachePrefs = context.getSharedPreferences("bus_cache", Context.MODE_PRIVATE)
    val settingsPrefs = context.getSharedPreferences("bus_prefs", Context.MODE_PRIVATE)
    
    val lastUpdated = cachePrefs.getLong("last_updated", 0L)
    val bgColorHex = settingsPrefs.getString("widget_bg_color", "#000000") ?: "#000000"
    val opacity = settingsPrefs.getFloat("widget_opacity", 0.6f)

    // 2. Set Consolidated Heading Text
    val timeAgo = if (lastUpdated == 0L) "Never" else {
        val diff = System.currentTimeMillis() - lastUpdated
        val mins = diff / (1000 * 60)
        if (mins < 1) "Just now" else "$mins mins ago"
    }
    views.setTextViewText(R.id.widget_title, "Bus Schedule • updated $timeAgo")

    // 3. Apply Background Styling
    try {
        val baseColor = android.graphics.Color.parseColor(bgColorHex)
        val alpha = (opacity * 255).toInt()
        val argbColor = (alpha shl 24) or (baseColor and 0x00FFFFFF)
        views.setInt(R.id.widget_root, "setBackgroundColor", argbColor)
    } catch (e: Exception) {}

    // 4. Set up Refresh Button
    val refreshIntent = Intent(context, BusWidgetProvider::class.java).apply {
        action = BusWidgetProvider.ACTION_REFRESH
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, appWidgetId, refreshIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_refresh, pendingIntent)

    // 5. Set up ListView
    val intent = Intent(context, BusWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.bus_list, intent)
    views.setEmptyView(R.id.bus_list, R.id.empty_view)

    // 6. Set up Click PendingIntent Template
    val clickIntent = Intent(context, MainActivity::class.java)
    val clickPI = PendingIntent.getActivity(
        context, 0, clickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setPendingIntentTemplate(R.id.bus_list, clickPI)

    // 7. Force refresh the list
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.bus_list)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun formatTo12HourWidget(time24: String): String {
    try {
        val parts = time24.replace(Regex("[^0-9:]"), "").split(":")
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
    } catch (e: Exception) { return time24 }
}