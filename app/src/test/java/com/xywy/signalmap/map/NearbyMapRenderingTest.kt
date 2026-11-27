package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.NearbyLayerSelector
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import kotlinx.coroutines.runBlocking
import org.maplibre.geojson.FeatureCollection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NearbyMapRenderingTest {
    private val center = GeoPoint(120.0, 34.0)
    private fun layer(id: String) = LayerMetadata(id, id, "$id.xlsx", 0, true, 500)
    private fun rows(id: String, count: Int = 500) = (0 until count).map { index ->
        SectorRecord(id = "$id-$index", layerId = id, radioTechnology = RadioTechnology.UNKNOWN,
            siteName = "$id-$index", longitude = 120.0 + index * .001, latitude = 34.0,
            azimuth = 90.0, isPointOnly = id == "points")
    }
    private fun request(records: List<SectorRecord>, layers: List<LayerMetadata>) = MapVectorRenderRequest(
        records, layers, TilePresets.byId(TilePresetId.OPEN_STREET_MAP), true, true, true, center = center,
    )

    @Test
    fun `default is 200 per layer before projection serialization and hit testing`() = runBlocking {
        val records = rows("points") + rows("sectors") + rows("small", 3)
        val prepared = MapVectorPreparer.prepare(request(records, listOf(layer("points"), layer("sectors"), layer("small"))))
        assertEquals(mapOf("points" to 200, "sectors" to 200, "small" to 3),
            prepared.rawRecordsById.values.groupingBy { it.layerId }.eachCount())
        assertEquals(prepared.rawRecordsById.keys, prepared.displayRecordsById.keys)
        assertEquals(203, FeatureCollection.fromJson(prepared.sectorsJson).features()!!.size)
        assertEquals(403, FeatureCollection.fromJson(prepared.sitesJson).features()!!.size)
        assertTrue("points-199" in prepared.rawRecordsById)
        assertFalse("points-200" in prepared.rawRecordsById)
    }

    @Test
    fun `one common limit applies independently and full switch only bypasses its own layer`() = runBlocking {
        val records = rows("points") + rows("sectors") + rows("hidden")
        val layers = listOf(layer("points"), layer("sectors"), layer("hidden").copy(isVisible = false, alwaysRenderAllRecords = true))
        val normal = request(records, layers).copy(nearbyRecordLimit = 37)
        val selected = MapVectorPreparer.prepare(normal)
        assertEquals(mapOf("points" to 37, "sectors" to 37), selected.rawRecordsById.values.groupingBy { it.layerId }.eachCount())
        val full = normal.copy(layers = layers.map { if (it.id == "points") it.copy(alwaysRenderAllRecords = true) else it })
        val all = MapVectorPreparer.prepare(full)
        assertEquals(mapOf("points" to 500, "sectors" to 37), all.rawRecordsById.values.groupingBy { it.layerId }.eachCount())
        assertEquals(selected.rawRecordsById, MapVectorPreparer.prepare(normal).rawRecordsById)
    }

    @Test
    fun `panning selects nearest to raw map center even for remote layers`() = runBlocking {
        val records = rows("points")
        val initial = request(records, listOf(layer("points"))).copy(nearbyRecordLimit = 1)
        assertEquals(setOf("points-0"), MapVectorPreparer.prepare(initial).rawRecordsById.keys)
        val east = initial.copy(center = GeoPoint(120.499, 34.0), tilePreset = TilePresets.byId(TilePresetId.AMAP_ROAD))
        assertEquals(setOf("points-499"), MapVectorPreparer.prepare(east).rawRecordsById.keys)
        assertEquals(1, MapVectorPreparer.prepare(initial.copy(center = GeoPoint(-100.0, -34.0))).rawRecordsById.size)
        assertFalse(initial.sameRenderingAs(east))
        assertFalse(initial.sameRenderingAs(initial.copy(nearbyRecordLimit = 12)))
    }

    @Test
    fun `split maps select independently and reuse unchanged or identical panel payloads`() = runBlocking {
        val preparer = PanelMapVectorPreparer()
        val request = request(rows("points"), listOf(layer("points"))).copy(nearbyRecordLimit = 1)
        val centers = mapOf("west" to center, "east" to GeoPoint(120.499, 34.0))
        val first = preparer.prepare(request, centers)
        assertEquals(setOf("points-0"), first.getValue("west").rawRecordsById.keys)
        assertEquals(setOf("points-499"), first.getValue("east").rawRecordsById.keys)
        val moved = preparer.prepare(request, centers + ("east" to GeoPoint(120.3, 34.0)))
        assertSame(first.getValue("west"), moved.getValue("west"))
        assertNotEquals(first.getValue("east").rawRecordsById, moved.getValue("east").rawRecordsById)
        val same = preparer.prepare(request, mapOf("west" to center, "east" to center))
        assertSame(same.getValue("west"), same.getValue("east"))
        assertTrue(preparer.prepare(request, emptyMap()).isEmpty())
    }

    @Test
    fun `all mode shares city payload even at different centers`() = runBlocking {
        val request = request(rows("points"), listOf(layer("points").copy(alwaysRenderAllRecords = true)))
        val result = PanelMapVectorPreparer().prepare(request, mapOf("a" to center, "b" to GeoPoint(10.0, 20.0)))
        assertSame(result.getValue("a"), result.getValue("b"))
        assertEquals(500, result.getValue("a").rawRecordsById.size)
    }

    @Test
    fun `nearest selection is deterministic for ties and wraps longitude`() = runBlocking {
        val records = rows("points", 2).map { it.copy(longitude = 120.0) }
        assertEquals(records.take(1), NearbyLayerSelector.select(records, listOf(layer("points")), center, 1))
        val dateline = listOf(records[0].copy(longitude = 170.0), records[1].copy(longitude = -179.9))
        assertEquals(dateline.takeLast(1), NearbyLayerSelector.select(dateline, listOf(layer("points")), GeoPoint(179.9, 34.0), 1))
    }
}
