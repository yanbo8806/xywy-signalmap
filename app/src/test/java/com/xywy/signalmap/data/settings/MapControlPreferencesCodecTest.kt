package com.xywy.signalmap.data.settings

import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimOverlayPreferenceState
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.model.defaultMapControlPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

class MapControlPreferencesCodecTest {
    @Test
    fun `invalid nearest counts fall back to 200`() {
        listOf(null, 0, -1, "350", 350.5, Long.MAX_VALUE).forEach { invalid ->
            val decoded = MapControlPreferencesCodec.decode(mapOf(MapControlPreferencesCodec.NEARBY_RECORD_LIMIT to invalid), listOf(0, 1))
            assertEquals(200, decoded.nearbyRecordLimit)
        }
    }

    @Test
    fun `decode returns safe defaults when saved values are missing`() {
        val decoded = MapControlPreferencesCodec.decode(
            raw = emptyMap(),
            slotIndexes = listOf(0, 1),
        )

        assertEquals(setOf(RadioTechnology.NR, RadioTechnology.LTE), decoded.selectedTechs)
        assertEquals(emptySet(), decoded.selectedBands)
        assertEquals(true, decoded.showSectorFill)
        assertEquals(true, decoded.showSitePoints)
        assertEquals(true, decoded.showSiteLabels)
        assertEquals(200, decoded.nearbyRecordLimit)
        assertEquals(MapDisplayMode.Overlay, decoded.mapDisplayMode)
        assertEquals(TestMetricMode.RSRP, decoded.testMetricMode)
        assertEquals(true, decoded.simOverlayPreferences.getValue(0).showServingLine)
        assertEquals(true, decoded.simOverlayPreferences.getValue(1).showNeighborLines)
        assertEquals(true, decoded.simOverlayPreferences.getValue(1).showTestPoints)
    }

    @Test
    fun `encode and decode preserve map controls and per sim line switches`() {
        val preferences = defaultMapControlPreferences().copy(
            selectedTechs = setOf(RadioTechnology.NR),
            selectedBands = setOf("N41", "B3"),
            showSectorFill = false,
            showSitePoints = true,
            showSiteLabels = false,
            nearbyRecordLimit = 350,
            mapDisplayMode = MapDisplayMode.Split,
            testMetricMode = TestMetricMode.SINR,
            simOverlayPreferences = mapOf(
                0 to SimOverlayPreferenceState(
                    showServingLine = false,
                    showNeighborLines = true,
                    showTestPoints = false,
                ),
                1 to SimOverlayPreferenceState(
                    showServingLine = true,
                    showNeighborLines = false,
                    showTestPoints = true,
                ),
            ),
        )

        val decoded = MapControlPreferencesCodec.decode(
            raw = MapControlPreferencesCodec.encode(preferences),
            slotIndexes = listOf(0, 1),
        )

        assertEquals(preferences, decoded)
    }

    @Test
    fun `decode ignores invalid enum names instead of breaking map startup`() {
        val decoded = MapControlPreferencesCodec.decode(
            raw = mapOf(
                "selected_techs" to setOf("NR", "BAD_TECH"),
                "map_display_mode" to "BAD_MODE",
                "test_metric_mode" to "BAD_METRIC",
            ),
            slotIndexes = listOf(0, 1),
        )

        assertEquals(setOf(RadioTechnology.NR), decoded.selectedTechs)
        assertEquals(MapDisplayMode.Overlay, decoded.mapDisplayMode)
        assertEquals(TestMetricMode.RSRP, decoded.testMetricMode)
    }
}
