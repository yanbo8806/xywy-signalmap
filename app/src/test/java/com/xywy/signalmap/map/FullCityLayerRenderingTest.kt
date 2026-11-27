package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.MapOverlayRenderPolicy
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullCityLayerRenderingTest {
    @Test
    fun `full city points sectors and labels exceed old budgets without disappearing`() {
        // Beyond the former 6000 point / 1000 sector / 260 label limits, spread across a city.
        val records = (0 until 8_000).map { index ->
            record(index, if (index % 2 == 0) "points" else "sectors")
        }
        val layers = mapOf(
            "points" to layer("points"),
            "sectors" to layer("sectors"),
        )
        var previousSignature: MapVectorRenderSignature? = null
        listOf(5.2, 8.0, 10.0, 12.0, 16.5).forEach { zoom ->
            val payload = MapVectorRenderPayloadFactory.createFullLayer(records, layers, policy(zoom))
            assertEquals(4_000, payload.sectorFeatures.features().orEmpty().size)
            assertEquals(8_000, payload.pointIconFeatures.features().orEmpty().size)
            assertEquals(8_000, payload.siteLabelFeatures.features().orEmpty().size)
            assertEquals(records.map { it.id }.toSet(), payload.pointIconFeatures.features().orEmpty().map { it.getStringProperty("recordId") }.toSet())
            previousSignature?.let { assertEquals(it, payload.signature, "zoom alone must not regenerate different data") }
            previousSignature = payload.signature
        }
    }

    @Test
    fun `old automatic thinning preference is ignored but manual layer visibility works`() {
        val records = (0 until 8_000).map { record(it, "points") }
        val oldLayer = layer("points").copy(alwaysRenderAllRecords = false)
        val visible = MapVectorRenderPayloadFactory.createFullLayer(records, mapOf("points" to oldLayer), policy(8.0))
        val legacyFull = MapVectorRenderPayloadFactory.createFullLayer(
            records, mapOf("points" to oldLayer.copy(alwaysRenderAllRecords = true)), policy(8.0),
        )
        assertEquals(8_000, visible.pointIconFeatures.features().orEmpty().size)
        assertEquals(visible.signature, legacyFull.signature)
        val hidden = MapVectorRenderPayloadFactory.createFullLayer(
            records, mapOf("points" to oldLayer.copy(isVisible = false)), policy(8.0),
        )
        assertTrue(hidden.pointIconFeatures.features().orEmpty().isEmpty())
        assertTrue(hidden.siteLabelFeatures.features().orEmpty().isEmpty())
        assertTrue(hidden.sectorFeatures.features().orEmpty().isEmpty())
    }

    @Test
    fun `manual label and geometry switches remain effective at city zoom`() {
        val records = listOf(record(1, "sectors"), record(2, "points"))
        val layers = mapOf("sectors" to layer("sectors"), "points" to layer("points"))
        val labelsOff = MapVectorRenderPayloadFactory.createFullLayer(
            records, layers.mapValues { it.value.copy(showLabels = false) }, policy(8.0),
        )
        assertEquals(2, labelsOff.pointIconFeatures.features().orEmpty().size)
        assertTrue(labelsOff.siteLabelFeatures.features().orEmpty().isEmpty())
        val iconsOnlyPolicy = MapOverlayRenderPolicy.resolve(8.0, false, false, false, false, false, wantPointIcons = true)
        val iconsOnly = MapVectorRenderPayloadFactory.createFullLayer(records, layers, iconsOnlyPolicy)
        assertTrue(iconsOnly.sectorFeatures.features().orEmpty().isEmpty())
        assertTrue(iconsOnly.siteLabelFeatures.features().orEmpty().isEmpty())
        assertTrue(iconsOnly.circleFeatures.features().orEmpty().isEmpty())
        assertEquals(1, iconsOnly.pointIconFeatures.features().orEmpty().size)
        assertFalse(iconsOnly.signature.showSitePoints)
    }

    @Test
    fun `polygons and lines remain in the overview data`() {
        val coordinates = listOf(GeoPoint(120.0, 34.0), GeoPoint(121.0, 34.0), GeoPoint(121.0, 35.0))
        val polygon = record(1, "shapes").copy(polygonCoordinates = coordinates, isPointOnly = false)
        val line = polygon.copy(id = "line", polygonCoordinates = coordinates.take(2), rawFields = mapOf("类型" to "线"))
        val payload = MapVectorRenderPayloadFactory.createFullLayer(listOf(polygon, line), mapOf("shapes" to layer("shapes")), policy(5.2))
        assertEquals(setOf("Polygon", "LineString"), payload.sectorFeatures.features().orEmpty().map { it.geometry()?.type() }.toSet())
    }

    private fun policy(zoom: Double) = MapOverlayRenderPolicy.resolve(zoom, true, true, true, true, true)

    private fun record(index: Int, layerId: String) = SectorRecord(
        id = "record-$index", layerId = layerId, radioTechnology = RadioTechnology.LTE,
        siteName = "站点$index", longitude = 120.0 + (index % 100) * .012,
        latitude = 34.0 + (index / 100) * .012, azimuth = (index % 360).toDouble(),
        isPointOnly = layerId == "points",
    )

    private fun layer(id: String) = LayerMetadata(id, id, "city.xlsx", 0, true, 8_000)
}
