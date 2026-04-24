package com.example.iitpbusschedule.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BusTrip::class], version = 1, exportSchema = false)
abstract class BusDatabase : RoomDatabase() {
    abstract fun busDao(): BusDao
}
