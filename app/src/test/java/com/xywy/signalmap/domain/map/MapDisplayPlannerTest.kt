package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.cell.MapDisplayPlanner
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellMatch
import kotlin.test.Test
import kotlin.test.assertEquals

class MapDisplayPlannerTest {
    @Test
    fun `split mode yields one panel per sim`() {
        val matches = listOf(
            SimCellMatch(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "101"),
            ),
            SimCellMatch(
                subscriptionId = 2,
                slotIndex = 1,
                label = "SIM2",
                servingCell = CellSignal(radioTechnology = RadioTechnology.NR, registered = true, cellId = "202"),
            ),
        )

        val splitPanels = MapDisplayPlanner.build(MapDisplayMode.Split, matches)
        val overlayPanels = MapDisplayPlanner.build(MapDisplayMode.Overlay, matches)

        assertEquals(2, splitPanels.size)
        assertEquals(1, overlayPanels.size)
        assertEquals(2, overlayPanels.single().simMatches.size)
    }
}
