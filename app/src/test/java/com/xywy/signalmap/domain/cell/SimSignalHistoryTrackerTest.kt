package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class SimSignalHistoryTrackerTest {
    @Test
    fun `tracker keeps at most five newest records per sim`() {
        var histories = emptyList<SimSignalHistory>()

        repeat(6) { index ->
            histories = SimSignalHistoryTracker.update(
                histories = histories,
                simSnapshots = listOf(
                    SimCellSnapshot(
                        subscriptionId = 1,
                        slotIndex = 0,
                        label = "SIM1",
                        servingCell = CellSignal(
                            radioTechnology = RadioTechnology.NR,
                            registered = true,
                            cellId = (1000 + index).toString(),
                            observedAtMillis = index.toLong(),
                        ),
                    ),
                ),
            )
        }

        val records = histories.single().records
        assertEquals(5, records.size)
        assertEquals("1005", records.first().servingCell.cellId)
        assertEquals("1001", records.last().servingCell.cellId)
    }

    @Test
    fun `tracker keeps histories separated by sim slot`() {
        val histories = SimSignalHistoryTracker.update(
            histories = emptyList(),
            simSnapshots = listOf(
                SimCellSnapshot(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.LTE,
                        registered = true,
                        cellId = "200",
                    ),
                ),
                SimCellSnapshot(
                    subscriptionId = 2,
                    slotIndex = 1,
                    label = "SIM2",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = true,
                        cellId = "300",
                    ),
                ),
            ),
        )

        assertEquals(2, histories.size)
        assertEquals("200", histories.first { it.slotIndex == 0 }.records.single().servingCell.cellId)
        assertEquals("300", histories.first { it.slotIndex == 1 }.records.single().servingCell.cellId)
    }
}
