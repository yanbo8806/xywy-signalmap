package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.EngineeringLayerBinding
import com.xywy.signalmap.domain.model.EngineeringLayerBindingMode
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimEngineeringBindings
import com.xywy.signalmap.domain.model.SimTechEngineeringBinding
import com.xywy.signalmap.domain.model.SimCellSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimCellMatcherTest {
    @Test
    fun `match keeps serving sectors separated by sim`() {
        val sectors = listOf(
            SectorRecord(
                id = "a",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "101",
                cellName = "LTE-A",
                pci = "11",
                longitude = 120.8,
                latitude = 34.4,
            ),
            SectorRecord(
                id = "b",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "202",
                cellName = "LTE-B",
                pci = "22",
                longitude = 120.81,
                latitude = 34.41,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "101", pci = "11"),
            ),
            SimCellSnapshot(
                subscriptionId = 2,
                slotIndex = 1,
                label = "SIM2",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "202", pci = "22"),
            ),
        )

        val matches = SimCellMatcher.match(sims, sectors, locationFix(120.8, 34.4))

        assertEquals(2, matches.size)
        assertEquals("LTE-A", matches.first { it.slotIndex == 0 }.servingSector?.cellName)
        assertEquals("LTE-B", matches.first { it.slotIndex == 1 }.servingSector?.cellName)
        assertNotNull(matches.first { it.slotIndex == 0 }.servingCell)
    }

    @Test
    fun `serving sector is matched by exact cell identity before pci`() {
        val sectors = listOf(
            SectorRecord(
                id = "wrong-pci-near",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "880001",
                cellName = "Wrong PCI",
                pci = "101",
                longitude = 120.8002,
                latitude = 34.4002,
            ),
            SectorRecord(
                id = "exact-nci",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "990001",
                cellName = "Exact NCI",
                pci = "22",
                longitude = 120.8010,
                latitude = 34.4010,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "990001",
                    pci = "101",
                ),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(longitude = 120.8000, latitude = 34.4000),
        )

        assertEquals("Exact NCI", matches.single().servingSector?.cellName)
    }

    @Test
    fun `serving sector matches lte engineering record imported with cgi identity`() {
        val sectors = listOf(
            SectorRecord(
                id = "lte-cgi",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "001-01-000250-76",
                cellName = "LTE CGI Sector",
                pci = "999",
                longitude = 120.76032,
                latitude = 34.37364,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.LTE,
                    registered = true,
                    cellId = "64076",
                    pci = "120",
                ),
            ),
        )

        val matches = SimCellMatcher.match(simSnapshots = sims, sectors = sectors,
            currentLocation = locationFix(sectors.first().longitude, sectors.first().latitude))

        assertEquals("LTE CGI Sector", matches.single().servingSector?.cellName)
    }

    @Test
    fun `serving sector matches nr engineering record imported with cgi identity`() {
        val sectors = listOf(
            SectorRecord(
                id = "nr-cgi",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "001-01-000020-20",
                cellName = "NR CGI Sector",
                pci = "999",
                longitude = 120.08222,
                latitude = 35.03611,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "81940",
                    pci = "558",
                ),
            ),
        )

        val matches = SimCellMatcher.match(simSnapshots = sims, sectors = sectors,
            currentLocation = locationFix(sectors.first().longitude, sectors.first().latitude))

        assertEquals("NR CGI Sector", matches.single().servingSector?.cellName)
    }

    @Test
    fun `serving sector prefers the named record when identity aliases are duplicated`() {
        val sectors = listOf(
            SectorRecord(
                id = "blank-near",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "TEST-NCI-01",
                cellName = "",
                siteName = "",
                pci = "697",
                band = "N41",
                longitude = 120.70440,
                latitude = 34.29488,
            ),
            SectorRecord(
                id = "named-far",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "TEST-NCI-01",
                cellName = "示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02-0",
                siteName = "示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02",
                pci = "697",
                band = "N41",
                longitude = 120.70490,
                latitude = 34.29490,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "TEST-NCI-01",
                    pci = "697",
                    band = "N41",
                ),
            ),
        )

        val matches = SimCellMatcher.match(simSnapshots = sims, sectors = sectors,
            currentLocation = locationFix(sectors.first().longitude, sectors.first().latitude))

        assertEquals("示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02-0", matches.single().servingSector?.cellName)
    }

    @Test
    fun `neighbor sector falls back to pci plus nearest distance`() {
        val sectors = listOf(
            SectorRecord(
                id = "serving",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "1001",
                cellName = "Serving LTE",
                pci = "11",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8000,
                latitude = 34.4000,
            ),
            SectorRecord(
                id = "neighbor-near",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "2001",
                cellName = "Neighbor Near",
                pci = "33",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8005,
                latitude = 34.4004,
            ),
            SectorRecord(
                id = "neighbor-far",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "2002",
                cellName = "Neighbor Far",
                pci = "33",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8300,
                latitude = 34.4200,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.LTE,
                    registered = true,
                    cellId = "1001",
                    pci = "11",
                    band = "B3",
                    channelNumber = "1850",
                ),
                neighbors = listOf(
                    CellSignal(
                        radioTechnology = RadioTechnology.LTE,
                        registered = false,
                        pci = "33",
                        band = "B3",
                        channelNumber = "1850",
                    ),
                ),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(longitude = 120.8001, latitude = 34.4001),
        )

        assertEquals("Neighbor Near", matches.single().neighborMatches.single()?.cellName)
        assertEquals("Neighbor Near", matches.single().neighborSectors.single().cellName)
    }

    @Test
    fun `neighbor sector only matches same pci same band same channel nearest sector`() {
        val sectors = listOf(
            SectorRecord(
                id = "serving",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "1001",
                cellName = "Serving LTE",
                pci = "11",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8000,
                latitude = 34.4000,
            ),
            SectorRecord(
                id = "wrong-channel-near",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "2001",
                cellName = "Wrong Channel",
                pci = "33",
                band = "B3",
                channelNumber = "1650",
                longitude = 120.8002,
                latitude = 34.4002,
            ),
            SectorRecord(
                id = "right-channel-near",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "2002",
                cellName = "Right Channel Near",
                pci = "33",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8005,
                latitude = 34.4004,
            ),
            SectorRecord(
                id = "right-channel-far",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                cellId = "2003",
                cellName = "Right Channel Far",
                pci = "33",
                band = "B3",
                channelNumber = "1850",
                longitude = 120.8300,
                latitude = 34.4200,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.LTE,
                    registered = true,
                    cellId = "1001",
                    pci = "11",
                    band = "B3",
                    channelNumber = "1850",
                ),
                neighbors = listOf(
                    CellSignal(
                        radioTechnology = RadioTechnology.LTE,
                        registered = false,
                        pci = "33",
                        band = "B3",
                        channelNumber = "1850",
                    ),
                ),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(longitude = 120.8001, latitude = 34.4001),
        )

        assertEquals("Right Channel Near", matches.single().neighborMatches.single()?.cellName)
    }

    @Test
    fun `neighbor sector uses pci and location when band or channel is missing`() {
        val sectors = listOf(
            SectorRecord(
                id = "neighbor",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                cellId = "500001",
                cellName = "Neighbor NR",
                pci = "201",
                band = "N41",
                channelNumber = "520000",
                longitude = 120.8005,
                latitude = 34.4005,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                neighbors = listOf(
                    CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = false,
                        pci = "201",
                        band = "N41",
                    ),
                    CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = false,
                        pci = "201",
                        channelNumber = "520000",
                    ),
                ),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(longitude = 120.8001, latitude = 34.4001),
        )

        assertEquals(listOf("neighbor", "neighbor"), matches.single().neighborMatches.map { it?.id })
        assertEquals(listOf("neighbor"), matches.single().neighborSectors.map { it.id })
    }

    @Test
    fun `match can bind sim and technology to specific engineering layers`() {
        val sectors = listOf(
            SectorRecord(
                id = "sim1-lte",
                layerId = "layer-a",
                radioTechnology = RadioTechnology.LTE,
                cellId = "101",
                cellName = "SIM1 LTE",
                pci = "11",
                longitude = 120.8,
                latitude = 34.4,
            ),
            SectorRecord(
                id = "sim2-lte",
                layerId = "layer-b",
                radioTechnology = RadioTechnology.LTE,
                cellId = "202",
                cellName = "SIM2 LTE",
                pci = "22",
                longitude = 120.81,
                latitude = 34.41,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "101", pci = "11"),
            ),
            SimCellSnapshot(
                subscriptionId = 2,
                slotIndex = 1,
                label = "SIM2",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "202", pci = "22"),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(120.8, 34.4),
            layerBindings = SimEngineeringBindings(
                perSlot = mapOf(
                    0 to SimTechEngineeringBinding(
                        lte = EngineeringLayerBinding(
                            mode = EngineeringLayerBindingMode.SPECIFIC_LAYER,
                            layerId = "layer-a",
                        ),
                    ),
                    1 to SimTechEngineeringBinding(
                        lte = EngineeringLayerBinding(
                            mode = EngineeringLayerBindingMode.SPECIFIC_LAYER,
                            layerId = "layer-b",
                        ),
                    ),
                ),
            ),
        )

        assertEquals("SIM1 LTE", matches.first { it.slotIndex == 0 }.servingSector?.cellName)
        assertEquals("SIM2 LTE", matches.first { it.slotIndex == 1 }.servingSector?.cellName)
    }

    @Test
    fun `match supports disabling engineering matching per sim technology`() {
        val sectors = listOf(
            SectorRecord(
                id = "nr-1",
                layerId = "layer-a",
                radioTechnology = RadioTechnology.NR,
                cellId = "990001",
                cellName = "Exact NCI",
                pci = "101",
                longitude = 120.8010,
                latitude = 34.4010,
            ),
        )
        val sims = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "990001",
                    pci = "101",
                ),
            ),
        )

        val matches = SimCellMatcher.match(
            simSnapshots = sims,
            sectors = sectors,
            currentLocation = locationFix(120.8, 34.4),
            layerBindings = SimEngineeringBindings(
                perSlot = mapOf(
                    0 to SimTechEngineeringBinding(
                        nr = EngineeringLayerBinding(mode = EngineeringLayerBindingMode.DISABLED),
                    ),
                ),
            ),
        )

        assertNull(matches.single().servingSector)
    }

    private fun locationFix(
        longitude: Double,
        latitude: Double,
    ): LocationFix = LocationFix(
        longitude = longitude,
        latitude = latitude,
        accuracyMeters = 5f,
        timeMillis = 1_000L,
    )
}
