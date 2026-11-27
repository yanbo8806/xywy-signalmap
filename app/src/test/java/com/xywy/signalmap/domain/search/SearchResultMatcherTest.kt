package com.xywy.signalmap.domain.search

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultMatcherTest {
    @Test
    fun `search returns at most twenty matching records`() {
        val records = (1..30).map { index ->
            SectorRecord(
                id = "id-$index",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellName = "测试小区$index",
                longitude = 120.8 + index * 0.0001,
                latitude = 34.4 + index * 0.0001,
            )
        }

        val results = SearchResultMatcher.search(
            records = records,
            query = "测试小区",
        )

        assertEquals(20, results.size)
        assertEquals("id-1", results.first().id)
        assertEquals("id-20", results.last().id)
    }

    @Test
    fun `blank query returns no results`() {
        val results = SearchResultMatcher.search(
            records = listOf(
                SectorRecord(
                    id = "id-1",
                    layerId = "layer",
                    radioTechnology = RadioTechnology.NR,
                    cellName = "N41扇区",
                    longitude = 120.8,
                    latitude = 34.4,
                ),
            ),
            query = "   ",
        )

        assertEquals(emptyList<SectorRecord>(), results)
    }

    @Test
    fun `search trims surrounding whitespace before matching`() {
        val record = SectorRecord(
            id = "id-1",
            layerId = "layer",
            radioTechnology = RadioTechnology.LTE,
            cellName = "Test Sector",
            longitude = 120.8,
            latitude = 34.4,
        )

        val results = SearchResultMatcher.search(
            records = listOf(record),
            query = "  Test Sector  ",
        )

        assertEquals(listOf(record), results)
    }
}
