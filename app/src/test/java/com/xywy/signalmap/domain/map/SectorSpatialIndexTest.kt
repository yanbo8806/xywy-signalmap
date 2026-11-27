package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SectorSpatialIndexTest {
    @Test
    fun `bounds query only returns cells inside viewport`() {
        val records = listOf(
            sector(id = "a", longitude = 120.8000, latitude = 34.4000),
            sector(id = "b", longitude = 120.8003, latitude = 34.4002),
            sector(id = "c", longitude = 120.8100, latitude = 34.4100),
        )

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = assertNotNull(
            index.queryBounds(
            GeoBounds(
                minLongitude = 120.7998,
                maxLongitude = 120.8005,
                minLatitude = 34.3998,
                maxLatitude = 34.4004,
            ),
        ),
        )

        assertEquals(setOf("a", "b"), results.map { it.id }.toSet())
    }

    @Test
    fun `bounds query can filter by layer id`() {
        val records = listOf(
            sector(id = "normal", layerId = "normal", longitude = 120.8000, latitude = 34.4000),
            sector(id = "manual", layerId = "manual", longitude = 120.8001, latitude = 34.4001),
        )

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = assertNotNull(
            index.queryBounds(
                bounds = GeoBounds(
                    minLongitude = 120.7998,
                    maxLongitude = 120.8005,
                    minLatitude = 34.3998,
                    maxLatitude = 34.4004,
                ),
                includeLayerIds = setOf("manual"),
            ),
        )

        assertEquals(listOf("manual"), results.map { it.id })
    }

    @Test
    fun `nearest query returns closest records without scanning all geometry order`() {
        val records = listOf(
            sector(id = "near-1", longitude = 120.8000, latitude = 34.4000),
            sector(id = "near-2", longitude = 120.8002, latitude = 34.4002),
            sector(id = "mid", longitude = 120.8030, latitude = 34.4020),
            sector(id = "far", longitude = 120.8300, latitude = 34.4200),
        )

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = index.queryNearest(
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            limit = 2,
        )

        assertEquals(listOf("near-1", "near-2"), results.map { it.id })
    }

    @Test
    fun `nearest query can exclude already selected ids`() {
        val records = listOf(
            sector(id = "a", longitude = 120.8000, latitude = 34.4000),
            sector(id = "b", longitude = 120.8002, latitude = 34.4002),
            sector(id = "c", longitude = 120.8004, latitude = 34.4004),
        )

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = index.queryNearest(
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            limit = 2,
            excludeIds = setOf("a"),
        )

        assertEquals(listOf("b", "c"), results.map { it.id })
        assertTrue(results.none { it.id == "a" })
    }

    @Test
    fun `nearest query can filter by layer id without include id set`() {
        val records = listOf(
            sector(id = "normal-near", layerId = "normal", longitude = 120.8000, latitude = 34.4000),
            sector(id = "manual-near", layerId = "manual", longitude = 120.8002, latitude = 34.4002),
            sector(id = "manual-far", layerId = "manual", longitude = 120.8200, latitude = 34.4200),
        )

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = index.queryNearest(
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            limit = 2,
            includeLayerIds = setOf("manual"),
        )

        assertEquals(listOf("manual-near", "manual-far"), results.map { it.id })
    }

    @Test
    fun `nearest query returns quickly when focus is far outside indexed bounds`() {
        val records = buildList {
            for (x in 0 until 20) {
                for (y in 0 until 10) {
                    add(
                        sector(
                            id = "s-$x-$y",
                            longitude = 120.8000 + x * 0.00008,
                            latitude = 34.4000 + y * 0.00008,
                        ),
                    )
                }
            }
        }

        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)
        var results = emptyList<SectorRecord>()
        val elapsedMs = measureTimeMillis {
            results = index.queryNearest(
                focusLongitude = -179.0,
                focusLatitude = 0.0,
                limit = 4,
            )
        }

        assertEquals(4, results.size)
        assertTrue(
            elapsedMs < 250,
            "expected nearest query to avoid scanning massive empty rings, elapsed=${elapsedMs}ms",
        )
    }

    private fun sector(
        id: String,
        layerId: String = "layer",
        longitude: Double,
        latitude: Double,
    ) = SectorRecord(
        id = id,
        layerId = layerId,
        radioTechnology = RadioTechnology.LTE,
        longitude = longitude,
        latitude = latitude,
    )
}
