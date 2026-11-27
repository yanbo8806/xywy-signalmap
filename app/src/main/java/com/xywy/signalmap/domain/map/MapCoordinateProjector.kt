package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.geo.ChinaCoordinateTransform
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.TestLogPoint
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets

object MapCoordinateProjector {
    fun usesGcjProjection(tilePreset: TilePreset): Boolean =
        tilePreset.coordinateSystem == TileCoordinateSystem.GCJ02

    fun usesGcjProjection(tilePresetId: TilePresetId): Boolean = when (tilePresetId) {
        TilePresetId.AMAP_ROAD,
        TilePresetId.AMAP_SATELLITE,
        TilePresetId.GOOGLE_PROXY,
        -> true

        TilePresetId.OPEN_STREET_MAP,
        -> false
    }

    fun toDisplayPoint(
        longitude: Double,
        latitude: Double,
        tilePreset: TilePreset,
    ): GeoPoint = if (usesGcjProjection(tilePreset)) {
        ChinaCoordinateTransform.wgs84ToGcj02(longitude, latitude)
    } else {
        GeoPoint(longitude = longitude, latitude = latitude)
    }

    fun toDisplayPoint(
        longitude: Double,
        latitude: Double,
    tilePresetId: TilePresetId,
    ): GeoPoint = toDisplayPoint(longitude, latitude, TilePresets.byId(tilePresetId))

    fun toRawPoint(
        longitude: Double,
        latitude: Double,
        tilePreset: TilePreset,
    ): GeoPoint = if (usesGcjProjection(tilePreset)) {
        ChinaCoordinateTransform.gcj02ToWgs84(longitude, latitude)
    } else {
        GeoPoint(longitude = longitude, latitude = latitude)
    }

    fun toRawPoint(
        longitude: Double,
        latitude: Double,
        tilePresetId: TilePresetId,
    ): GeoPoint = toRawPoint(longitude, latitude, TilePresets.byId(tilePresetId))

    fun toDisplayLocation(
        locationFix: LocationFix?,
        tilePreset: TilePreset,
    ): LocationFix? = locationFix?.let { fix ->
        val point = toDisplayPoint(fix.longitude, fix.latitude, tilePreset)
        fix.copy(
            longitude = point.longitude,
            latitude = point.latitude,
        )
    }

    fun toDisplayLocation(
        locationFix: LocationFix?,
        tilePresetId: TilePresetId,
    ): LocationFix? = toDisplayLocation(locationFix, TilePresets.byId(tilePresetId))

    fun toDisplayTestPoint(
        point: TestLogPoint,
        tilePreset: TilePreset,
    ): TestLogPoint {
        if (!usesGcjProjection(tilePreset)) return point
        val displayPoint = toDisplayPoint(point.longitude, point.latitude, tilePreset)
        return point.copy(
            longitude = displayPoint.longitude,
            latitude = displayPoint.latitude,
        )
    }

    fun toDisplayTestPoint(
        point: TestLogPoint,
        tilePresetId: TilePresetId,
    ): TestLogPoint = toDisplayTestPoint(point, TilePresets.byId(tilePresetId))

    fun toDisplayTestSamples(
        samples: List<TestLogSample>,
        tilePreset: TilePreset,
    ): List<TestLogSample> = if (usesGcjProjection(tilePreset)) {
        samples.map { sample ->
            sample.copy(
                point = sample.point?.let { toDisplayTestPoint(it, tilePreset) },
            )
        }
    } else {
        samples
    }

    fun toDisplayTestSamples(
        samples: List<TestLogSample>,
        tilePresetId: TilePresetId,
    ): List<TestLogSample> = toDisplayTestSamples(samples, TilePresets.byId(tilePresetId))

    fun toDisplaySector(
        record: SectorRecord,
        tilePreset: TilePreset,
    ): SectorRecord {
        if (!usesGcjProjection(tilePreset)) return record
        val center = toDisplayPoint(record.longitude, record.latitude, tilePreset)
        return record.copy(
            longitude = center.longitude,
            latitude = center.latitude,
            polygonCoordinates = record.polygonCoordinates.map { point ->
                toDisplayPoint(point.longitude, point.latitude, tilePreset)
            },
        )
    }

    fun toDisplaySector(
        record: SectorRecord,
        tilePresetId: TilePresetId,
    ): SectorRecord = toDisplaySector(record, TilePresets.byId(tilePresetId))

    fun toDisplaySectors(
        records: List<SectorRecord>,
        tilePreset: TilePreset,
    ): List<SectorRecord> = if (usesGcjProjection(tilePreset)) {
        records.map { toDisplaySector(it, tilePreset) }
    } else {
        records
    }

    fun toDisplaySectors(
        records: List<SectorRecord>,
        tilePresetId: TilePresetId,
    ): List<SectorRecord> = toDisplaySectors(records, TilePresets.byId(tilePresetId))

    fun toDisplaySimMatches(
        matches: List<SimCellMatch>,
        tilePreset: TilePreset,
    ): List<SimCellMatch> = if (usesGcjProjection(tilePreset)) {
        matches.map { match ->
            match.copy(
                servingSector = match.servingSector?.let { toDisplaySector(it, tilePreset) },
                neighborMatches = match.neighborMatches.map { sector ->
                    sector?.let { toDisplaySector(it, tilePreset) }
                },
                neighborSectors = match.neighborSectors.map { toDisplaySector(it, tilePreset) },
            )
        }
    } else {
        matches
    }

    fun toDisplaySimMatches(
        matches: List<SimCellMatch>,
        tilePresetId: TilePresetId,
    ): List<SimCellMatch> = toDisplaySimMatches(matches, TilePresets.byId(tilePresetId))
}
