package com.xywy.signalmap.domain.geo

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object SectorGeometry {
    private const val EARTH_RADIUS_METERS = 6_378_137.0

    fun buildSectorRing(
        longitude: Double,
        latitude: Double,
        azimuth: Double,
        radiusMeters: Double,
        beamWidthDegrees: Double,
        segments: Int = 24,
    ): List<Pair<Double, Double>> {
        val safeSegments = maxOf(6, segments)
        val half = beamWidthDegrees / 2.0
        val start = azimuth - half
        val step = beamWidthDegrees / safeSegments
        val coordinates = mutableListOf<Pair<Double, Double>>()
        coordinates += longitude to latitude
        for (index in 0..safeSegments) {
            coordinates += destinationPoint(longitude, latitude, start + (step * index), radiusMeters)
        }
        coordinates += longitude to latitude
        return coordinates
    }

    fun buildTriangleRing(
        longitude: Double,
        latitude: Double,
        azimuth: Double,
        radiusMeters: Double,
        beamWidthDegrees: Double,
    ): List<Pair<Double, Double>> {
        val half = beamWidthDegrees / 2.0
        val left = destinationPoint(longitude, latitude, azimuth - half, radiusMeters)
        val right = destinationPoint(longitude, latitude, azimuth + half, radiusMeters)
        return listOf(
            longitude to latitude,
            left,
            right,
            longitude to latitude,
        )
    }

    fun triangleBaseMidpoint(
        longitude: Double,
        latitude: Double,
        azimuth: Double,
        radiusMeters: Double,
        beamWidthDegrees: Double,
    ): Pair<Double, Double> {
        val half = beamWidthDegrees / 2.0
        val left = destinationPoint(longitude, latitude, azimuth - half, radiusMeters)
        val right = destinationPoint(longitude, latitude, azimuth + half, radiusMeters)
        return ((left.first + right.first) / 2.0) to ((left.second + right.second) / 2.0)
    }

    fun destinationPoint(
        longitude: Double,
        latitude: Double,
        bearingDegrees: Double,
        distanceMeters: Double,
    ): Pair<Double, Double> {
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val bearing = Math.toRadians(bearingDegrees)
        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinAngular = sin(angularDistance)
        val cosAngular = cos(angularDistance)

        val resultLat = asin((sinLat * cosAngular) + (cosLat * sinAngular * cos(bearing)))
        val resultLon = lonRad + atan2(
            sin(bearing) * sinAngular * cosLat,
            cosAngular - (sinLat * sin(resultLat)),
        )
        return Math.toDegrees(resultLon) to Math.toDegrees(resultLat)
    }
}

object SectorStyleCatalog {
    fun useTriangleShape(record: SectorRecord): Boolean =
        record.radioTechnology == RadioTechnology.LTE || record.radioTechnology == RadioTechnology.NR

    fun beamWidthDegrees(record: SectorRecord): Double = when (record.radioTechnology) {
        RadioTechnology.NR -> when (record.band.orEmpty().uppercase()) {
            "N28", "N8", "N5" -> 26.0
            "N41" -> 20.0
            "N78", "N79" -> 18.0
            else -> 22.0
        }
        RadioTechnology.LTE -> when (record.band.orEmpty().uppercase()) {
            "B8", "B5", "B20" -> 40.0
            "B1", "B3" -> 34.0
            "B34", "B38", "B39", "B40", "B41" -> 28.0
            else -> 32.0
        }
        RadioTechnology.WCDMA, RadioTechnology.GSM -> 90.0
        RadioTechnology.CDMA -> 120.0
        RadioTechnology.UNKNOWN -> 75.0
    }

    fun radiusMeters(record: SectorRecord): Double {
        val band = record.band.orEmpty().uppercase()
        return when (record.radioTechnology) {
            RadioTechnology.NR -> when {
                band.contains("N28") || band.contains("N8") || band.contains("N5") -> 100.0
                band.contains("N41") -> 76.0
                band.contains("N78") || band.contains("N79") -> 58.0
                else -> 72.0
            }
            RadioTechnology.LTE -> when {
                band.contains("B8") || band.contains("B20") -> 92.0
                band.contains("B1") || band.contains("B3") -> 74.0
                band.contains("B34") || band.contains("B38") || band.contains("B39") || band.contains("B40") || band.contains("B41") -> 68.0
                else -> 72.0
            }
            RadioTechnology.WCDMA -> 70.0
            RadioTechnology.GSM -> 82.0
            RadioTechnology.CDMA -> 88.0
            RadioTechnology.UNKNOWN -> 60.0
        }
    }

    fun color(record: SectorRecord): String = when (record.radioTechnology) {
        RadioTechnology.NR -> when (record.band.orEmpty().uppercase()) {
            "N28" -> "#00A870"
            "N41" -> "#7B61FF"
            "N78", "N79" -> "#A855F7"
            else -> "#6D5EF8"
        }
        RadioTechnology.LTE -> when (record.band.orEmpty().uppercase()) {
            "B8" -> "#1677FF"
            "B3" -> "#FF7A45"
            "B41", "B40" -> "#FA8C16"
            else -> "#2F54EB"
        }
        RadioTechnology.WCDMA -> "#13C2C2"
        RadioTechnology.GSM -> "#FAAD14"
        RadioTechnology.CDMA -> "#FF7A45"
        RadioTechnology.UNKNOWN -> "#8C8C8C"
    }
}
