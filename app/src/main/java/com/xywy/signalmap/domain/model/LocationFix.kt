package com.xywy.signalmap.domain.model

data class LocationFix(
    val longitude: Double,
    val latitude: Double,
    val accuracyMeters: Float,
    val timeMillis: Long,
)
