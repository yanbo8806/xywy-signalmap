package com.xywy.signalmap.data.tile

import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TileSourceCatalogTest {
    @Test
    fun `catalog exposes stable source ids for raster presets and overlays`() {
        val allSources = TileSourceCatalog.allSources()

        assertTrue(allSources.any { it.sourceId == "preset-amap-road-base" })
        assertTrue(allSources.any { it.sourceId == "preset-amap-satellite-base" })
        assertTrue(allSources.any { it.sourceId == "preset-amap-satellite-overlay-amap-label" })
        assertTrue(allSources.any { it.sourceId == "preset-open-street-map-base" })
    }

    @Test
    fun `proxy url rewriting keeps preset structure and swaps urls to localhost`() {
        val preset = TilePresets.byId(TilePresetId.AMAP_SATELLITE)

        val proxied = TileSourceCatalog.rewriteForProxy(
            preset = preset,
            proxyConfig = TileProxyConfig(host = "127.0.0.1", port = 18765),
        )

        assertEquals("http://127.0.0.1:18765/tiles/preset-amap-satellite-base/{z}/{x}/{y}", proxied.tiles.single())
        val overlay = proxied.overlays.singleOrNull()
        assertNotNull(overlay)
        assertEquals(
            "http://127.0.0.1:18765/tiles/preset-amap-satellite-overlay-amap-label/{z}/{x}/{y}",
            overlay.tiles.single(),
        )
    }
}
