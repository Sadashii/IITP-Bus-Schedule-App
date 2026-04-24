package com.example.iitpbusschedule.di

import android.content.Context
import androidx.room.Room
import com.example.iitpbusschedule.data.BusDao
import com.example.iitpbusschedule.data.BusDatabase
import com.example.iitpbusschedule.repository.BusRepository
import com.example.iitpbusschedule.repository.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBusDatabase(@ApplicationContext context: Context): BusDatabase {
        return Room.databaseBuilder(
            context,
            BusDatabase::class.java,
            "bus_database"
        ).build()
    }

    @Provides
    fun provideBusDao(database: BusDatabase): BusDao {
        return database.busDao()
    }

    @Provides
    @Singleton
    fun provideBusRepository(
        @ApplicationContext context: Context,
        busDao: BusDao
    ): BusRepository {
        return BusRepository(context, busDao)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}
