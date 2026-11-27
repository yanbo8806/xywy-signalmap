package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.CellSnapshot
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellSnapshot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CellSnapshotContentComparatorTest {
    @Test
    fun `equivalent snapshots ignore collection and observed timestamps`() {
        val previous = snapshot(
            collectedAtMillis = 1_000L,
            servingObservedAtMillis = 900L,
            rsrp = -95,
        )
        val latest = snapshot(
            collectedAtMillis = 6_000L,
            servingObservedAtMillis = 5_900L,
            rsrp = -95,
        )

        assertTrue(CellSnapshotContentComparator.areEquivalent(previous, latest))
    }

    @Test
    fun `different signal values are not equivalent`() {
        val previous = snapshot(
            collectedAtMillis = 1_000L,
            servingObservedAtMillis = 900L,
            rsrp = -95,
        )
        val latest = snapshot(
            collectedAtMillis = 6_000L,
            servingObservedAtMillis = 5_900L,
            rsrp = -103,
        )

        assertFalse(CellSnapshotContentComparator.areEquivalent(previous, latest))
    }

    private fun snapshot(
        collectedAtMillis: Long,
        servingObservedAtMillis: Long,
        rsrp: Int,
    ): CellSnapshot = CellSnapshot(
        simCards = listOf(
            SimCellSnapshot(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(
                    radioTechnology = RadioTechnology.LTE,
                    registered = true,
                    subscriptionId = 1,
                    slotIndex = 0,
                    cellId = "1001",
                    pci = "11",
                    tacOrLac = "22",
                    channelNumber = "39148",
                    band = "B3",
                    rsrp = rsrp,
                    rsrq = -12,
                    sinr = 8,
                    dbm = -72,
                    observedAtMillis = servingObservedAtMillis,
                ),
            ),
        ),
        collectedAtMillis = collectedAtMillis,
    )
}
