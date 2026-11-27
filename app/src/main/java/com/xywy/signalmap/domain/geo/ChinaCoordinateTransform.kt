package com.xywy.signalmap.domain.geo

import com.xywy.signalmap.domain.model.GeoPoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ChinaCoordinateTransform {
    fun wgs84ToGcj02(
        longitude: Double,
        latitude: Double,
    ): GeoPoint {
        if (isOutOfChina(longitude, latitude)) {
            return GeoPoint(longitude = longitude, latitude = latitude)
        }

        val delta = delta(longitude, latitude)
        return GeoPoint(
            longitude = longitude + delta.longitude,
            latitude = latitude + delta.latitude,
        )
    }

    fun gcj02ToWgs84(
        longitude: Double,
        latitude: Double,
    ): GeoPoint {
        if (isOutOfChina(longitude, latitude)) {
            return GeoPoint(longitude = longitude, latitude = latitude)
        }

        val delta = delta(longitude, latitude)
        val mgLongitude = longitude + delta.longitude
        val mgLatitude = latitude + delta.latitude
        return GeoPoint(
            longitude = longitude * 2 - mgLongitude,
            latitude = latitude * 2 - mgLatitude,
        )
    }

    private fun delta(
        longitude: Double,
        latitude: Double,
    ): GeoPoint {
        var dLatitude = transformLatitude(longitude - 105.0, latitude - 35.0)
        var dLongitude = transformLongitude(longitude - 105.0, latitude - 35.0)
        val radLatitude = latitude / 180.0 * PI
        var magic = sin(radLatitude)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLatitude = (dLatitude * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLongitude = (dLongitude * 180.0) / (A / sqrtMagic * cos(radLatitude) * PI)
        return GeoPoint(longitude = dLongitude, latitude = dLatitude)
    }

    private fun isOutOfChina(
        longitude: Double,
        latitude: Double,
    ): Boolean = longitude !in 72.004..137.8347 || latitude !in 0.8293..55.8271

    private fun transformLatitude(
        longitude: Double,
        latitude: Double,
    ): Double {
        var result = -100.0 + 2.0 * longitude + 3.0 * latitude + 0.2 * latitude * latitude +
            0.1 * longitude * latitude + 0.2 * sqrt(kotlin.math.abs(longitude))
        result += (20.0 * sin(6.0 * longitude * PI) + 20.0 * sin(2.0 * longitude * PI)) * 2.0 / 3.0
        result += (20.0 * sin(latitude * PI) + 40.0 * sin(latitude / 3.0 * PI)) * 2.0 / 3.0
        result += (160.0 * sin(latitude / 12.0 * PI) + 320 * sin(latitude * PI / 30.0)) * 2.0 / 3.0
        return result
    }

    private fun transformLongitude(
        longitude: Double,
        latitude: Double,
    ): Double {
        var result = 300.0 + longitude + 2.0 * latitude + 0.1 * longitude * longitude +
            0.1 * longitude * latitude + 0.1 * sqrt(kotlin.math.abs(longitude))
        result += (20.0 * sin(6.0 * longitude * PI) + 20.0 * sin(2.0 * longitude * PI)) * 2.0 / 3.0
        result += (20.0 * sin(longitude * PI) + 40.0 * sin(longitude / 3.0 * PI)) * 2.0 / 3.0
        result += (150.0 * sin(longitude / 12.0 * PI) + 300.0 * sin(longitude / 30.0 * PI)) * 2.0 / 3.0
        return result
    }

    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323
}
