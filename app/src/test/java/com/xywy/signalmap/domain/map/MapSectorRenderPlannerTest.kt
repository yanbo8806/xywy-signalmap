package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapSectorRenderPlannerTest {
    @Test
    fun `planner fills in viewport first and then nearest remainder using spatial index`() {
        val records = listOf(
            sector(id = "in-1", longitude = 120.8000, latitude = 34.4000),
            sector(id = "in-2", longitude = 120.8002, latitude = 34.4002),
            sector(id = "near-out-1", longitude = 120.8008, latitude = 34.4008),
            sector(id = "near-out-2", longitude = 120.8010, latitude = 34.4010),
            sector(id = "far", longitude = 120.8200, latitude = 34.4200),
        )
        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = MapSectorRenderPlanner.selectSectors(
            records = records,
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            bounds = GeoBounds(
                minLongitude = 120.7999,
                maxLongitude = 120.8003,
                minLatitude = 34.3999,
                maxLatitude = 34.4003,
            ),
            limit = 4,
            spatialIndex = index,
        )

        assertEquals(setOf("in-1", "in-2"), results.take(2).map { it.id }.toSet())
        assertEquals(listOf("near-out-1", "near-out-2"), results.drop(2).map { it.id })
    }

    @Test
    fun `planner falls back to nearest when viewport spans too many grid cells`() {
        val records = listOf(
            sector(id = "near-1", longitude = 120.8000, latitude = 34.4000),
            sector(id = "near-2", longitude = 120.8002, latitude = 34.4002),
            sector(id = "mid", longitude = 120.8300, latitude = 34.4300),
            sector(id = "far", longitude = 121.3000, latitude = 34.9000),
        )
        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = MapSectorRenderPlanner.selectSectors(
            records = records,
            focusLongitude = 120.80005,
            focusLatitude = 34.40005,
            bounds = GeoBounds(
                minLongitude = 70.0,
                maxLongitude = 140.0,
                minLatitude = 10.0,
                maxLatitude = 60.0,
            ),
            limit = 2,
            spatialIndex = index,
        )

        assertEquals(listOf("near-1", "near-2"), results.map { it.id })
    }

    @Test
    fun `planner always includes records from unrestricted layers beyond nearest limit`() {
        val records = listOf(
            sector(id = "near-1", longitude = 120.8000, latitude = 34.4000),
            sector(id = "near-2", longitude = 120.8002, latitude = 34.4002),
            sector(id = "far-normal", longitude = 121.3000, latitude = 34.9000),
            sector(id = "far-user-1", layerId = "user-layer", longitude = 121.4000, latitude = 35.0000),
            sector(id = "far-user-2", layerId = "user-layer", longitude = 121.5000, latitude = 35.1000),
        )
        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = MapSectorRenderPlanner.selectSectors(
            records = records,
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            bounds = null,
            limit = 2,
            spatialIndex = index,
            alwaysIncludeLayerIds = setOf("user-layer"),
        )

        assertEquals(
            listOf("far-user-1", "far-user-2"),
            results.take(2).map { it.id },
        )
        assertEquals(setOf("near-1", "near-2"), results.drop(2).map { it.id }.toSet())
    }

    @Test
    fun `planner caps unrestricted layer records by nearest distance before normal records`() {
        val records = listOf(
            sector(id = "normal-near-1", longitude = 120.8000, latitude = 34.4000),
            sector(id = "normal-near-2", longitude = 120.8002, latitude = 34.4002),
            sector(id = "forced-near", layerId = "full", longitude = 120.8001, latitude = 34.4001),
            sector(id = "forced-mid", layerId = "full", longitude = 120.8200, latitude = 34.4200),
            sector(id = "forced-far", layerId = "full", longitude = 121.3000, latitude = 34.9000),
        )
        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = MapSectorRenderPlanner.selectSectors(
            records = records,
            focusLongitude = 120.8001,
            focusLatitude = 34.4001,
            bounds = null,
            limit = 2,
            spatialIndex = index,
            alwaysIncludeLayerIds = setOf("full"),
            alwaysIncludeLimit = 2,
        )

        assertEquals(
            listOf("forced-near", "forced-mid"),
            results.take(2).map { it.id },
        )
        assertEquals(setOf("normal-near-1", "normal-near-2"), results.drop(2).map { it.id }.toSet())
    }

    @Test
    fun `planner uses spatial index when unrestricted layers are enabled`() {
        val records = buildList {
            repeat(900) { index ->
                add(
                    sector(
                        id = "normal-$index",
                        longitude = 120.60 + (index * 0.0001),
                        latitude = 34.10 + (index * 0.0001),
                    ),
                )
            }
            add(sector(id = "near-normal-a", longitude = 120.8000, latitude = 34.4000))
            add(sector(id = "near-normal-b", longitude = 120.8001, latitude = 34.4001))
            add(sector(id = "user-a", layerId = "user-layer", longitude = 120.8002, latitude = 34.4002))
        }
        val index = SectorSpatialIndex.build(records, cellSizeMeters = 40.0)

        val results = MapSectorRenderPlanner.selectSectors(
            records = records,
            focusLongitude = 120.8000,
            focusLatitude = 34.4000,
            bounds = null,
            limit = 2,
            spatialIndex = index,
            alwaysIncludeLayerIds = setOf("user-layer"),
            alwaysIncludeLimit = 1,
        )

        assertEquals(listOf("user-a", "near-normal-a", "near-normal-b"), results.map { it.id })
    }

    @Test
    fun `small point and sector layers are retained beside dense engineering data`() {
        val records = (0 until 2000).map {
            sector("engineering-$it", longitude = 120.8 + it * 0.000001, latitude = 34.4)
        } + listOf(
            sector("poi", "points", 120.805, 34.401).copy(isPointOnly = true),
            sector("custom-sector", "sectors", 120.806, 34.401),
        )
        val result = MapSectorRenderPlanner.selectSectors(
            records, 120.8, 34.4, GeoBounds(120.79, 120.82, 34.39, 34.42),
            limit = 700, spatialIndex = SectorSpatialIndex.build(records),
        )
        assertEquals(700, result.size)
        assertTrue(result.any { it.id == "poi" })
        assertTrue(result.any { it.id == "custom-sector" })
    }

    @Test
    fun `old spatial index cannot hide newly imported points or revive hidden records`() {
        val old = listOf(sector("old", longitude = 120.8, latitude = 34.4))
        val index = SectorSpatialIndex.build(old)
        val point = sector("new", "points", 120.801, 34.401).copy(isPointOnly = true)
        val imported = MapSectorRenderPlanner.selectSectors(old + point, 120.8, 34.4, null, 10, index)
        assertEquals(setOf("old", "new"), imported.map { it.id }.toSet())
        val hidden = MapSectorRenderPlanner.selectSectors(listOf(point), 120.8, 34.4, null, 10, index)
        assertEquals(listOf("new"), hidden.map { it.id })
    }

    @Test
    fun `viewport priority works without an index and when a large query falls back`() {
        val records = listOf(
            sector("outside-near", longitude = 120.8, latitude = 34.4),
            sector("inside", longitude = 121.0, latitude = 34.6),
        )
        listOf(null, SectorSpatialIndex.build(records)).forEach { index ->
            val result = MapSectorRenderPlanner.selectSectors(
                records, 120.8, 34.4, GeoBounds(120.9, 121.1, 34.5, 34.7), 1, index,
            )
            assertEquals(listOf("inside"), result.map { it.id })
        }
    }

    @Test
    fun `full-layer quota does not leak excluded full records back into normal quota`() {
        val records = (0 until 30).map {
            sector("full-$it", "full", 120.8 + it * .00001, 34.4)
        } + listOf(sector("normal", "normal", 120.81, 34.4))
        val result = MapSectorRenderPlanner.selectSectors(
            records, 120.8, 34.4, GeoBounds(120.7, 120.9, 34.3, 34.5),
            limit = 10, spatialIndex = SectorSpatialIndex.build(records),
            alwaysIncludeLayerIds = setOf("full"), alwaysIncludeLimit = 2,
        )
        assertEquals(listOf("full-0", "full-1", "normal"), result.map { it.id })
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
