package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.SectorRecord
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Geometry
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.MultiPoint
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.hypot

data class ScreenPoint(
    val x: Float,
    val y: Float,
)

data class MapSelectionCandidate(
    val record: SectorRecord,
    val distancePx: Float,
)

object MapFeatureSelectionResolver {
    fun selectionRadiusPx(zoom: Double, density: Float): Float {
        val dp = when {
            zoom >= 17.0 -> 24f
            zoom >= 15.0 -> 28f
            zoom >= 13.0 -> 32f
            else -> 40f
        }
        return dp * density
    }

    fun resolveCandidates(
        pressPoint: ScreenPoint,
        hits: List<Feature>,
        rawRecordsById: Map<String, SectorRecord>,
        displayRecordsById: Map<String, SectorRecord>,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
        limit: Int = 12,
        isSelectable: (SectorRecord) -> Boolean = { true },
    ): List<MapSelectionCandidate> {
        if (hits.isEmpty()) return emptyList()
        return finalizeCandidates(
            hits.mapNotNull { feature ->
                val recordId = feature.getStringProperty("recordId").orEmpty().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val rawRecord = rawRecordsById[recordId] ?: displayRecordsById[recordId] ?: return@mapNotNull null
                if (!isSelectable(rawRecord)) return@mapNotNull null
                val displayRecord = displayRecordsById[recordId] ?: rawRecord
                val distance = geometryDistancePx(
                    geometry = feature.geometry(),
                    pressPoint = pressPoint,
                    projector = projector,
                ) ?: pointDistance(
                    pressPoint,
                    projector(displayRecord.longitude, displayRecord.latitude),
                )
                MapSelectionCandidate(record = rawRecord, distancePx = distance)
            },
            limit = limit,
        )
    }

    fun resolveVisibleCandidates(
        pressPoint: ScreenPoint,
        rawRecordsById: Map<String, SectorRecord>,
        displayRecordsById: Map<String, SectorRecord>,
        radiusPx: Float,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
        limit: Int = 12,
    ): List<MapSelectionCandidate> {
        return finalizeCandidates(
            displayRecordsById.values.mapNotNull { displayRecord ->
                val distance = pointDistance(
                    pressPoint,
                    projector(displayRecord.longitude, displayRecord.latitude),
                )
                if (distance > radiusPx) return@mapNotNull null
                val rawRecord = rawRecordsById[displayRecord.id] ?: displayRecord
                MapSelectionCandidate(record = rawRecord, distancePx = distance)
            },
            limit = limit,
        )
    }

    private fun finalizeCandidates(
        candidates: List<MapSelectionCandidate>,
        limit: Int,
    ): List<MapSelectionCandidate> {
        if (candidates.isEmpty() || limit <= 0) return emptyList()
        val nearestByRecordId = linkedMapOf<String, MapSelectionCandidate>()
        candidates.forEach { candidate ->
            val current = nearestByRecordId[candidate.record.id]
            if (current == null || candidate.distancePx < current.distancePx) {
                nearestByRecordId[candidate.record.id] = candidate
            }
        }
        return nearestByRecordId.values
            .sortedBy { it.distancePx }
            .take(limit)
    }

    private fun geometryCandidates(
        pressPoint: ScreenPoint,
        hits: List<Feature>,
        rawRecordsById: Map<String, SectorRecord>,
        displayRecordsById: Map<String, SectorRecord>,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
    ): List<MapSelectionCandidate> {
        return hits.mapNotNull { feature ->
            val recordId = feature.getStringProperty("recordId").orEmpty().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val rawRecord = rawRecordsById[recordId] ?: displayRecordsById[recordId] ?: return@mapNotNull null
            val displayRecord = displayRecordsById[recordId] ?: rawRecord
            val distance = geometryDistancePx(
                geometry = feature.geometry(),
                pressPoint = pressPoint,
                projector = projector,
            ) ?: pointDistance(
                pressPoint,
                projector(displayRecord.longitude, displayRecord.latitude),
            )
            MapSelectionCandidate(record = rawRecord, distancePx = distance)
        }
    }

    private fun geometryDistancePx(
        geometry: Geometry?,
        pressPoint: ScreenPoint,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
    ): Float? = when (geometry) {
        is Point -> pointDistance(pressPoint, projector(geometry.longitude(), geometry.latitude()))
        is MultiPoint -> geometry.coordinates().minOfOrNull { point ->
            pointDistance(pressPoint, projector(point.longitude(), point.latitude()))
        }
        is LineString -> lineDistancePx(pressPoint, geometry.coordinates(), projector)
        is MultiLineString -> geometry.coordinates().minOfOrNull { line ->
            lineDistancePx(pressPoint, line, projector)
        }
        is Polygon -> polygonDistancePx(pressPoint, geometry.coordinates(), projector)
        is MultiPolygon -> geometry.coordinates().minOfOrNull { polygon ->
            polygonDistancePx(pressPoint, polygon, projector)
        }
        else -> null
    }

    private fun lineDistancePx(
        pressPoint: ScreenPoint,
        coordinates: List<Point>,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
    ): Float {
        val screenPoints = coordinates.map { projector(it.longitude(), it.latitude()) }
        return minSegmentDistancePx(pressPoint, screenPoints)
    }

    private fun polygonDistancePx(
        pressPoint: ScreenPoint,
        coordinates: List<List<Point>>,
        projector: (longitude: Double, latitude: Double) -> ScreenPoint,
    ): Float {
        val outerRing = coordinates.firstOrNull().orEmpty().map { projector(it.longitude(), it.latitude()) }
        if (outerRing.isEmpty()) return Float.MAX_VALUE
        if (pointInPolygon(pressPoint, outerRing)) return 0f
        return minSegmentDistancePx(pressPoint, outerRing)
    }

    private fun minSegmentDistancePx(
        pressPoint: ScreenPoint,
        points: List<ScreenPoint>,
    ): Float {
        if (points.isEmpty()) return Float.MAX_VALUE
        if (points.size == 1) return pointDistance(pressPoint, points.first())
        return points.zip(points.drop(1)).minOf { (start, end) ->
            pointToSegmentDistancePx(pressPoint, start, end)
        }
    }

    private fun pointInPolygon(point: ScreenPoint, polygon: List<ScreenPoint>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var previous = polygon.last()
        polygon.forEach { current ->
            val intersects = ((current.y > point.y) != (previous.y > point.y)) &&
                (point.x < (previous.x - current.x) * (point.y - current.y) / ((previous.y - current.y).takeIf { it != 0f } ?: 0.00001f) + current.x)
            if (intersects) inside = !inside
            previous = current
        }
        return inside
    }

    private fun pointToSegmentDistancePx(
        point: ScreenPoint,
        start: ScreenPoint,
        end: ScreenPoint,
    ): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0f && dy == 0f) return pointDistance(point, start)
        val projection = (((point.x - start.x) * dx) + ((point.y - start.y) * dy)) / ((dx * dx) + (dy * dy))
        val clamped = projection.coerceIn(0f, 1f)
        val projected = ScreenPoint(
            x = start.x + dx * clamped,
            y = start.y + dy * clamped,
        )
        return pointDistance(point, projected)
    }

    private fun pointDistance(a: ScreenPoint, b: ScreenPoint): Float = hypot(a.x - b.x, a.y - b.y)
}
