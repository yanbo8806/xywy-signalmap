package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.MapOverlayRenderPolicyState
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.SectorRecord
import org.maplibre.geojson.FeatureCollection

data class MapVectorRenderPayload(
    val signature: MapVectorRenderSignature,
    val sectorFeatures: FeatureCollection,
    val circleFeatures: FeatureCollection,
    val pointIconFeatures: FeatureCollection,
    val siteLabelFeatures: FeatureCollection,
) {
    companion object {
        val Empty: MapVectorRenderPayload = MapVectorRenderPayload(
            signature = MapVectorRenderSignature(),
            sectorFeatures = FeatureCollection.fromFeatures(emptyList()),
            circleFeatures = FeatureCollection.fromFeatures(emptyList()),
            pointIconFeatures = FeatureCollection.fromFeatures(emptyList()),
            siteLabelFeatures = FeatureCollection.fromFeatures(emptyList()),
        )
    }
}

data class MapVectorRenderSignature(
    val showSectorFill: Boolean = false,
    val showSitePoints: Boolean = false,
    val showSiteLabels: Boolean = false,
    val renderedCount: Int = 0,
    val pointRenderedCount: Int = 0,
    val labelRenderedCount: Int = 0,
    val renderedHash: Long = EMPTY_HASH,
    val pointRenderedHash: Long = EMPTY_HASH,
    val labelRenderedHash: Long = EMPTY_HASH,
    val layerStyleCount: Int = 0,
    val layerStyleHash: Long = EMPTY_HASH,
    val showPointIcons: Boolean = false,
) {
    companion object {
        const val EMPTY_HASH: Long = 1125899906842597L
    }
}

object MapVectorRenderPayloadFactory {
    /** Full data path: no viewport, nearest-record selection, zoom budget or label quota. */
    fun createFullLayer(
        displayRecords: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
        renderPolicy: MapOverlayRenderPolicyState,
    ): MapVectorRenderPayload = create(
        displayRenderedSectors = displayRecords.filterNot { it.isPointOnly },
        displayPointSectors = displayRecords,
        displayLabelSectors = displayRecords,
        layersById = layersById,
        renderPolicy = renderPolicy,
    )

    fun create(
        displayRenderedSectors: List<SectorRecord>,
        displayPointSectors: List<SectorRecord>,
        displayLabelSectors: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
        renderPolicy: MapOverlayRenderPolicyState,
    ): MapVectorRenderPayload {
        val sectorRecords = if (renderPolicy.showSectorFill) {
            displayRenderedSectors.filter { layersById[it.layerId]?.isVisible != false }
        } else emptyList()
        val pointRecords = displayPointSectors.filter { record ->
            renderPolicy.showsPoint(record) && layersById[record.layerId]?.isVisible != false
        }
        val labelRecords = if (renderPolicy.showSiteLabels) {
            displayLabelSectors.filter { layersById[it.layerId]?.isVisible != false }
        } else emptyList()
        val styleLayers = collectStyleLayers(
            displayRenderedSectors = sectorRecords,
            displayPointSectors = pointRecords,
            displayLabelSectors = labelRecords,
            layersById = layersById,
        )
        val signature = MapVectorRenderSignature(
            showSectorFill = renderPolicy.showSectorFill,
            showSitePoints = renderPolicy.showSitePoints,
            showPointIcons = renderPolicy.showPointIcons,
            showSiteLabels = renderPolicy.showSiteLabels,
            renderedCount = sectorRecords.size,
            pointRenderedCount = pointRecords.size,
            labelRenderedCount = labelRecords.size,
            renderedHash = recordsHash(sectorRecords),
            pointRenderedHash = recordsHash(pointRecords),
            labelRenderedHash = recordsHash(labelRecords),
            layerStyleCount = styleLayers.size,
            layerStyleHash = styleLayersHash(styleLayers),
        )
        return MapVectorRenderPayload(
            signature = signature,
            sectorFeatures = GeoJsonFeatureFactory.sectorFeatures(sectorRecords),
            circleFeatures = GeoJsonFeatureFactory.siteCircleFeatures(pointRecords, layersById),
            pointIconFeatures = GeoJsonFeatureFactory.sitePointIconFeatures(pointRecords, layersById),
            siteLabelFeatures = GeoJsonFeatureFactory.siteLabelFeatures(labelRecords, layersById),
        )
    }

    private fun collectStyleLayers(
        displayRenderedSectors: List<SectorRecord>,
        displayPointSectors: List<SectorRecord>,
        displayLabelSectors: List<SectorRecord>,
        layersById: Map<String, LayerMetadata>,
    ): List<LayerMetadata> {
        val layerIds = linkedSetOf<String>()
        displayRenderedSectors.forEach { layerIds += it.layerId }
        displayPointSectors.forEach { layerIds += it.layerId }
        displayLabelSectors.forEach { layerIds += it.layerId }
        return layerIds
            .mapNotNull { layersById[it] }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    private fun recordsHash(records: List<SectorRecord>): Long {
        var hash = MapVectorRenderSignature.EMPTY_HASH
        records.forEach { record -> hash = mix(hash, recordHash(record)) }
        return hash
    }

    private fun recordHash(record: SectorRecord): Long {
        var hash = MapVectorRenderSignature.EMPTY_HASH
        hash = mixString(hash, record.id)
        hash = mixString(hash, record.layerId)
        hash = mixString(hash, record.radioTechnology.name)
        hash = mixString(hash, record.band)
        hash = mixLong(hash, record.longitude.toBits())
        hash = mixLong(hash, record.latitude.toBits())
        hash = mixLong(hash, record.azimuth?.toBits() ?: 0L)
        hash = mixInt(hash, if (record.isPointOnly) 1 else 0)
        hash = mixInt(hash, if (record.isLineOnly) 1 else 0)
        hash = mixString(hash, record.siteId)
        hash = mixString(hash, record.siteName)
        hash = mixString(hash, record.cellName)
        hash = mixString(hash, record.rawFields["icon"])
        hash = mixString(hash, record.rawFields["\u56FE\u6807"])
        hash = mixInt(hash, record.polygonCoordinates.hashCode())
        return hash
    }

    private fun styleLayersHash(layers: List<LayerMetadata>): Long {
        var hash = MapVectorRenderSignature.EMPTY_HASH
        layers.forEach { layer ->
            hash = mixString(hash, layer.id)
            hash = mixInt(hash, if (layer.showLabels) 1 else 0)
            hash = mixString(hash, layer.labelTextColor)
            hash = mixString(hash, layer.labelHaloColor)
            hash = mixString(hash, layer.pointIconPreset.name)
        }
        return hash
    }

    private fun mixString(hash: Long, value: String?): Long = mixInt(hash, value?.hashCode() ?: 0)

    private fun mixLong(hash: Long, value: Long): Long = mixInt(mixInt(hash, value.toInt()), (value ushr 32).toInt())

    private fun mixInt(hash: Long, value: Int): Long = mix(hash, value.toLong())

    private fun mix(hash: Long, value: Long): Long = (hash xor value) * 1099511628211L
}
