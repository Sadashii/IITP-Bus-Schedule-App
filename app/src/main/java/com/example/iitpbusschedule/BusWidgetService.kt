package com.example.iitpbusschedule

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.iitpbusschedule.data.BusTrip
import com.example.iitpbusschedule.repository.BusParser
import java.util.Calendar
import java.util.TimeZone

class BusWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return BusRemoteViewsFactory(this.applicationContext)
    }
}

class BusRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var trips: List<BusTrip> = emptyList()
    private var use12h: Boolean = false
    private var showTints: Boolean = false

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val cachePrefs = context.getSharedPreferences("bus_cache", Context.MODE_PRIVATE)
        val settingsPrefs = context.getSharedPreferences("bus_prefs", Context.MODE_PRIVATE)

        val csvData = cachePrefs.getString("cached_csv", "") ?: ""
        val scrollBuffer = settingsPrefs.getInt("scroll_buffer", 10)
        use12h = settingsPrefs.getBoolean("use_12h", false)
        showTints = settingsPrefs.getBoolean("show_tints", true)

        val allTrips = BusParser.parseCsv(csvData)

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val targetTime = currentMinutes - scrollBuffer
        val isTodayWeekend = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        trips = allTrips.filter { it.isWeekend == isTodayWeekend && it.sortKey >= targetTime }
            .sortedBy { it.sortKey }
    }

    override fun onDestroy() {
        trips = emptyList()
    }

    override fun getCount(): Int = trips.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= trips.size) return RemoteViews(context.packageName, R.layout.widget_item)

        val trip = trips[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        val displayTime = if (use12h) formatTo12HourWidget(trip.departureTime) else trip.departureTime

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val diff = trip.sortKey - currentMinutes
        val relativeTime = when {
            diff > 0 -> "in ${diff}m"
            diff == 0 -> "now"
            else -> "${-diff}m ago"
        }

        val text = "$displayTime ($relativeTime)  —  ${trip.from} → ${trip.to}"
        views.setTextViewText(R.id.item_text, text)

        if (showTints) {
            val isPast = trip.sortKey < currentMinutes
            val color = if (isPast) 0xFFFFD1D1.toInt() else 0xFFD1FFD1.toInt()
            views.setTextColor(R.id.item_text, color)
        } else {
            views.setTextColor(R.id.item_text, 0xFFFFFFFF.toInt())
        }

        // Deep link fill-in intent
        val tripId = "${trip.departureTime}-${trip.from}-${trip.to}-${trip.busName}"
        val fillInIntent = Intent().apply {
            putExtra("EXTRA_HIGHLIGHT_BUS_ID", tripId)
        }
        views.setOnClickFillInIntent(R.id.item_text, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
