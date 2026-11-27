package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.GeoPoint

enum class MapMeasurementMode {
    NONE,
    DISTANCE,
    AREA,
}

data class MapMeasurementState(
    val mode: MapMeasurementMode = MapMeasurementMode.NONE,
    val points: List<GeoPoint> = emptyList(),
    val distanceMeters: Double? = null,
    val areaSquareMeters: Double? = null,
    val isFinished: Boolean = false,
) {
    val isActive: Boolean
        get() = mode != MapMeasurementMode.NONE

    val canFinish: Boolean
        get() = !isFinished && when (mode) {
            MapMeasurementMode.DISTANCE -> points.size >= 2
            MapMeasurementMode.AREA -> points.size >= 3
            MapMeasurementMode.NONE -> false
        }
}
