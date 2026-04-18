package com.example.iitpbusschedule.repository

import com.example.iitpbusschedule.data.BusMetaData
import com.example.iitpbusschedule.data.BusTrip

object BusParser {
    fun parseCsv(csvData: String): List<BusTrip> {
        val resultList = mutableListOf<BusTrip>()
        if (csvData.isEmpty()) return resultList

        val lines = csvData.split("\n")
        val activeBuses = mutableMapOf<Int, BusMetaData>()
        var isWeekendSection = false

        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) continue

            val cols = (cleanLine + " ").split(",").map { it.replace("\"", "").trim() }
            val rawJoined = cols.joinToString("")

            if (rawJoined.contains("Weekends", ignoreCase = true)) {
                isWeekendSection = true
                activeBuses.clear()
                continue
            } else if (rawJoined.contains("Weekdays", ignoreCase = true)) {
                isWeekendSection = false
                activeBuses.clear()
                continue
            }

            val hasBusHeader = cols.any {
                val lower = it.lowercase()
                lower.startsWith("bus ") || lower.startsWith("institute bus")
            }

            if (hasBusHeader) {
                activeBuses.clear()
                for (i in cols.indices) {
                    val lower = cols[i].lowercase()
                    if (lower.startsWith("bus ") || lower.startsWith("institute bus")) {
                        activeBuses[i] = BusMetaData(busName = cols[i])
                    }
                }
                continue
            }

            if (cols.any { it.contains("Driver", ignoreCase = true) || it.contains("Conductor", ignoreCase = true) }) {
                for ((colIdx, meta) in activeBuses) {
                    val cell = cols.getOrNull(colIdx) ?: ""
                    if (cell.contains("Driver", ignoreCase = true) || cell.contains("Conductor", ignoreCase = true)) {
                        meta.driverName = cell.replace(Regex("(?i)(driver|conductor)\\s*[-:]?\\s*"), "").trim()
                    }
                }
                continue
            }

            if (cols.any { it.contains("Contact", ignoreCase = true) }) {
                for ((colIdx, meta) in activeBuses) {
                    val cell = cols.getOrNull(colIdx) ?: ""
                    if (cell.contains("Contact", ignoreCase = true)) {
                        meta.contactNumber = cell.replace(Regex("(?i)contact\\s*[-:]?\\s*"), "").trim()
                    }
                }
                continue
            }

            if (cols.any { it.contains("Departure Time", ignoreCase = true) }) continue

            if (activeBuses.isNotEmpty()) {
                for ((colIdx, meta) in activeBuses) {
                    val time = cols.getOrNull(colIdx) ?: ""
                    val from = cols.getOrNull(colIdx + 1) ?: ""
                    val to = cols.getOrNull(colIdx + 2) ?: ""

                    if (time.contains(":") && time.any { it.isDigit() }) {
                        resultList.add(
                            BusTrip(
                                departureTime = time,
                                from = from.ifEmpty { "N/A" },
                                to = to.ifEmpty { "N/A" },
                                busName = meta.busName,
                                driverName = meta.driverName.ifEmpty { "Unknown" },
                                contactNumber = meta.contactNumber.ifEmpty { "Unknown" },
                                isWeekend = isWeekendSection
                            )
                        )
                    }
                }
            }
        }
        return resultList
    }
}
