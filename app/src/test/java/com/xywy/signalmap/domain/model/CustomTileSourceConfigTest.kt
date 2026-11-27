package com.xywy.signalmap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CustomTileSourceConfigTest {
    @Test
    fun `create validates url placeholders and normalizes tile source`() {
        val source = CustomTileSourceConfig.create(
            existingKey = null,
            title = "  测试底图  ",
            urlTemplate = " https://example.com/tiles/{z}/{x}/{y}.png ",
            tileSize = 512,
            coordinateSystem = TileCoordinateSystem.WGS84,
            keySeed = "abc",
        )

        assertEquals("custom-abc", source.key)
        assertEquals("测试底图", source.title)
        assertEquals("https://example.com/tiles/{z}/{x}/{y}.png", source.urlTemplate)
        assertEquals(512, source.tileSize)
        assertEquals(TileCoordinateSystem.WGS84, source.coordinateSystem)
    }

    @Test
    fun `create rejects invalid custom tile url`() {
        assertFailsWith<IllegalArgumentException> {
            CustomTileSourceConfig.create(
                existingKey = null,
                title = "坏底图",
                urlTemplate = "https://example.com/tiles/{z}/{x}.png",
                tileSize = 256,
                coordinateSystem = TileCoordinateSystem.GCJ02,
                keySeed = "bad",
            )
        }
    }

    @Test
    fun `custom source converts to selectable tile preset`() {
        val preset = CustomTileSourceConfig.create(
            existingKey = "custom-fixed",
            title = "自定义",
            urlTemplate = "http://127.0.0.1/{z}/{x}/{y}.png",
            tileSize = 256,
            coordinateSystem = TileCoordinateSystem.GCJ02,
            keySeed = "ignored",
        ).toTilePreset()

        assertEquals("custom-fixed", preset.key)
        assertEquals(null, preset.id)
        assertEquals("自定义", preset.title)
        assertEquals(TileCoordinateSystem.GCJ02, preset.coordinateSystem)
        assertTrue(preset.isCustom)
    }
}
