package com.xywy.signalmap.ui.map

import com.xywy.signalmap.SimPreferenceUiState
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellMatch

data class MapSimSignalBadgeState(
    val slotIndex: Int,
    val title: String,
    val technologyLabel: String,
    val rsrpText: String,
    val sinrText: String,
    val active: Boolean,
)

object MapSimSignalBadgeFactory {
    fun create(
        simPreferences: List<SimPreferenceUiState>,
        simMatches: List<SimCellMatch>,
        maxSlots: Int = 2,
    ): List<MapSimSignalBadgeState> {
        val matchesBySlot = simMatches.associateBy { it.slotIndex }
        return simPreferences
            .sortedBy { it.slotIndex }
            .take(maxSlots)
            .map { preference ->
                val match = matchesBySlot[preference.slotIndex]
                val servingCell = match?.servingCell
                MapSimSignalBadgeState(
                    slotIndex = preference.slotIndex,
                    title = "SIM${preference.slotIndex + 1}",
                    technologyLabel = if (servingCell?.isStale == true) "信号过期" else servingCell?.radioTechnology?.label ?: "无服务",
                    rsrpText = formatMetric(servingCell?.takeUnless { it.isStale }?.rsrp, "dBm"),
                    sinrText = formatMetric(servingCell?.takeUnless { it.isStale }?.sinr, "dB"),
                    active = servingCell != null && !servingCell.isStale && servingCell.radioTechnology != RadioTechnology.UNKNOWN,
                )
            }
    }

    private fun formatMetric(
        value: Int?,
        unit: String,
    ): String = value?.let { "$it $unit" } ?: "--"
}
