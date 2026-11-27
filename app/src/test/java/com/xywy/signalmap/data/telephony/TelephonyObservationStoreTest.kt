package com.xywy.signalmap.data.telephony

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class TelephonyObservationStoreTest {
    @Test
    fun `snapshot keeps serving cells separated by sim`() {
        val store = TelephonyObservationStore()
        store.refreshSubscriptions(
            listOf(
                subscription(subscriptionId = 101, slotIndex = 0, label = "SIM1"),
                subscription(subscriptionId = 202, slotIndex = 1, label = "SIM2"),
            ),
        )

        store.updateCells(
            subscriptionId = 101,
            slotIndex = 0,
            label = "SIM1",
            cells = listOf(
                servingCell(cellId = "1001", pci = "11", rsrp = -87),
                neighborCell(cellId = "1002", pci = "12", rsrp = -95),
            ),
        )
        store.updateCells(
            subscriptionId = 202,
            slotIndex = 1,
            label = "SIM2",
            cells = listOf(
                servingCell(cellId = "2001", pci = "21", rsrp = -101),
                neighborCell(cellId = "2002", pci = "22", rsrp = -109),
            ),
        )

        val snapshot = store.snapshot()

        assertEquals(2, snapshot.simCards.size)
        assertEquals("1001", snapshot.simCards.first { it.slotIndex == 0 }.servingCell?.cellId)
        assertEquals("2001", snapshot.simCards.first { it.slotIndex == 1 }.servingCell?.cellId)
        assertEquals("1002", snapshot.simCards.first { it.slotIndex == 0 }.neighbors.single().cellId)
        assertEquals("2002", snapshot.simCards.first { it.slotIndex == 1 }.neighbors.single().cellId)
    }

    @Test
    fun `updating sim2 does not overwrite cached sim1 cells`() {
        val store = TelephonyObservationStore()
        store.refreshSubscriptions(
            listOf(
                subscription(subscriptionId = 101, slotIndex = 0, label = "SIM1"),
                subscription(subscriptionId = 202, slotIndex = 1, label = "SIM2"),
            ),
        )
        store.updateCells(
            subscriptionId = 101,
            slotIndex = 0,
            label = "SIM1",
            cells = listOf(servingCell(cellId = "1001", pci = "11", rsrp = -90)),
        )
        store.updateCells(
            subscriptionId = 202,
            slotIndex = 1,
            label = "SIM2",
            cells = listOf(servingCell(cellId = "2001", pci = "21", rsrp = -105)),
        )

        store.updateCells(
            subscriptionId = 202,
            slotIndex = 1,
            label = "SIM2",
            cells = listOf(servingCell(cellId = "2009", pci = "29", rsrp = -97)),
        )

        val snapshot = store.snapshot()

        assertEquals("1001", snapshot.simCards.first { it.slotIndex == 0 }.servingCell?.cellId)
        assertEquals("2009", snapshot.simCards.first { it.slotIndex == 1 }.servingCell?.cellId)
        assertNull(snapshot.errorMessage)
    }

    @Test
    fun `snapshot keeps registered nr secondary cells as neighbors and prioritizes same channel`() {
        val store = TelephonyObservationStore()
        store.refreshSubscriptions(
            listOf(
                subscription(subscriptionId = 101, slotIndex = 0, label = "SIM1"),
            ),
        )

        store.updateCells(
            subscriptionId = 101,
            slotIndex = 0,
            label = "SIM1",
            cells = listOf(
                CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "7001",
                    pci = "101",
                    channelNumber = "504990",
                    rsrp = -88,
                    dbm = -88,
                ),
                CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = true,
                    cellId = "7002",
                    pci = "102",
                    channelNumber = "504990",
                    rsrp = -95,
                    dbm = -95,
                ),
                CellSignal(
                    radioTechnology = RadioTechnology.LTE,
                    registered = false,
                    cellId = "8001",
                    pci = "33",
                    channelNumber = "40490",
                    rsrp = -85,
                    dbm = -85,
                ),
                CellSignal(
                    radioTechnology = RadioTechnology.NR,
                    registered = false,
                    cellId = "7003",
                    pci = "103",
                    channelNumber = "504990",
                    rsrp = -101,
                    dbm = -101,
                ),
            ),
        )

        val neighbors = store.snapshot().simCards.single().neighbors

        assertEquals(listOf("7002", "7003"), neighbors.take(2).map { it.cellId })
        assertTrue(neighbors.any { it.cellId == "8001" })
    }

    @Test
    fun `lte signal supplement updates only serving lte cell`() {
        val store = TelephonyObservationStore()
        store.refreshSubscriptions(
            listOf(subscription(subscriptionId = 101, slotIndex = 0, label = "SIM1")),
        )
        store.updateCells(
            subscriptionId = 101,
            slotIndex = 0,
            label = "SIM1",
            cells = listOf(
                servingCell(cellId = "1001", pci = "11", rsrp = -101),
                neighborCell(cellId = "1002", pci = "12", rsrp = -95),
            ),
        )

        val updated = store.updateServingSignal(
            subscriptionId = 101,
            slotIndex = 0,
            update = ServingSignalUpdate(RadioTechnology.LTE, rsrp = -90, rsrq = -8, sinr = 14, dbm = -90),
            measuredAtElapsedMillis = 1000,
            observedAtMillis = 1000,
        )

        val snapshot = store.snapshot().simCards.single()
        assertTrue(updated)
        assertEquals("1001", snapshot.servingCell?.cellId)
        assertEquals(-90, snapshot.servingCell?.rsrp)
        assertEquals(-8, snapshot.servingCell?.rsrq)
        assertEquals(14, snapshot.servingCell?.sinr)
        assertEquals(null, snapshot.neighbors.single { it.cellId == "1002" }.sinr)
    }

    private fun subscription(
        subscriptionId: Int,
        slotIndex: Int,
        label: String,
    ): TelephonyObservedSubscription = TelephonyObservedSubscription(
        subscriptionId = subscriptionId,
        slotIndex = slotIndex,
        label = label,
    )

    private fun servingCell(
        cellId: String,
        pci: String,
        rsrp: Int,
    ): CellSignal = CellSignal(
        radioTechnology = RadioTechnology.LTE,
        registered = true,
        cellId = cellId,
        pci = pci,
        rsrp = rsrp,
        dbm = rsrp,
    )

    private fun neighborCell(
        cellId: String,
        pci: String,
        rsrp: Int,
    ): CellSignal = CellSignal(
        radioTechnology = RadioTechnology.LTE,
        registered = false,
        cellId = cellId,
        pci = pci,
        rsrp = rsrp,
        dbm = rsrp,
    )
}
