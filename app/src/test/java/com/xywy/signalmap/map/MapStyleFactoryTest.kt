package com.xywy.signalmap.map

import com.xywy.signalmap.data.tile.TileProxyConfig
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapStyleFactoryTest {
    @Test
    fun `style json contains glyphs endpoint for runtime station labels`() {
        val styleJson = MapStyleFactory.buildStyleJson(TilePresets.byId(TilePresetId.AMAP_ROAD))
        val style = Json.parseToJsonElement(styleJson).jsonObject

        assertTrue(style.containsKey("glyphs"))
        assertEquals(
            "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
            style["glyphs"]?.toString()?.trim('"'),
        )
    }

    @Test
    fun `style json uses proxy tile url when config is provided`() {
        val styleJson = MapStyleFactory.buildStyleJson(
            tilePreset = TilePresets.byId(TilePresetId.AMAP_ROAD),
            proxyConfig = TileProxyConfig(host = "127.0.0.1", port = 18080),
        )

        assertTrue(styleJson.contains("http://127.0.0.1:18080/tiles/preset-amap-road-base/{z}/{x}/{y}"))
        assertTrue(styleJson.contains("http://127.0.0.1:18080/glyphs/{fontstack}/{range}.pbf"))
        assertTrue(styleJson.contains("\"base-raster\""))
    }

    @Test
    fun `legacy source setting falls back to a public 256px tile source`() {
        val styleJson = MapStyleFactory.buildStyleJson(TilePresets.legacyKeyToPreset("google-proxy")!!)
        val style = Json.parseToJsonElement(styleJson).jsonObject
        val baseRaster = style.getValue("sources").jsonObject.getValue("base-raster").jsonObject

        assertEquals(256, baseRaster.getValue("tileSize").toString().toInt())
    }

    @Test
    fun `amap road style keeps 256 tile size`() {
        val styleJson = MapStyleFactory.buildStyleJson(TilePresets.byId(TilePresetId.AMAP_ROAD))
        val style = Json.parseToJsonElement(styleJson).jsonObject
        val baseRaster = style.getValue("sources").jsonObject.getValue("base-raster").jsonObject

        assertEquals(256, baseRaster.getValue("tileSize").toString().toInt())
    }
}
