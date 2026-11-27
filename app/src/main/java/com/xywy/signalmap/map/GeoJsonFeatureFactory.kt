package com.xywy.signalmap.map

import com.xywy.signalmap.domain.cell.SimColorPalette
import com.xywy.signalmap.domain.geo.SectorGeometry
import com.xywy.signalmap.domain.geo.SectorStyleCatalog
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.TestLogCellRecord
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.map.MapMeasurementMode
import com.xywy.signalmap.domain.map.MapMeasurementState
import com.xywy.signalmap.domain.map.TestPointColorScale
import com.xywy.signalmap.domain.map.MapOverlayRenderPolicyState
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.util.Locale
import kotlin.math.absoluteValue

object GeoJsonFeatureFactory {
    private const val NR_LABEL_COLOR = "#FFFFFF"
    private const val NR_LABEL_HALO_COLOR = "#111111"
    private const val LTE_LABEL_COLOR = "#9AD4FF"
    private const val LTE_LABEL_HALO_COLOR = "#D4AF37"
    private const val POINT_LABEL_COLOR = "#FFF7E6"
    private const val POINT_LABEL_HALO_COLOR = "#4C3A2A"
    private const val POINT_COLOR = "#FF7A45"

    fun sectorFeatures(records: List<SectorRecord>): FeatureCollection {
        val features = records
            .filterNot { it.isPointOnly }
            .map { record ->
                if (record.isLineOnly) {
                    val coordinates = record.polygonCoordinates.map { Point.fromLngLat(it.longitude, it.latitude) }
                    return@map Feature.fromGeometry(LineString.fromLngLats(coordinates)).apply {
                        addStringProperty("layerId", record.layerId)
                        addStringProperty("color", SectorStyleCatalog.color(record))
                        addStringProperty("title", record.cellName ?: record.siteName ?: record.id)
                        addStringProperty("recordId", record.id)
                    }
                }
                val ring = when {
                    record.polygonCoordinates.isNotEmpty() -> normalizePolygonRing(record.polygonCoordinates)
                    SectorStyleCatalog.useTriangleShape(record) -> SectorGeometry.buildTriangleRing(
                        longitude = record.longitude,
                        latitude = record.latitude,
                        azimuth = record.azimuth ?: 0.0,
                        radiusMeters = SectorStyleCatalog.radiusMeters(record),
                        beamWidthDegrees = SectorStyleCatalog.beamWidthDegrees(record),
                    )
                    else -> SectorGeometry.buildSectorRing(
                        longitude = record.longitude,
                        latitude = record.latitude,
                        azimuth = record.azimuth ?: 0.0,
                        radiusMeters = SectorStyleCatalog.radiusMeters(record),
                        beamWidthDegrees = SectorStyleCatalog.beamWidthDegrees(record),
                    )
                }.map { Point.fromLngLat(it.first, it.second) }
                Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                    addStringProperty("layerId", record.layerId)
                    addStringProperty("color", SectorStyleCatalog.color(record))
                    addStringProperty("title", record.cellName ?: record.siteName ?: record.id)
                    addStringProperty("recordId", record.id)
                }
            }
        return FeatureCollection.fromFeatures(features)
    }

    fun siteFeatures(records: List<SectorRecord>): FeatureCollection {
        val features = summarizeSites(records).map { site ->
            Feature.fromGeometry(Point.fromLngLat(site.longitude, site.latitude)).apply {
                addStringProperty("color", site.color)
                addStringProperty("title", site.title)
                addStringProperty("recordId", site.recordId)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun siteCircleFeatures(
        records: List<SectorRecord>,
        layersById: Map<String, LayerMetadata> = emptyMap(),
    ): FeatureCollection {
        val features = summarizeSites(
            records = records,
            layersById = layersById,
            includePointOnly = false,
            includeNonPoint = true,
        ).map { site ->
            Feature.fromGeometry(Point.fromLngLat(site.longitude, site.latitude)).apply {
                addStringProperty("color", site.color)
                addStringProperty("title", site.title)
                addStringProperty("recordId", site.recordId)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun siteLabelFeatures(records: List<SectorRecord>): FeatureCollection {
        return siteLabelFeatures(records, emptyMap())
    }

    fun siteLabelFeatures(
        records: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
    ): FeatureCollection {
        val features = summarizeSites(
            records = records,
            layersById = layersById,
            includePointOnly = true,
            includeNonPoint = true,
            respectLabelVisibility = true,
        ).map { site ->
            Feature.fromGeometry(Point.fromLngLat(site.longitude, site.latitude)).apply {
                addStringProperty("title", site.title)
                addStringProperty("labelColor", site.labelColor)
                addStringProperty("labelHaloColor", site.labelHaloColor)
                addStringProperty("labelId", site.labelId)
                addStringProperty("recordId", site.recordId)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun sitePointIconFeatures(
        records: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
    ): FeatureCollection {
        val features = summarizeSites(
            records = records,
            layersById = layersById,
            includePointOnly = true,
            includeNonPoint = true,
        ).map { site ->
            Feature.fromGeometry(Point.fromLngLat(site.longitude, site.latitude)).apply {
                addStringProperty("title", site.title)
                addStringProperty("iconId", site.iconId)
                addStringProperty("recordId", site.recordId)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    /** Share one native source between circles, icons and text instead of indexing each site three times. */
    internal fun forEachSiteRenderFeature(
        records: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
        policy: MapOverlayRenderPolicyState,
        consume: (Feature) -> Unit,
    ) {
        summarizeSites(records, layersById, generateLabelIds = false).forEach { site ->
            val showCircle = !site.isPointOnly && policy.showSitePoints
            val showIcon = if (site.isPointOnly) policy.showPointIcons else policy.showSitePoints
            val showLabel = policy.showSiteLabels && layersById[site.layerId]?.showLabels != false
            if (!showCircle && !showIcon && !showLabel) return@forEach
            consume(Feature.fromGeometry(Point.fromLngLat(site.longitude, site.latitude)).apply {
                addStringProperty("layerId", site.layerId)
                addStringProperty("recordId", site.recordId)
                addStringProperty("title", site.title)
                addStringProperty("color", site.color)
                addStringProperty("iconId", site.iconId)
                addStringProperty("labelColor", site.labelColor)
                addStringProperty("labelHaloColor", site.labelHaloColor)
                addBooleanProperty("showCircle", showCircle)
                addBooleanProperty("showIcon", showIcon)
                addBooleanProperty("showLabel", showLabel)
            })
        }
    }

    fun locationFeatures(locationFix: LocationFix?): FeatureCollection {
        val features = locationFix?.let { fix ->
            listOf(
                Feature.fromGeometry(Point.fromLngLat(fix.longitude, fix.latitude)).apply {
                    addStringProperty("color", "#0057FF")
                },
            )
        }.orEmpty()
        return FeatureCollection.fromFeatures(features)
    }

    fun testLogPointFeatures(
        samples: List<TestLogSample>,
        metricMode: TestMetricMode,
        visibleSlotIndexes: Set<Int> = samples.flatMapTo(linkedSetOf<Int>()) { sample ->
            sample.sims.map { it.slotIndex }
        },
    ): FeatureCollection {
        val features = samples.flatMap { sample ->
            val point = sample.point ?: return@flatMap emptyList()
            val metricRecords = sample.resolveMetricRecords(
                metricMode = metricMode,
                visibleSlotIndexes = visibleSlotIndexes,
            )
            metricRecords.map { metricRecord ->
                Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                    addStringProperty("color", TestPointColorScale.colorHex(metricMode, metricRecord.value))
                    addStringProperty("metricMode", metricMode.name)
                    metricRecord.source?.takeIf(String::isNotBlank)?.let {
                        addStringProperty("metricSource", it)
                    }
                    metricRecord.value?.let { addNumberProperty("metricValue", it) }
                    addNumberProperty("timestampMillis", sample.timestampMillis)
                    addNumberProperty("slotIndex", metricRecord.slotIndex)
                }
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun measurementLineFeatures(state: MapMeasurementState): FeatureCollection {
        val points = state.points.map { Point.fromLngLat(it.longitude, it.latitude) }
        if (points.size < 2) return FeatureCollection.fromFeatures(emptyList())
        val coordinates = when {
            state.mode == MapMeasurementMode.AREA && points.size >= 3 -> points + points.first()
            else -> points
        }
        return FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(LineString.fromLngLats(coordinates)).apply {
                    addStringProperty("color", "#1D9BF0")
                },
            ),
        )
    }

    fun measurementFillFeatures(state: MapMeasurementState): FeatureCollection {
        if (state.mode != MapMeasurementMode.AREA || state.points.size < 3) {
            return FeatureCollection.fromFeatures(emptyList())
        }
        val ring = state.points
            .map { Point.fromLngLat(it.longitude, it.latitude) }
            .let { points -> if (points.first() == points.last()) points else points + points.first() }
        return FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                    addStringProperty("color", "#1D9BF0")
                },
            ),
        )
    }

    fun measurementPointFeatures(state: MapMeasurementState): FeatureCollection {
        val features = state.points.mapIndexed { index, point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addNumberProperty("index", index)
                addStringProperty("color", "#1D9BF0")
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun measurementLabelFeatures(state: MapMeasurementState): FeatureCollection {
        val feature = when (state.mode) {
            MapMeasurementMode.DISTANCE -> {
                val lastPoint = state.points.lastOrNull() ?: return FeatureCollection.fromFeatures(emptyList())
                val title = state.distanceMeters?.takeIf { state.points.size >= 2 }?.let(::formatDistanceText)
                    ?: return FeatureCollection.fromFeatures(emptyList())
                Feature.fromGeometry(Point.fromLngLat(lastPoint.longitude, lastPoint.latitude)).apply {
                    addStringProperty("title", title)
                }
            }

            MapMeasurementMode.AREA -> {
                if (state.points.size < 3) return FeatureCollection.fromFeatures(emptyList())
                val centroid = centroid(state.points) ?: return FeatureCollection.fromFeatures(emptyList())
                val title = state.areaSquareMeters?.let(::formatAreaText) ?: return FeatureCollection.fromFeatures(emptyList())
                Feature.fromGeometry(Point.fromLngLat(centroid.longitude, centroid.latitude)).apply {
                    addStringProperty("title", title)
                }
            }

            MapMeasurementMode.NONE -> return FeatureCollection.fromFeatures(emptyList())
        }
        return FeatureCollection.fromFeatures(listOf(feature))
    }

    fun measurementEndHandleFeatures(state: MapMeasurementState): FeatureCollection {
        if (!state.canFinish) return FeatureCollection.fromFeatures(emptyList())
        val lastPoint = state.points.lastOrNull() ?: return FeatureCollection.fromFeatures(emptyList())
        return FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(Point.fromLngLat(lastPoint.longitude, lastPoint.latitude)).apply {
                    addStringProperty("title", "\u7ED3\u675F")
                },
            ),
        )
    }

    fun userPolygonDraftFeatures(
        points: List<GeoPoint>,
        name: String,
        closeShape: Boolean = true,
    ): UserPolygonDraftFeatureCollections {
        val mapPoints = points.map { Point.fromLngLat(it.longitude, it.latitude) }
        val lineCoordinates = when {
            closeShape && mapPoints.size >= 3 -> mapPoints + mapPoints.first()
            else -> mapPoints
        }
        val lineFeatures = if (lineCoordinates.size >= 2) {
            FeatureCollection.fromFeatures(
                listOf(
                    Feature.fromGeometry(LineString.fromLngLats(lineCoordinates)).apply {
                        addStringProperty("color", USER_DRAFT_COLOR)
                    },
                ),
            )
        } else {
            FeatureCollection.fromFeatures(emptyList())
        }
        val fillFeatures = if (closeShape && mapPoints.size >= 3) {
            val ring = mapPoints + mapPoints.first()
            FeatureCollection.fromFeatures(
                listOf(
                    Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                        addStringProperty("color", USER_DRAFT_COLOR)
                    },
                ),
            )
        } else {
            FeatureCollection.fromFeatures(emptyList())
        }
        val pointFeatures = FeatureCollection.fromFeatures(
            points.mapIndexed { index, point ->
                Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                    addNumberProperty("index", index + 1)
                    addStringProperty("title", (index + 1).toString())
                    addStringProperty("color", USER_DRAFT_COLOR)
                }
            },
        )
        val labelFeatures = points.lastOrNull()?.let { lastPoint ->
            FeatureCollection.fromFeatures(
                listOf(
                    Feature.fromGeometry(Point.fromLngLat(lastPoint.longitude, lastPoint.latitude)).apply {
                        val fallbackName = if (closeShape) "多边形" else "线"
                        addStringProperty("title", "${name.ifBlank { fallbackName }} · ${points.size}点")
                    },
                ),
            )
        } ?: FeatureCollection.fromFeatures(emptyList())
        return UserPolygonDraftFeatureCollections(
            fillFeatures = fillFeatures,
            lineFeatures = lineFeatures,
            pointFeatures = pointFeatures,
            labelFeatures = labelFeatures,
        )
    }

    fun servingLineFeatures(
        locationFix: LocationFix?,
        simMatches: List<SimCellMatch>,
        visibleSlotIndexes: Set<Int> = simMatches.mapTo(linkedSetOf<Int>()) { it.slotIndex },
    ): FeatureCollection {
        val features = simMatches
            .filter { it.slotIndex in visibleSlotIndexes }
            .mapNotNull { match ->
            buildLine(
                locationFix = locationFix,
                sectorRecord = match.servingSector,
                color = SimColorPalette.servingColor(match.slotIndex),
                label = match.label,
            )
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun neighborLineFeatures(
        locationFix: LocationFix?,
        simMatches: List<SimCellMatch>,
        visibleSlotIndexes: Set<Int> = simMatches.mapTo(linkedSetOf<Int>()) { it.slotIndex },
    ): FeatureCollection {
        val features = simMatches
            .filter { it.slotIndex in visibleSlotIndexes }
            .flatMap { match ->
            match.neighborSectors.mapNotNull { neighborSector ->
                buildLine(
                    locationFix = locationFix,
                    sectorRecord = neighborSector,
                    color = SimColorPalette.neighborColor(match.slotIndex),
                    label = match.label,
                )
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    private fun buildLine(
        locationFix: LocationFix?,
        sectorRecord: SectorRecord?,
        color: String,
        label: String,
    ): Feature? {
        if (locationFix == null || sectorRecord == null) return null
        return Feature.fromGeometry(
            LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(locationFix.longitude, locationFix.latitude),
                    sectorLineTargetPoint(sectorRecord).let { target ->
                        Point.fromLngLat(target.first, target.second)
                    },
                ),
            ),
        ).apply {
            addStringProperty("color", color)
            addStringProperty("simLabel", label)
        }
    }

    private fun sectorLineTargetPoint(record: SectorRecord): Pair<Double, Double> {
        if (record.polygonCoordinates.isEmpty() && SectorStyleCatalog.useTriangleShape(record)) {
            return SectorGeometry.triangleBaseMidpoint(
                longitude = record.longitude,
                latitude = record.latitude,
                azimuth = record.azimuth ?: 0.0,
                radiusMeters = SectorStyleCatalog.radiusMeters(record),
                beamWidthDegrees = SectorStyleCatalog.beamWidthDegrees(record),
            )
        }
        return record.longitude to record.latitude
    }

    private fun normalizePolygonRing(points: List<GeoPoint>): List<Pair<Double, Double>> {
        val ring = points.map { it.longitude to it.latitude }
        if (ring.isEmpty()) return emptyList()
        return if (ring.first() == ring.last()) ring else ring + ring.first()
    }

    private fun summarizeSites(
        records: List<SectorRecord>,
        layersById: Map<String, LayerMetadata> = emptyMap(),
        includePointOnly: Boolean = true,
        includeNonPoint: Boolean = true,
        respectLabelVisibility: Boolean = false,
        generateLabelIds: Boolean = true,
    ): List<SiteSummary> {
        if (records.isEmpty()) return emptyList()
        val representatives = linkedMapOf<String, SectorRecord>()
        records.forEach { record ->
            if (!includePointOnly && record.isPointOnly) return@forEach
            if (!includeNonPoint && !record.isPointOnly) return@forEach
            if (respectLabelVisibility && layersById[record.layerId]?.showLabels == false) return@forEach

            val key = siteKey(record)
            val current = representatives[key]
            if (current == null || sitePriority(record) < sitePriority(current)) {
                representatives[key] = record
            }
        }
        return representatives.values.map { representative ->
            val layerStyle = layersById[representative.layerId]
            val title = representative.siteName ?: representative.cellName ?: representative.id
            val labelColor = layerStyle?.labelTextColor ?: resolveLabelColor(representative)
            val labelHaloColor = layerStyle?.labelHaloColor ?: resolveLabelHaloColor(representative)
            SiteSummary(
                layerId = representative.layerId,
                isPointOnly = representative.isPointOnly,
                title = title,
                longitude = representative.longitude,
                latitude = representative.latitude,
                color = resolveSiteColor(representative),
                labelColor = labelColor,
                labelHaloColor = labelHaloColor,
                labelId = if (generateLabelIds) buildLabelId(representative, title, labelColor, labelHaloColor) else "",
                recordId = representative.id,
                iconId = resolvePointIconId(representative, layerStyle),
            )
        }
    }

    private fun resolvePointIconId(
        record: SectorRecord,
        layerStyle: LayerMetadata?,
    ): String {
        val recordPresetName = record.rawFields["icon"] ?: record.rawFields["图标"]
        val recordPreset = recordPresetName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { name ->
                LayerPointIconPreset.entries.firstOrNull { preset ->
                    preset.name.equals(name, ignoreCase = true) ||
                        preset.iconId.equals(name, ignoreCase = true) ||
                        preset.title == name
                }
            }
        return (recordPreset ?: layerStyle?.pointIconPreset ?: LayerPointIconPreset.RED_PIN).iconId
    }

    private fun resolveSiteColor(record: SectorRecord): String {
        return if (record.isPointOnly && record.radioTechnology == RadioTechnology.UNKNOWN) {
            POINT_COLOR
        } else {
            SectorStyleCatalog.color(record)
        }
    }

    private fun resolveLabelColor(record: SectorRecord): String = when {
        record.isPointOnly && record.radioTechnology == RadioTechnology.UNKNOWN -> POINT_LABEL_COLOR
        record.radioTechnology == RadioTechnology.NR -> NR_LABEL_COLOR
        else -> LTE_LABEL_COLOR
    }

    private fun resolveLabelHaloColor(record: SectorRecord): String = when {
        record.isPointOnly && record.radioTechnology == RadioTechnology.UNKNOWN -> POINT_LABEL_HALO_COLOR
        record.radioTechnology == RadioTechnology.NR -> NR_LABEL_HALO_COLOR
        else -> LTE_LABEL_HALO_COLOR
    }

    private fun siteKey(record: SectorRecord): String = buildString {
        append(record.layerId)
        // A point marker must not be replaced by a colocated engineering sector.
        append(if (record.isPointOnly) "|point" else "|site")
        append('|')
        append(record.siteId ?: record.siteName ?: record.cellName ?: record.id)
        append('|')
        append(record.longitude)
        append('|')
        append(record.latitude)
    }

    private fun sitePriority(record: SectorRecord): Int {
        val technologyPriority = when (record.radioTechnology) {
            RadioTechnology.NR -> 0
            RadioTechnology.LTE -> 1
            else -> 2
        }
        return technologyPriority + if (record.isPointOnly) 10 else 0
    }

    private fun buildLabelId(
        record: SectorRecord,
        title: String,
        labelColor: String,
        labelHaloColor: String,
    ): String {
        val base = "${record.layerId}-${record.id}"
        val normalizedBase = base
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { base.hashCode().absoluteValue.toString(16) }
            .take(32)
        val contentKey = listOf(title, labelColor, labelHaloColor).joinToString("|")
        val contentHash = contentKey.hashCode().absoluteValue.toString(16)
        return "site-label-$normalizedBase-$contentHash"
    }

    private data class SiteSummary(
        val layerId: String,
        val isPointOnly: Boolean,
        val title: String,
        val longitude: Double,
        val latitude: Double,
        val color: String,
        val labelColor: String,
        val labelHaloColor: String,
        val labelId: String,
        val recordId: String,
        val iconId: String,
    )

    private data class ResolvedMetricRecord(
        val slotIndex: Int,
        val source: String?,
        val value: Int?,
    )

    private fun centroid(points: List<com.xywy.signalmap.domain.model.GeoPoint>): com.xywy.signalmap.domain.model.GeoPoint? {
        if (points.isEmpty()) return null
        return com.xywy.signalmap.domain.model.GeoPoint(
            longitude = points.map { it.longitude }.average(),
            latitude = points.map { it.latitude }.average(),
        )
    }

    private fun formatDistanceText(value: Double): String = when {
        value >= 1000.0 -> String.format(Locale.CHINA, "%.2f km", value / 1000.0)
        else -> String.format(Locale.CHINA, "%.0f m", value)
    }

    private fun formatAreaText(value: Double): String = when {
        value >= 1_000_000.0 -> String.format(Locale.CHINA, "%.2f km²", value / 1_000_000.0)
        else -> String.format(Locale.CHINA, "%.0f ㎡", value)
    }

    private fun TestLogSample.resolveMetricRecords(
        metricMode: TestMetricMode,
        visibleSlotIndexes: Set<Int>,
    ): List<ResolvedMetricRecord> {
        val visibleServing = sims
            .filter { it.slotIndex in visibleSlotIndexes }
            .sortedBy { it.slotIndex }
            .mapNotNull { sim ->
                sim.servingCell?.let { cell ->
                    ResolvedMetricRecord(
                        slotIndex = sim.slotIndex,
                        source = sim.label.ifBlank { "SIM${sim.slotIndex + 1}" },
                        value = cell.metricValue(metricMode),
                    )
                }
            }
            .filter { it.value != null }
        if (visibleServing.isNotEmpty()) return visibleServing

        if (sims.isEmpty()) {
            val fallbackValue = when (metricMode) {
                TestMetricMode.RSRP -> primaryRsrp
                TestMetricMode.SINR -> primarySinr
            }
            return fallbackValue?.let {
                ResolvedMetricRecord(
                    slotIndex = -1,
                    source = primaryMetricSource,
                    value = it,
                )
            }?.let(::listOf).orEmpty()
        }
        return emptyList()
    }

    private fun TestLogCellRecord.metricValue(metricMode: TestMetricMode): Int? = when (metricMode) {
        TestMetricMode.RSRP -> rsrp
        TestMetricMode.SINR -> sinr
    }

    private const val USER_DRAFT_COLOR = "#FF7A00"
}

data class UserPolygonDraftFeatureCollections(
    val fillFeatures: FeatureCollection,
    val lineFeatures: FeatureCollection,
    val pointFeatures: FeatureCollection,
    val labelFeatures: FeatureCollection,
)
