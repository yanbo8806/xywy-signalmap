package com.xywy.signalmap.ui.map

import com.xywy.signalmap.SimPreferenceUiState
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapSimSignalBadgeFactoryTest {
    @Test
    fun `stale readings are not presented as current valid measurements`() {
        val badge = MapSimSignalBadgeFactory.create(
            simPreferences = listOf(SimPreferenceUiState(slotIndex = 0, label = "SIM1")),
            simMatches = listOf(
                SimCellMatch(
                    subscriptionId = 1, slotIndex = 0, label = "SIM1",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.LTE, registered = true,
                        rsrp = -75, sinr = 20, isStale = true,
                    ),
                ),
            ),
        ).single()

        assertEquals("信号过期", badge.technologyLabel)
        assertEquals("--", badge.rsrpText)
        assertEquals("--", badge.sinrText)
        assertFalse(badge.active)
    }

    @Test
    fun `factory builds ordered sim badges with rsrp and sinr`() {
        val badges = MapSimSignalBadgeFactory.create(
            simPreferences = listOf(
                SimPreferenceUiState(slotIndex = 1, label = "SIM2 中国联通"),
                SimPreferenceUiState(slotIndex = 0, label = "SIM1 示例运营商"),
            ),
            simMatches = listOf(
                SimCellMatch(
                    subscriptionId = 2,
                    slotIndex = 1,
                    label = "SIM2 中国联通",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = true,
                        rsrp = -108,
                        sinr = 5,
                    ),
                ),
                SimCellMatch(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1 示例运营商",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.LTE,
                        registered = true,
                        rsrp = -95,
                        sinr = 12,
                    ),
                ),
            ),
        )

        assertEquals(listOf(0, 1), badges.map { it.slotIndex })
        assertEquals("SIM1", badges[0].title)
        assertEquals("LTE", badges[0].technologyLabel)
        assertEquals("-95 dBm", badges[0].rsrpText)
        assertEquals("12 dB", badges[0].sinrText)
        assertTrue(badges[0].active)

        assertEquals("SIM2", badges[1].title)
        assertEquals("NR", badges[1].technologyLabel)
        assertEquals("-108 dBm", badges[1].rsrpText)
        assertEquals("5 dB", badges[1].sinrText)
    }

    @Test
    fun `factory shows empty state when sim has no serving cell`() {
        val badges = MapSimSignalBadgeFactory.create(
            simPreferences = listOf(
                SimPreferenceUiState(slotIndex = 0, label = "SIM1"),
                SimPreferenceUiState(slotIndex = 1, label = "SIM2"),
            ),
            simMatches = listOf(
                SimCellMatch(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.LTE,
                        registered = true,
                        rsrp = -99,
                        sinr = 7,
                    ),
                ),
            ),
        )

        assertEquals("无服务", badges[1].technologyLabel)
        assertEquals("--", badges[1].rsrpText)
        assertEquals("--", badges[1].sinrText)
        assertFalse(badges[1].active)
    }
}
