package com.example.iitpbusschedule.data

data class BusTrip(
    val departureTime: String,
    val from: String,
    val to: String,
    val busName: String,
    val driverName: String,
    val contactNumber: String,
    val isWeekend: Boolean
) {
    val sortKey: Int get() {
        val cleanTime = departureTime.replace(Regex("[^0-9:]"), "")
        val p = cleanTime.split(":")
        return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 +
                (p.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}

data class FetchResult(
    val trips: List<BusTrip>,
    val isLive: Boolean,
    val lastUpdated: Long
)

data class AdminContact(val name: String, val role: String, val phone: String)
data class BusMetaData(var busName: String = "", var driverName: String = "", var contactNumber: String = "")
