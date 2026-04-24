package com.example.iitpbusschedule.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.iitpbusschedule.data.BusTrip
import java.util.Calendar
import java.util.TimeZone

object AlarmHelper {
    fun scheduleAlarm(context: Context, trip: BusTrip, leadTimeMinutes: Int = 10) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Please grant notification permission in settings to set reminders", Toast.LENGTH_LONG).show()
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        
        // Target time
        val targetMinutes = trip.sortKey - leadTimeMinutes
        
        if (targetMinutes <= currentMinutes) {
            Toast.makeText(context, "Departure is too soon to set a reminder!", Toast.LENGTH_SHORT).show()
            return
        }

        calendar.set(Calendar.HOUR_OF_DAY, targetMinutes / 60)
        calendar.set(Calendar.MINUTE, targetMinutes % 60)
        calendar.set(Calendar.SECOND, 0)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("bus_name", trip.busName)
            putExtra("departure_time", trip.departureTime)
            putExtra("from", trip.from)
            putExtra("to", trip.to)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            trip.id, // Unique ID for each trip
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        
        Toast.makeText(context, "Reminder set for ${leadTimeMinutes}m before departure", Toast.LENGTH_SHORT).show()
    }

    fun cancelAlarm(context: Context, trip: BusTrip) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            trip.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show()
    }
}
