package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapLayerRenderLimiterTest {
    @Test
    fun `symbol layers include every unrestricted layer record even when normal limit is lower`() {
        val records = listOf(
            record("full-1", "full"),
            record("full-2", "full"),
            record("normal-1", "normal"),
            record("normal-2", "normal"),
            record("normal-3", "normal"),
        )

        val selected = MapLayerRenderLimiter.selectForSymbolLayer(
            records = records,
            limit = 2,
            alwaysIncludeLayerIds = setOf("full"),
        )

        assertEquals(
            listOf("full-1", "full-2", "normal-1", "normal-2"),
            selected.map { it.id },
        )
    }

    @Test
    fun `symbol layers still show unrestricted layer records when zoom policy normal limit is zero`() {
        val selected = MapLayerRenderLimiter.selectForSymbolLayer(
            records = listOf(record("full-1", "full"), record("normal-1", "normal")),
            limit = 0,
            alwaysIncludeLayerIds = setOf("full"),
        )

        assertEquals(listOf("full-1"), selected.map { it.id })
    }

    @Test
    fun `symbol layers cap unrestricted layer records before adding normal records`() {
        val records = listOf(
            record("full-1", "full"),
            record("full-2", "full"),
            record("full-3", "full"),
            record("normal-1", "normal"),
            record("normal-2", "normal"),
            record("normal-3", "normal"),
        )

        val selected = MapLayerRenderLimiter.selectForSymbolLayer(
            records = records,
            limit = 2,
            alwaysIncludeLayerIds = setOf("full"),
            alwaysIncludeLimit = 2,
        )

        assertEquals(
            listOf("full-1", "full-2", "normal-1", "normal-2"),
            selected.map { it.id },
        )
    }

    @Test
    fun `small layers survive smaller point and label budgets`() {
        val records = (0 until 1000).map { record("engineering-$it", "engineering") } +
            (0 until 12).map { record("point-$it", "points") } + record("sector", "sectors")
        listOf(160, 700).forEach { limit ->
            val selected = MapLayerRenderLimiter.selectForSymbolLayer(records, limit, emptySet())
            assertEquals(limit, selected.size)
            assertEquals(12, selected.count { it.layerId == "points" })
            assertTrue(selected.any { it.id == "sector" })
        }
    }

    @Test
    fun `multiple large layers share the cap without multiplying it`() {
        val records = (0 until 10).flatMap { layer ->
            (0 until 500).map { record("$layer-$it", "layer-$layer") }
        }
        val selected = MapLayerRenderLimiter.selectForSymbolLayer(records, 160, emptySet())
        assertEquals(160, selected.size)
        assertTrue(selected.groupingBy { it.layerId }.eachCount().values.all { it == 16 })
        assertEquals(0, MapLayerRenderLimiter.selectForSymbolLayer(records, 0, emptySet()).size)
    }

    @Test
    fun `full layer point budget also protects a small full layer`() {
        val records = (0 until 1000).map { record("large-$it", "large") } + record("poi", "small")
        val selected = MapLayerRenderLimiter.selectForSymbolLayer(records, 0, setOf("large", "small"), 160)
        assertEquals(160, selected.size)
        assertTrue(selected.any { it.id == "poi" })
    }

    private fun record(id: String, layerId: String) = SectorRecord(
        id = id,
        layerId = layerId,
        radioTechnology = RadioTechnology.UNKNOWN,
        longitude = 120.8,
        latitude = 34.4,
        isPointOnly = true,
    )
}
