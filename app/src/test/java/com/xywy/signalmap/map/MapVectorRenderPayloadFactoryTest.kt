package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.MapOverlayRenderPolicyState
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class MapVectorRenderPayloadFactoryTest {
    @Test
    fun `creates vector payload off main caller with policy controlled collections`() {
        val layer = LayerMetadata(
            id = "layer",
            name = "手动图层",
            sourceFileName = "manual",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.GREEN_PIN,
        )
        val records = listOf(
            SectorRecord(
                id = "p1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点A",
                longitude = 120.8,
                latitude = 34.4,
                isPointOnly = true,
            ),
        )

        val payload = MapVectorRenderPayloadFactory.create(
            displayRenderedSectors = records,
            displayPointSectors = records,
            displayLabelSectors = records,
            layersById = mapOf(layer.id to layer),
            renderPolicy = MapOverlayRenderPolicyState(
                sectorLimit = 0,
                pointLimit = 1,
                labelLimit = 1,
                showSectorFill = false,
                showSitePoints = true,
                showSiteLabels = true,
                showServingLine = false,
                showNeighborLines = false,
            ),
        )

        assertEquals(0, payload.sectorFeatures.features().orEmpty().size)
        assertEquals(0, payload.circleFeatures.features().orEmpty().size)
        assertEquals(1, payload.pointIconFeatures.features().orEmpty().size)
        assertEquals(1, payload.siteLabelFeatures.features().orEmpty().size)
        assertEquals(0, payload.signature.renderedCount)
        assertEquals(1, payload.signature.pointRenderedCount)
        assertEquals(1, payload.signature.labelRenderedCount)
        assertEquals("point-icon-green-pin", payload.pointIconFeatures.features().orEmpty().single().getStringProperty("iconId"))
    }

    @Test
    fun `style signature includes point only layers even when sector collection is empty`() {
        val layer = LayerMetadata(
            id = "point-layer",
            name = "点图层",
            sourceFileName = "manual",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.BLUE_PIN,
        )
        val records = listOf(
            SectorRecord(
                id = "p1",
                layerId = "point-layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点A",
                longitude = 120.8,
                latitude = 34.4,
                isPointOnly = true,
            ),
        )

        val payload = MapVectorRenderPayloadFactory.create(
            displayRenderedSectors = emptyList(),
            displayPointSectors = records,
            displayLabelSectors = emptyList(),
            layersById = mapOf(layer.id to layer),
            renderPolicy = MapOverlayRenderPolicyState(
                sectorLimit = 0,
                pointLimit = 1,
                labelLimit = 0,
                showSectorFill = false,
                showSitePoints = true,
                showSiteLabels = false,
                showServingLine = false,
                showNeighborLines = false,
            ),
        )

        assertEquals(1, payload.signature.layerStyleCount)
    }

    @Test
    fun `record signature changes when point title or icon changes`() {
        val layer = LayerMetadata(
            id = "point-layer",
            name = "点图层",
            sourceFileName = "manual",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.BLUE_PIN,
        )
        val baseRecord = SectorRecord(
            id = "p1",
            layerId = "point-layer",
            radioTechnology = RadioTechnology.UNKNOWN,
            siteName = "点A",
            longitude = 120.8,
            latitude = 34.4,
            isPointOnly = true,
        )
        val updatedRecord = baseRecord.copy(
            siteName = "点B",
            rawFields = mapOf("icon" to LayerPointIconPreset.PURPLE_PIN.name),
        )

        val firstPayload = MapVectorRenderPayloadFactory.create(
            displayRenderedSectors = emptyList(),
            displayPointSectors = listOf(baseRecord),
            displayLabelSectors = listOf(baseRecord),
            layersById = mapOf(layer.id to layer),
            renderPolicy = mapPolicy,
        )
        val updatedPayload = MapVectorRenderPayloadFactory.create(
            displayRenderedSectors = emptyList(),
            displayPointSectors = listOf(updatedRecord),
            displayLabelSectors = listOf(updatedRecord),
            layersById = mapOf(layer.id to layer),
            renderPolicy = mapPolicy,
        )

        assertEquals(false, firstPayload.signature == updatedPayload.signature)
    }

    private val mapPolicy = MapOverlayRenderPolicyState(
        sectorLimit = 0,
        pointLimit = 1,
        labelLimit = 1,
        showSectorFill = false,
        showSitePoints = true,
        showSiteLabels = true,
        showServingLine = false,
        showNeighborLines = false,
    )
}
