package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.GeoPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MapMeasurementController {
    fun startDistance(): MapMeasurementState = MapMeasurementState(
        mode = MapMeasurementMode.DISTANCE,
    )

    fun startArea(): MapMeasurementState = MapMeasurementState(
        mode = MapMeasurementMode.AREA,
    )

    fun addPoint(
        state: MapMeasurementState,
        longitude: Double,
        latitude: Double,
    ): MapMeasurementState {
        if (!state.isActive || state.isFinished) return state
        return recalculate(
            state.copy(
                points = state.points + GeoPoint(longitude = longitude, latitude = latitude),
            ),
        )
    }

    fun finish(state: MapMeasurementState): MapMeasurementState {
        if (!state.canFinish) return state
        return state.copy(isFinished = true)
    }

    fun clear(): MapMeasurementState = MapMeasurementState()

    private fun recalculate(state: MapMeasurementState): MapMeasurementState = state.copy(
        distanceMeters = when (state.mode) {
            MapMeasurementMode.DISTANCE,
            MapMeasurementMode.AREA,
            -> cumulativeDistanceMeters(state.points)

            MapMeasurementMode.NONE -> null
        },
        areaSquareMeters = when (state.mode) {
            MapMeasurementMode.AREA -> polygonAreaSquareMeters(state.points)
            else -> null
        },
    )

    private fun cumulativeDistanceMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        return points.zip(points.drop(1)).sumOf { (start, end) ->
            haversineMeters(
                longitudeA = start.longitude,
                latitudeA = start.latitude,
                longitudeB = end.longitude,
                latitudeB = end.latitude,
            )
        }
    }

    private fun polygonAreaSquareMeters(points: List<GeoPoint>): Double? {
        if (points.size < 3) return null
        val averageLatitude = points.map { it.latitude }.average()
        val projected = points.map { point ->
            val x = Math.toRadians(point.longitude) * EARTH_RADIUS_METERS * cos(Math.toRadians(averageLatitude))
            val y = Math.toRadians(point.latitude) * EARTH_RADIUS_METERS
            x to y
        }
        val area = projected.indices.sumOf { index ->
            val current = projected[index]
            val next = projected[(index + 1) % projected.size]
            current.first * next.second - next.first * current.second
        }
        return abs(area) / 2.0
    }

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

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
