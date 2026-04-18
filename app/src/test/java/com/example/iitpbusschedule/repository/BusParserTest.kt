package com.example.iitpbusschedule.repository

import com.example.iitpbusschedule.data.BusTrip
import org.junit.Assert.assertEquals
import org.junit.Test

class BusParserTest {
    @Test
    fun testParseCsv() {
        val csvData = "Weekdays\nBus 1\nDeparture Time,From,To\n7:00,Campus,Station\n\nWeekends\nBus 2\nDeparture Time,From,To\n9:00,Campus,Station"

        val trips: List<BusTrip> = BusParser.parseCsv(csvData)
        
        assertEquals(2, trips.size)
        
        val trip1 = trips[0]
        assertEquals("7:00", trip1.departureTime)
        assertEquals(false, trip1.isWeekend)
        
        val trip2 = trips[1]
        assertEquals("9:00", trip2.departureTime)
        assertEquals(true, trip2.isWeekend)
    }
}
