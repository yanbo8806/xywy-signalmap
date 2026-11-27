package com.xywy.signalmap.data.tile

import com.xywy.signalmap.domain.model.CustomTileSourceConfig
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TileSourceCatalogCustomTest {
    @Test
    fun `custom tile source is registered with stable proxy source id`() {
        val customPreset = CustomTileSourceConfig.create(
            existingKey = "custom-test",
            title = "测试",
            urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.png",
            tileSize = 256,
            coordinateSystem = TileCoordinateSystem.GCJ02,
            keySeed = "ignored",
        ).toTilePreset()

        TileSourceCatalog.replaceCustomPresets(listOf(customPreset))

        val sourceId = TileSourceCatalog.baseSourceId(customPreset)
        val source = TileSourceCatalog.findSource(sourceId)

        assertEquals("preset-custom-test-base", sourceId)
        assertNotNull(source)
        assertEquals(listOf("https://tiles.example.com/{z}/{x}/{y}.png"), source.urlTemplates)
    }
}
