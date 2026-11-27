package com.xywy.signalmap.data.telephony

import com.xywy.signalmap.domain.model.*
import kotlin.test.*

class SignalFreshnessTest {
    @Test fun `network state survives cell refresh and radio clearing`() {
        val store = store()
        store.updateNetworkState(1, 0, service = "已注册", data = "已连接")
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        assertEquals("已注册", store.snapshot().simCards.first().serviceState)
        store.updateServingSignal(1, 0, update(), 3000, 3000)
        store.updateCells(1, 0, "SIM1", listOf(cell(at = 2000).copy(measurementSource = "系统缓存读取")))
        assertEquals("SignalStrength 回调", store.snapshot().simCards.first().servingCell?.measurementSource)
        store.clearCells(1, 0, "无线电关闭")
        store.updateNetworkState(1, 0, service = "无线电关闭")
        assertEquals("无线电关闭", store.snapshot().simCards.first().serviceState)
        assertNull(store.snapshot().simCards.first().servingCell)
        store.refreshSubscriptions(emptyList())
        store.updateNetworkState(1, 0, service = "已注册")
        assertTrue(store.snapshot().simCards.isEmpty())
    }
    private fun store() = TelephonyObservationStore().apply {
        refreshSubscriptions(listOf(TelephonyObservedSubscription(1, 0, "SIM1"), TelephonyObservedSubscription(2, 1, "SIM2")))
    }
    private fun cell(id: String = "100", at: Long = 1000, technology: RadioTechnology = RadioTechnology.LTE) = CellSignal(
        technology, registered = true, cellId = id, channelNumber = "100", rsrp = -100, sinr = 5, dbm = -100,
        observedAtElapsedMillis = at, observedAtMillis = at,
    )
    private fun update(tech: RadioTechnology = RadioTechnology.LTE) = ServingSignalUpdate(tech, -88, -8, 20, -88)

    @Test fun `repeated cached readings do not become fresh when polled`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        val sim = store.snapshot(elapsedRealtimeMillis = 20000).simCards.first()
        assertTrue(sim.servingCell!!.isStale)
        assertNull(sim.servingCell.rsrp)
        assertNull(sim.servingCell.sinr)
        assertNotNull(sim.errorMessage)
    }

    @Test fun `unchanged values with new modem timestamps stay fresh`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        store.updateCells(1, 0, "SIM1", listOf(cell(at = 19000)))
        val signal = store.snapshot(elapsedRealtimeMillis = 20000).simCards.first().servingCell!!
        assertFalse(signal.isStale)
        assertEquals(-100, signal.rsrp)
    }

    @Test fun `late cached cell response cannot overwrite fresher signal callback`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        assertTrue(store.updateServingSignal(1, 0, update(), 5000, 5000))
        store.updateCells(1, 0, "SIM1", listOf(cell(at = 2000)))
        val signal = store.snapshot(elapsedRealtimeMillis = 6000).simCards.first().servingCell!!
        assertEquals(-88, signal.rsrp)
        assertEquals(20, signal.sinr)
        assertEquals(5000, signal.observedAtElapsedMillis)
    }

    @Test fun `handover does not transplant metrics onto a different cell`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        store.updateServingSignal(1, 0, update(), 5000, 5000)
        store.updateCells(1, 0, "SIM1", listOf(cell(id = "200", at = 6000)))
        assertFalse(store.updateServingSignal(1, 0, update(), 5500, 5500))
        val signal = store.snapshot().simCards.first().servingCell!!
        assertEquals("200", signal.cellId)
        assertEquals(-100, signal.rsrp)
    }

    @Test fun `nr callbacks refresh nr serving metrics without touching the other sim`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell(technology = RadioTechnology.NR)))
        store.updateCells(2, 1, "SIM2", listOf(cell(id = "200")))
        store.updateServingSignal(1, 0, update(RadioTechnology.NR), 19000, 19000)
        val sims = store.snapshot(elapsedRealtimeMillis = 20000).simCards
        assertEquals(20, sims[0].servingCell?.sinr)
        assertFalse(sims[0].servingCell!!.isStale)
        assertTrue(sims[1].servingCell!!.isStale)
    }

    @Test fun `radio reset clears metrics and removed subscriptions cannot be revived by callbacks`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        store.clearCells(1, 0, "关闭")
        assertNull(store.snapshot().simCards.first().servingCell)
        assertFalse(store.updateServingSignal(1, 0, update(), 2000, 2000))
        store.refreshSubscriptions(emptyList())
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        assertTrue(store.snapshot().simCards.isEmpty())
    }

    @Test fun `neighbor-only scan never masquerades as a serving measurement`() {
        val neighbor = cell().copy(registered = false, connectionStatus = CellConnectionStatus.NONE)
        assertNull(TelephonyCellSelection.selectServing(listOf(neighbor)))
    }

    @Test fun `missing sinr in a new measurement clears previous sinr`() {
        val store = store()
        store.updateCells(1, 0, "SIM1", listOf(cell()))
        store.updateServingSignal(1, 0, update().copy(sinr = null), 2000, 2000)
        assertNull(store.snapshot().simCards.first().servingCell?.sinr)
    }
}
