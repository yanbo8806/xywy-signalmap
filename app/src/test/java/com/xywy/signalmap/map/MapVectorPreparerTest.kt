package com.xywy.signalmap.map

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.maplibre.geojson.FeatureCollection
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapVectorPreparerTest {
    @Test
    fun `old projection is never submitted to a newly selected coordinate system`() = runBlocking {
        val rows = listOf(record(1, false))
        val osm = TilePresets.byId(TilePresetId.OPEN_STREET_MAP)
        val amap = TilePresets.byId(TilePresetId.AMAP_ROAD)
        val satellite = TilePresets.byId(TilePresetId.AMAP_SATELLITE)
        val wgs = MapVectorPreparer.prepare(request(rows))
        assertSame(wgs, wgs.forTilePreset(osm))
        assertSame(PreparedMapVectors.Empty, wgs.forTilePreset(amap))
        val gcj = MapVectorPreparer.prepare(MapVectorRenderRequest(rows, listOf(layer), amap, true, true, true))
        assertSame(gcj, gcj.forTilePreset(amap))
        assertSame(gcj, gcj.forTilePreset(satellite))
        assertSame(PreparedMapVectors.Empty, gcj.forTilePreset(osm))
        assertSame(PreparedMapVectors.Empty, gcj.forTilePreset(amap.copy(coordinateSystem = osm.coordinateSystem)))
    }

    @Test
    fun `removed layer metadata cannot resurrect stale rows during asynchronous preparation`() = runBlocking {
        val prepared = MapVectorPreparer.prepare(request(listOf(record(1, false)), layers = emptyList()))
        assertTrue(prepared.rawRecordsById.isEmpty())
        assertTrue(FeatureCollection.fromJson(prepared.sectorsJson).features().orEmpty().isEmpty())
        assertTrue(FeatureCollection.fromJson(prepared.sitesJson).features().orEmpty().isEmpty())
    }

    @Test
    fun `a mixed layer retains a point icon sharing a station location when engineering sites are off`() = runBlocking {
        val sector = record(1, false).copy(siteName = "同一站点", siteId = "site")
        val point = sector.copy(id = "point", isPointOnly = true, rawFields = mapOf("icon" to "GREEN_PIN"))
        val prepared = MapVectorPreparer.prepare(request(listOf(sector, point), showSites = false))
        val icons = FeatureCollection.fromJson(prepared.sitesJson).features().orEmpty().filter { it.getBooleanProperty("showIcon") }
        assertEquals(1, icons.size)
        assertEquals("point", icons.single().getStringProperty("recordId"))
        assertEquals(LayerPointIconPreset.GREEN_PIN.iconId, icons.single().getStringProperty("iconId"))
    }

    @Test
    fun `50k full city features prepare off the UI thread with shared text and point data`() = runBlocking {
        val rows = (0 until 50_000).map { record(it, it % 2 == 0) }
        val request = request(rows)
        var heartbeats = 0
        val heartbeat = launch { while (true) { heartbeats++; delay(1) } }
        lateinit var prepared: PreparedMapVectors
        val elapsed = measureTimeMillis { prepared = withContext(Dispatchers.Default) { MapVectorPreparer.prepare(request) } }
        heartbeat.cancelAndJoin()
        assertTrue(heartbeats > 0, "Background preparation must allow the UI loop to run")
        assertEquals(50_000, prepared.rawRecordsById.size)
        assertEquals(50_000, prepared.displayRecordsById.size)
        assertEquals(25_000, Regex("\"recordId\"").findAll(prepared.sectorsJson).count())
        assertEquals(50_000, Regex("\"recordId\"").findAll(prepared.sitesJson).count())
        assertEquals(50_000, Regex("\"showLabel\":true").findAll(prepared.sitesJson).count())
        assertFalse(prepared.sitesJson.contains("labelId"), "No per-station bitmap IDs may reach the renderer")
        println("50k full-layer preparation: ${elapsed}ms, UI heartbeats=$heartbeats, sources=${prepared.sectorsJson.length + prepared.sitesJson.length} chars")
    }

    @Test
    fun `native text retains titles colors icons visibility and long press identities`() = runBlocking {
        val rows = listOf(record(1, false), record(2, true), record(3, true).copy(layerId = "hidden"))
        val prepared = MapVectorPreparer.prepare(request(rows, layers = listOf(
            layer.copy(labelTextColor = "#00FF00", labelHaloColor = "#111111", pointIconPreset = LayerPointIconPreset.BLUE_PIN),
            layer.copy(id = "hidden", isVisible = false),
        ), showSites = false))
        val sites = FeatureCollection.fromJson(prepared.sitesJson).features().orEmpty()
        assertEquals(setOf("record-1", "record-2"), sites.map { it.getStringProperty("recordId") }.toSet())
        val point = sites.single { it.getStringProperty("recordId") == "record-2" }
        assertTrue(point.getBooleanProperty("showIcon"))
        assertFalse(point.getBooleanProperty("showCircle"))
        assertEquals(rows[1].siteName, point.getStringProperty("title"))
        assertEquals("#00FF00", point.getStringProperty("labelColor"))
        assertEquals("#111111", point.getStringProperty("labelHaloColor"))
        assertEquals(LayerPointIconPreset.BLUE_PIN.iconId, point.getStringProperty("iconId"))
        assertFalse(sites.first { it.getStringProperty("recordId") == "record-1" }.getBooleanProperty("showIcon"))
        assertSame(rows[1], prepared.rawRecordsById["record-2"])
        assertFalse("record-3" in prepared.rawRecordsById)
        val noLabels = MapVectorPreparer.prepare(request(rows.take(2), showLabels = false))
        assertTrue(FeatureCollection.fromJson(noLabels.sitesJson).features().orEmpty().none { it.getBooleanProperty("showLabel") })
    }

    @Test
    fun `network and camera refreshes reuse the same rendering request but edits invalidate it`() {
        val rows = listOf(record(1, false))
        val first = request(rows)
        assertTrue(first.sameRenderingAs(request(rows)))
        assertFalse(first.sameRenderingAs(request(listOf(rows.single().copy(siteName = "新名称")))))
        assertFalse(first.sameRenderingAs(request(rows, showLabels = false)))
        assertFalse(first.sameRenderingAs(request(rows, layers = listOf(layer.copy(pointIconPreset = LayerPointIconPreset.GREEN_PIN)))))
    }

    @Test
    fun `obsolete full city jobs cancel without completing old source uploads`() = runBlocking {
        val rows = (0 until 100_000).map { record(it, false) }
        val job = async(Dispatchers.Default) { MapVectorPreparer.prepare(request(rows)) }
        job.cancel()
        var cancelled = false
        try { job.await() } catch (_: CancellationException) { cancelled = true }
        assertTrue(cancelled)
    }

    @Test
    fun `full geometry and coordinate projection survive worker preparation`() = runBlocking {
        val polygon = record(1, false).copy(polygonCoordinates = listOf(GeoPoint(120.8, 34.4), GeoPoint(120.9, 34.4), GeoPoint(120.9, 34.5)))
        val request = MapVectorRenderRequest(listOf(polygon), listOf(layer), TilePresets.byId(TilePresetId.AMAP_ROAD), true, true, true)
        val prepared = MapVectorPreparer.prepare(request)
        assertEquals("Polygon", FeatureCollection.fromJson(prepared.sectorsJson).features().orEmpty().single().geometry()?.type())
        assertNotEquals(polygon.longitude, prepared.displayRecordsById.getValue(polygon.id).longitude)
        assertSame(polygon, prepared.rawRecordsById.getValue(polygon.id))
    }

    private val layer = LayerMetadata("city", "全市", "city.xlsx", 0, true, 50_000, alwaysRenderAllRecords = true)
    private fun request(
        rows: List<SectorRecord>, layers: List<LayerMetadata> = listOf(layer), showSites: Boolean = true, showLabels: Boolean = true,
    ) = MapVectorRenderRequest(rows, layers, TilePresets.byId(TilePresetId.OPEN_STREET_MAP), true, showSites, showLabels)

    private fun record(index: Int, point: Boolean) = SectorRecord(
        id = "record-$index", layerId = "city", radioTechnology = RadioTechnology.LTE,
        siteName = "示例地区示范区测试基站办公楼9至12F-DEMO-NR-02-$index",
        longitude = 120.0 + (index % 500) * .002, latitude = 34.0 + (index / 500) * .002,
        azimuth = (index % 360).toDouble(), isPointOnly = point,
    )
}
