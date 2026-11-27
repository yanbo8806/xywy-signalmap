package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLogSampleFactoryTest {
    @Test
    fun `factory captures location matched names and primary metrics`() {
        val sample = TestLogSampleFactory.create(
            timestampMillis = 1_713_676_805_000,
            locationFix = LocationFix(
                longitude = 120.9398,
                latitude = 34.3416,
                accuracyMeters = 6f,
                timeMillis = 1_713_676_805_000,
            ),
            simMatches = listOf(
                SimCellMatch(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1 示例运营商",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = true,
                        cellId = "49152",
                        pci = "704",
                        tacOrLac = "1234",
                        channelNumber = "504990",
                        band = "N41",
                        bandwidth = "100 MHz",
                        rsrp = -97,
                        rsrq = -11,
                        sinr = 16,
                        dbm = -87,
                    ),
                    servingSector = SectorRecord(
                        id = "sector-1",
                        layerId = "layer-1",
                        radioTechnology = RadioTechnology.NR,
                        siteId = "SITE-001",
                        siteName = "示例地区湖站",
                        cellId = "49152",
                        cellName = "示例地区湖-1",
                        longitude = 120.94,
                        latitude = 34.34,
                    ),
                    neighbors = listOf(
                        CellSignal(
                            radioTechnology = RadioTechnology.LTE,
                            registered = false,
                            cellId = "61706",
                            pci = "10",
                            tacOrLac = "1001",
                            channelNumber = "40490",
                            band = "B41",
                            rsrp = -108,
                            rsrq = -15,
                            sinr = 4,
                            dbm = -96,
                        ),
                    ),
                    neighborMatches = listOf(
                        SectorRecord(
                            id = "sector-2",
                            layerId = "layer-1",
                            radioTechnology = RadioTechnology.LTE,
                            siteId = "SITE-002",
                            siteName = "示例地区湖站",
                            cellId = "61706",
                            cellName = "示例地区湖-L1",
                            longitude = 120.941,
                            latitude = 34.342,
                        ),
                    ),
                    neighborSectors = emptyList(),
                ),
            ),
        )

        assertEquals(-97, sample.primaryRsrp)
        assertEquals(16, sample.primarySinr)
        assertEquals("SIM1", sample.primaryMetricSource)
        assertEquals(120.9398, sample.point?.longitude)
        assertEquals("示例地区湖-1", sample.sims.single().servingCell?.matchedSectorName)
        assertEquals("示例地区湖站", sample.sims.single().servingCell?.matchedSiteName)
        assertEquals("示例地区湖-L1", sample.sims.single().neighbors.single().matchedSectorName)
    }
}
