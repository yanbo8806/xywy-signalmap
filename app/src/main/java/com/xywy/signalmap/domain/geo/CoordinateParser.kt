package com.xywy.signalmap.domain.geo

data class ParsedCoordinate(
    val longitude: Double,
    val latitude: Double,
)

object CoordinateParser {
    fun parse(input: String): ParsedCoordinate? {
        val normalized = input
            .trim()
            .replace('?', ',')
            .replace(Regex("\\s+"), " ")
            .replace(" ,", ",")
            .replace(", ", ",")

        val parts = if (normalized.contains(',')) {
            normalized.split(',').map { it.trim() }
        } else {
            normalized.split(' ').map { it.trim() }
        }.filter { it.isNotEmpty() }

        if (parts.size != 2) return null
        val lon = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        if (lon !in -180.0..180.0 || lat !in -90.0..90.0) return null
        return ParsedCoordinate(longitude = lon, latitude = lat)
    }
}
