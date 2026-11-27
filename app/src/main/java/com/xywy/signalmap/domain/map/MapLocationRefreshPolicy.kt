package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.LocationFix
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MapLocationRefreshPolicy {
    private const val REFRESH_INTERVAL_MILLIS = 5_000L
    private const val REFRESH_DISTANCE_METERS = 100.0
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun resolve(
        previous: LocationFix?,
        candidate: LocationFix?,
    ): LocationFix? {
        if (candidate == null) return null
        if (previous == null) return candidate

        val elapsed = candidate.timeMillis - previous.timeMillis
        if (elapsed < REFRESH_INTERVAL_MILLIS) return previous

        val movedMeters = haversineMeters(
            longitudeA = previous.longitude,
            latitudeA = previous.latitude,
            longitudeB = candidate.longitude,
            latitudeB = candidate.latitude,
        )
        return if (movedMeters > REFRESH_DISTANCE_METERS) candidate else previous
    }

    fun resolveDisplayLocation(
        previous: LocationFix?,
        candidate: LocationFix?,
    ): LocationFix? = candidate

    private fun haversineMeters(
        longitudeA: Double,
        latitudeA: Double,
        longitudeB: Double,
        latitudeB: Double,
    ): Double {
        val lat1 = Math.toRadians(latitudeA)
        val lat2 = Math.toRadians(latitudeB)
        val deltaLat = Math.toRadians(latitudeB - latitudeA)
        val deltaLon = Math.toRadians(longitudeB - longitudeA)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
