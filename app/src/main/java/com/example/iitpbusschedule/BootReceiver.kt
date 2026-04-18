package com.example.iitpbusschedule

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule the widget minute alarm after reboot
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, BusWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(cn)
            if (ids.isNotEmpty()) {
                scheduleMinuteAlarm(context)
            }
        }
    }
}
