package com.xywy.signalmap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TilePresetTest {
    @Test
    fun `amap road preset contains raster templates`() {
        val preset = TilePresets.byId(TilePresetId.AMAP_ROAD)
        assertTrue(preset.tiles.any { it.contains("autonavi") })
    }

    @Test
    fun `legacy source setting falls back to public road tiles`() {
        assertEquals(TilePresetId.AMAP_ROAD, TilePresets.legacyKeyToPreset("google-proxy")?.id)
    }

    @Test
    fun `selectable presets do not include osm`() {
        assertTrue(TilePresets.selectable.none { it.id == TilePresetId.OPEN_STREET_MAP })
        assertTrue(TilePresets.selectable.any { it.id == TilePresetId.AMAP_ROAD })
    }
}
