package com.example.iitpbusschedule.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    @Query("SELECT * FROM bus_trips")
    fun getAllTrips(): Flow<List<BusTrip>>

    @Query("SELECT * FROM bus_trips")
    suspend fun getAllTripsOneShot(): List<BusTrip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<BusTrip>)

    @Query("DELETE FROM bus_trips")
    suspend fun deleteAllTrips()

    @Query("SELECT * FROM bus_trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Int): BusTrip?
}
