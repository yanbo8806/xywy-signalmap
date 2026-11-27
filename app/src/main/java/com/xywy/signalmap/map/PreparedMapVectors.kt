package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.MapCoordinateProjector
import com.xywy.signalmap.domain.map.MapOverlayRenderPolicy
import com.xywy.signalmap.domain.map.NearbyLayerSelector
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.maplibre.geojson.Feature

/** Identity equality: signal and camera updates must never compare/rebuild all city features. */
class PreparedMapVectors(
    val sectorsJson: String,
    val sitesJson: String,
    val rawRecordsById: Map<String, SectorRecord>,
    val displayRecordsById: Map<String, SectorRecord>,
    val coordinateSystem: TileCoordinateSystem? = null,
) {
    fun forTilePreset(preset: TilePreset): PreparedMapVectors =
        if (this === Empty || coordinateSystem == preset.coordinateSystem) this else Empty

    companion object {
        const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"
        val Empty = PreparedMapVectors(EMPTY_GEOJSON, EMPTY_GEOJSON, emptyMap(), emptyMap())
    }
}

data class MapVectorRenderRequest(
    val records: List<SectorRecord>,
    val layers: List<LayerMetadata>,
    val tilePreset: TilePreset,
    val showSectors: Boolean,
    val showSites: Boolean,
    val showLabels: Boolean,
    val nearbyRecordLimit: Int = 200,
    val center: GeoPoint = GeoPoint(0.0, 0.0),
) {
    fun sameRenderingAs(other: MapVectorRenderRequest): Boolean =
        records === other.records && layers == other.layers && tilePreset == other.tilePreset &&
            showSectors == other.showSectors && showSites == other.showSites && showLabels == other.showLabels &&
            (layers.none { it.isVisible && !it.alwaysRenderAllRecords && it.rasterInfo == null } ||
                (nearbyRecordLimit == other.nearbyRecordLimit && center == other.center))
}

/** Reuse unchanged panels and identical centers; never duplicate full-city preparation for dual maps. */
class PanelMapVectorPreparer {
    private var cache = emptyMap<String, Pair<MapVectorRenderRequest, PreparedMapVectors>>()

    suspend fun prepare(request: MapVectorRenderRequest, centers: Map<String, GeoPoint>): Map<String, PreparedMapVectors> {
        val next = linkedMapOf<String, Pair<MapVectorRenderRequest, PreparedMapVectors>>()
        centers.forEach { (panel, center) ->
            val panelRequest = request.copy(center = center)
            val reusable = next.values.firstOrNull { it.first.sameRenderingAs(panelRequest) }
                ?: cache.values.firstOrNull { it.first.sameRenderingAs(panelRequest) }
            val vectors = reusable?.second ?: MapVectorPreparer.prepare(panelRequest)
            next[panel] = panelRequest to vectors
        }
        currentCoroutineContext().ensureActive()
        cache = next
        return next.mapValues { it.value.second }
    }
}

object MapVectorPreparer {
    /** Called on a background dispatcher, once for both map panels. Keeps only a small geometry batch alive. */
    suspend fun prepare(request: MapVectorRenderRequest): PreparedMapVectors {
        val coroutineContext = currentCoroutineContext()
        coroutineContext.ensureActive()
        if (request.records.isEmpty()) return PreparedMapVectors.Empty
        val layersById = request.layers.associateBy { it.id }
        val rawById = LinkedHashMap<String, SectorRecord>()
        val displayById = LinkedHashMap<String, SectorRecord>()
        val selected = NearbyLayerSelector.select(request.records, request.layers, request.center, request.nearbyRecordLimit)
        selected.forEachIndexed { index, record ->
            if (index % 128 == 0) coroutineContext.ensureActive()
            if (layersById[record.layerId]?.isVisible != true) return@forEachIndexed
            rawById[record.id] = record
            displayById[record.id] = MapCoordinateProjector.toDisplaySector(record, request.tilePreset)
        }
        val records = displayById.values.toList()
        val policy = MapOverlayRenderPolicy.resolve(
            zoom = 0.0, wantSectorFill = request.showSectors, wantSitePoints = request.showSites,
            wantSiteLabels = request.showLabels, wantServingLine = false, wantNeighborLines = false,
            wantPointIcons = records.any { it.isPointOnly },
        )
        val sectorsJson = if (!request.showSectors) PreparedMapVectors.EMPTY_GEOJSON else encode { consume ->
            // Serialize and release each batch rather than retaining a whole-city geometry object graph.
            for (start in records.indices step 128) {
                coroutineContext.ensureActive()
                GeoJsonFeatureFactory.sectorFeatures(records.subList(start, minOf(start + 128, records.size)))
                    .features().orEmpty().forEach(consume)
            }
        }
        coroutineContext.ensureActive()
        val sitesJson = encode { consume ->
            GeoJsonFeatureFactory.forEachSiteRenderFeature(records, layersById, policy) { feature ->
                coroutineContext.ensureActive()
                consume(feature)
            }
        }
        coroutineContext.ensureActive()
        return PreparedMapVectors(sectorsJson, sitesJson, rawById, displayById, request.tilePreset.coordinateSystem)
    }

    private fun encode(writeFeatures: ((Feature) -> Unit) -> Unit): String {
        val json = StringBuilder("{\"type\":\"FeatureCollection\",\"features\":[")
        var first = true
        writeFeatures { feature ->
            if (!first) json.append(',')
            first = false
            json.append(feature.toJson())
        }
        return json.append("]}").toString()
    }
}
