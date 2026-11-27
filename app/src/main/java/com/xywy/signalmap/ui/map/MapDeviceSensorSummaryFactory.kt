package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import kotlin.math.roundToInt

data class MapDeviceSensorSummaryState(
    val verticalAngleText: String,
    val altitudeText: String,
    val pressureText: String,
    val azimuthText: String,
    val available: Boolean,
)

object MapDeviceSensorSummaryFactory {
    fun create(snapshot: DeviceSensorSnapshot): MapDeviceSensorSummaryState =
        MapDeviceSensorSummaryState(
            verticalAngleText = "垂直 ${formatDegrees(snapshot.verticalAngleDegrees)}",
            altitudeText = "海拔 ${snapshot.altitudeMeters?.roundToInt()?.let { "$it m" } ?: "--"}",
            pressureText = "气压 ${snapshot.pressureHPa?.roundToInt()?.let { "$it hPa" } ?: "--"}",
            azimuthText = "方位 ${formatDegrees(snapshot.azimuthDegrees)}",
            available = snapshot.hasAnySensorValue,
        )

    private fun formatDegrees(value: Float?): String =
        value?.roundToInt()?.let { "$it°" } ?: "--"
}
