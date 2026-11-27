package com.xywy.signalmap.data.telephony

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LteSignalStrengthExtractorTest {
    @Test
    fun `extracts lte sinr from legacy SignalStrength integer fields`() {
        val signalStrength = LegacySignalStrengthFields(
            mLteRsrp = -96,
            mLteRsrq = -11,
            mLteSignalStrength = 23,
            mLteRssnr = 184,
        )

        val supplement = LteSignalStrengthExtractor.fromReflectionFields(signalStrength)

        assertEquals(-96, supplement?.rsrp)
        assertEquals(-11, supplement?.rsrq)
        assertEquals(-96, supplement?.dbm)
        assertEquals(18, supplement?.sinr)
    }

    @Test
    fun `supplement overwrites missing lte sinr on serving cell`() {
        val cell = CellSignal(
            radioTechnology = RadioTechnology.LTE,
            registered = true,
            cellId = "1001",
            pci = "11",
            rsrp = -101,
            sinr = null,
            dbm = -101,
        )

        val updated = LteSignalStrengthExtractor.applySupplement(
            cell = cell,
            supplement = LteSignalSupplement(
                rsrp = -95,
                rsrq = -9,
                sinr = 13,
                dbm = -95,
            ),
        )

        assertEquals(-95, updated.rsrp)
        assertEquals(-9, updated.rsrq)
        assertEquals(13, updated.sinr)
        assertEquals(-95, updated.dbm)
    }

    @Test
    fun `supplement does not change nr cell`() {
        val cell = CellSignal(
            radioTechnology = RadioTechnology.NR,
            registered = true,
            cellId = "9001",
            pci = "101",
            sinr = null,
        )

        val updated = LteSignalStrengthExtractor.applySupplement(
            cell = cell,
            supplement = LteSignalSupplement(sinr = 88),
        )

        assertNull(updated.sinr)
    }

    private class LegacySignalStrengthFields(
        private val mLteRsrp: Int,
        private val mLteRsrq: Int,
        private val mLteSignalStrength: Int,
        private val mLteRssnr: Int,
    )
}
