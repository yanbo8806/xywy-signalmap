package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object EngineeringMatchDistance {
    const val MAX_METERS = 5_000.0

    fun hasValidLocation(location: LocationFix?): Boolean = location != null &&
        validCoordinates(location.longitude, location.latitude)

    fun meters(location: LocationFix?, sector: SectorRecord): Double {
        if (!hasValidLocation(location) || !validCoordinates(sector.longitude, sector.latitude)) {
            return Double.POSITIVE_INFINITY
        }
        val anchor = requireNotNull(location)
        val lat1 = Math.toRadians(anchor.latitude)
        val lat2 = Math.toRadians(sector.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(sector.longitude - anchor.longitude)
        val a = (sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)).coerceIn(0.0, 1.0)
        return 6_371_000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun isWithinRange(location: LocationFix?, sector: SectorRecord): Boolean =
        meters(location, sector) <= MAX_METERS

    private fun validCoordinates(longitude: Double, latitude: Double): Boolean =
        longitude in -180.0..180.0 && latitude in -90.0..90.0
}
