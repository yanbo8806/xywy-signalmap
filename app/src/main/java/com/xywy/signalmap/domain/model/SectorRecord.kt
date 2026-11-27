package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val longitude: Double,
    val latitude: Double,
)

@Serializable
data class SectorRecord(
    val id: String,
    val layerId: String,
    val radioTechnology: RadioTechnology,
    val siteId: String? = null,
    val siteName: String? = null,
    val cellId: String? = null,
    val cellName: String? = null,
    val pci: String? = null,
    val tacOrLac: String? = null,
    val band: String? = null,
    val bandwidth: String? = null,
    val vendor: String? = null,
    val operator: String? = null,
    val province: String? = null,
    val city: String? = null,
    val longitude: Double,
    val latitude: Double,
    val azimuth: Double? = null,
    val antennaHeight: String? = null,
    val channelNumber: String? = null,
    val polygonCoordinates: List<GeoPoint> = emptyList(),
    val rawFields: Map<String, String> = emptyMap(),
    val isPointOnly: Boolean = false,
) {
    val isLineOnly: Boolean
        get() = !isPointOnly &&
            polygonCoordinates.size >= 2 &&
            listOf(rawFields["类型"], rawFields["type"])
                .any { it.equals("线", ignoreCase = true) || it.equals("line", ignoreCase = true) }

    val searchText: String
        get() = listOf(
            siteName,
            siteId,
            cellName,
            cellId,
            pci,
            band,
            vendor,
            operator,
            province,
            city,
        )
            .filterNot { it.isNullOrBlank() }
            .joinToString(" ")
}
