package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapFeatureSelectionResolverTest {
    @Test
    fun `hidden stale hits cannot be selected before the native source finishes updating`() {
        val hidden = record("hidden", 0.0, 0.0, "隐藏点")
        val shown = record("shown", 1.0, 0.0, "可见点")
        val records = listOf(hidden, shown).associateBy { it.id }
        val hits = records.values.map {
            Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)).apply { addStringProperty("recordId", it.id) }
        }
        val result = MapFeatureSelectionResolver.resolveCandidates(
            ScreenPoint(0f, 0f), hits, records, records,
            projector = { lon, lat -> ScreenPoint(lon.toFloat(), lat.toFloat()) },
            isSelectable = { it.id != "hidden" },
        )
        assertEquals(listOf("shown"), result.map { it.record.id })
    }

    @Test
    fun `empty rendered hit query does not project any city records`() {
        val records = (0 until 50_000).associate {
            val record = record("point-$it", 120.0 + it * .00001, 34.4, "点$it")
            record.id to record
        }
        var projections = 0
        val result = MapFeatureSelectionResolver.resolveCandidates(
            ScreenPoint(0f, 0f), emptyList(), records, records,
            projector = { _, _ -> projections++; ScreenPoint(0f, 0f) },
        )
        assertTrue(result.isEmpty())
        assertEquals(0, projections)
    }

    @Test
    fun `resolve candidates deduplicates hits and sorts by nearest distance`() {
        val rawRecords = listOf(
            record(id = "a", longitude = 0.0, latitude = 0.0, cellName = "A"),
            record(id = "b", longitude = 18.0, latitude = 0.0, cellName = "B"),
        ).associateBy { it.id }
        val displayRecords = rawRecords

        val hits = listOf(
            Feature.fromGeometry(Point.fromLngLat(0.0, 0.0)).apply { addStringProperty("recordId", "a") },
            Feature.fromGeometry(Point.fromLngLat(0.0, 0.0)).apply { addStringProperty("recordId", "a") },
            Feature.fromGeometry(Point.fromLngLat(18.0, 0.0)).apply { addStringProperty("recordId", "b") },
        )

        val candidates = MapFeatureSelectionResolver.resolveCandidates(
            pressPoint = ScreenPoint(2f, 0f),
            hits = hits,
            rawRecordsById = rawRecords,
            displayRecordsById = displayRecords,
            projector = { longitude, latitude -> ScreenPoint(longitude.toFloat(), latitude.toFloat()) },
        )

        assertEquals(listOf("a", "b"), candidates.map { it.record.id })
        assertEquals(2f, candidates.first().distancePx)
        assertEquals(16f, candidates.last().distancePx)
    }

    @Test
    fun `resolve candidates treats polygon hit area as zero distance`() {
        val polygonRecord = record(id = "sector", longitude = 0.0, latitude = 0.0, cellName = "扇区")
        val pointRecord = record(id = "point", longitude = 6.0, latitude = 0.0, cellName = "点位")
        val rawRecords = listOf(polygonRecord, pointRecord).associateBy { it.id }
        val displayRecords = rawRecords

        val polygon = Polygon.fromLngLats(
            listOf(
                listOf(
                    Point.fromLngLat(-4.0, -4.0),
                    Point.fromLngLat(4.0, -4.0),
                    Point.fromLngLat(4.0, 4.0),
                    Point.fromLngLat(-4.0, 4.0),
                    Point.fromLngLat(-4.0, -4.0),
                ),
            ),
        )
        val hits = listOf(
            Feature.fromGeometry(Point.fromLngLat(6.0, 0.0)).apply { addStringProperty("recordId", "point") },
            Feature.fromGeometry(polygon).apply { addStringProperty("recordId", "sector") },
        )

        val candidates = MapFeatureSelectionResolver.resolveCandidates(
            pressPoint = ScreenPoint(0f, 0f),
            hits = hits,
            rawRecordsById = rawRecords,
            displayRecordsById = displayRecords,
            projector = { longitude, latitude -> ScreenPoint(longitude.toFloat(), latitude.toFloat()) },
        )

        assertEquals(listOf("sector", "point"), candidates.map { it.record.id })
        assertEquals(0f, candidates.first().distancePx)
        assertTrue(candidates.last().distancePx > 0f)
    }

    @Test
    fun `selection radius grows when zoomed out`() {
        assertTrue(MapFeatureSelectionResolver.selectionRadiusPx(12.0, 1f) > MapFeatureSelectionResolver.selectionRadiusPx(17.0, 1f))
    }

    @Test
    fun `fallback candidates can resolve nearest visible records without rendered feature hits`() {
        val rawRecords = listOf(
            record(id = "near", longitude = 10.0, latitude = 10.0, cellName = "Near"),
            record(id = "far", longitude = 80.0, latitude = 80.0, cellName = "Far"),
        ).associateBy { it.id }
        val displayRecords = rawRecords

        val candidates = MapFeatureSelectionResolver.resolveVisibleCandidates(
            pressPoint = ScreenPoint(12f, 11f),
            rawRecordsById = rawRecords,
            displayRecordsById = displayRecords,
            radiusPx = 20f,
            projector = { longitude, latitude -> ScreenPoint(longitude.toFloat(), latitude.toFloat()) },
        )

        assertEquals(listOf("near"), candidates.map { it.record.id })
        assertTrue(candidates.single().distancePx < 5f)
    }

    private fun record(
        id: String,
        longitude: Double,
        latitude: Double,
        cellName: String,
    ): SectorRecord = SectorRecord(
        id = id,
        layerId = "layer",
        radioTechnology = RadioTechnology.LTE,
        cellName = cellName,
        longitude = longitude,
        latitude = latitude,
    )
}
