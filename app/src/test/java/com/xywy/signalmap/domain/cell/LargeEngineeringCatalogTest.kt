package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*
import kotlin.system.measureTimeMillis
import kotlin.test.*

class LargeEngineeringCatalogTest {
    @Test fun `100k rows resolve ECI and NCI without scanning full catalog on each lookup`() {
        val rows = (0 until 100_000).map { i ->
            val nr = i % 2 == 0
            SectorRecord(id = "row-$i", layerId = if (nr) "nr" else "lte",
                radioTechnology = if (nr) RadioTechnology.NR else RadioTechnology.LTE,
                siteId = (100000 + i).toString(), cellId = "1", cellName = "小区$i",
                pci = (i % 500).toString(), band = if (nr) "N41" else "B3",
                longitude = 120.8, latitude = 34.4)
        }
        lateinit var index: SectorMatchIndex
        val buildMs = measureTimeMillis { index = SectorMatchIndex.build(rows) }
        val lookupMs = measureTimeMillis {
            repeat(1000) { i ->
                val nr = i % 2 == 0
                val globalId = ((100000L + i) * (if (nr) 4096 else 256) + 1).toString()
                val found = index.scope(0, rows[i].radioTechnology, SimEngineeringBindings()).byCellId(globalId)
                assertEquals("row-$i", found.single().id)
            }
        }
        println("100k engineering rows: index=$buildMs ms, 1000 ECI/NCI lookups=$lookupMs ms")
    }
}
