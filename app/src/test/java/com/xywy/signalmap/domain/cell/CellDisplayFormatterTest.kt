package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import kotlin.test.Test
import kotlin.test.assertEquals

class CellDisplayFormatterTest {
    @Test
    fun `nr details split nci into gnodeb and cell id`() {
        val signal = CellSignal(
            radioTechnology = RadioTechnology.NR,
            registered = true,
            cellId = "49152",
            pci = "704",
            tacOrLac = "1234",
            channelNumber = "504990",
            band = "N41",
        )

        val details = CellDisplayFormatter.from(signal)

        assertEquals("NCI", details.cellIdentityLabel)
        assertEquals("49152", details.cellIdentityValue)
        assertEquals("12", details.baseStationId)
        assertEquals("0", details.localCellId)
        assertEquals("网络", details.modeOrNetworkLabel)
        assertEquals("NR", details.modeOrNetworkValue)
        assertEquals("2", details.pciMod3)
    }

    @Test
    fun `nr details derive frequency and gscn from nrarfcn`() {
        val signal = CellSignal(
            radioTechnology = RadioTechnology.NR,
            registered = true,
            cellId = "49152",
            channelNumber = "504990",
            band = "N41",
        )

        val details = CellDisplayFormatter.from(signal)

        assertEquals("NRARFCN", details.channelLabel)
        assertEquals("504990", details.channelValue)
        assertEquals("2.5GHz", details.frequencyLabel)
        assertEquals("6312", details.gscn)
    }

    @Test
    fun `nr gscn follows dex sub 3ghz rule exactly`() {
        val signal = CellSignal(
            radioTechnology = RadioTechnology.NR,
            registered = true,
            cellId = "1000",
            channelNumber = "500410",
        )

        val details = CellDisplayFormatter.from(signal)

        assertEquals("2.5GHz", details.frequencyLabel)
        assertEquals("6252", details.gscn)
    }

    @Test
    fun `lte details split eci into enodeb and sector id`() {
        val signal = CellSignal(
            radioTechnology = RadioTechnology.LTE,
            registered = true,
            cellId = "61706",
            channelNumber = "40490",
            band = "B41",
            pci = "10",
        )

        val details = CellDisplayFormatter.from(signal)

        assertEquals("ECI", details.cellIdentityLabel)
        assertEquals("61706", details.cellIdentityValue)
        assertEquals("241", details.baseStationId)
        assertEquals("10", details.localCellId)
        assertEquals("模式", details.modeOrNetworkLabel)
        assertEquals("TDD", details.modeOrNetworkValue)
        assertEquals("2580", details.frequencyLabel)
        assertEquals("1", details.pciMod3)
    }

    @Test
    fun `bandwidth label falls back to engineering params`() {
        assertEquals(
            "20 MHz",
            CellDisplayFormatter.resolveBandwidthLabel(
                liveBandwidth = null,
                sectorBandwidth = "20M",
            ),
        )
        assertEquals(
            "15 MHz",
            CellDisplayFormatter.resolveBandwidthLabel(
                liveBandwidth = "15 MHz",
                sectorBandwidth = "20M",
            ),
        )
    }
}
