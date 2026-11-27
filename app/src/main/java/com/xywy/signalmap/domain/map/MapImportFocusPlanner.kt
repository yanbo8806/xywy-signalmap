package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.math.cos

data class MapImportFocus(val longitude: Double, val latitude: Double, val zoom: Double)

object MapImportFocusPlanner {
    fun resolve(records: List<SectorRecord>): MapImportFocus? {
        val validRecords = records.filter {
            it.longitude in -180.0..180.0 && it.latitude in -90.0..90.0
        }
        if (validRecords.isEmpty()) return null
        val longitude = validRecords.map { it.longitude }.average()
        val latitude = validRecords.map { it.latitude }.average()
        val longitudeScale = cos(Math.toRadians(latitude))
        // A fitted overview may hide every overlay; an average can fall between distant clusters.
        val anchor = validRecords.minBy {
            val dx = (it.longitude - longitude) * longitudeScale
            val dy = it.latitude - latitude
            dx * dx + dy * dy
        }
        return MapImportFocus(anchor.longitude, anchor.latitude, zoom = 15.0)
    }
}
