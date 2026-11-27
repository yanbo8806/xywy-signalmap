package com.xywy.signalmap.domain.model

data class DeviceSensorSnapshot(
    val verticalAngleDegrees: Float? = null,
    val pressureHPa: Float? = null,
    val altitudeMeters: Float? = null,
    val azimuthDegrees: Float? = null,
    val observedAtMillis: Long = 0L,
) {
    val hasAnySensorValue: Boolean
        get() = verticalAngleDegrees != null ||
            pressureHPa != null ||
            altitudeMeters != null ||
            azimuthDegrees != null

    companion object {
        val Empty = DeviceSensorSnapshot()
    }
}
