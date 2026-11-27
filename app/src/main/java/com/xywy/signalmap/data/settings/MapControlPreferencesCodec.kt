package com.xywy.signalmap.data.settings

import com.xywy.signalmap.domain.model.MapControlPreferences
import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimOverlayPreferenceState
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.model.defaultMapControlPreferences

internal object MapControlPreferencesCodec {
    const val SELECTED_TECHS = "selected_techs"
    const val SELECTED_BANDS = "selected_bands"
    const val SHOW_SECTOR_FILL = "show_sector_fill"
    const val SHOW_SITE_POINTS = "show_site_points"
    const val SHOW_SITE_LABELS = "show_site_labels"
    const val NEARBY_RECORD_LIMIT = "nearby_record_limit"
    const val MAP_DISPLAY_MODE = "map_display_mode"
    const val TEST_METRIC_MODE = "test_metric_mode"

    fun encode(preferences: MapControlPreferences): Map<String, Any> = buildMap {
        put(SELECTED_TECHS, preferences.selectedTechs.mapTo(linkedSetOf()) { it.name })
        put(SELECTED_BANDS, preferences.selectedBands.toSortedSet())
        put(SHOW_SECTOR_FILL, preferences.showSectorFill)
        put(SHOW_SITE_POINTS, preferences.showSitePoints)
        put(SHOW_SITE_LABELS, preferences.showSiteLabels)
        put(NEARBY_RECORD_LIMIT, preferences.nearbyRecordLimit.takeIf { it > 0 } ?: 200)
        put(MAP_DISPLAY_MODE, preferences.mapDisplayMode.name)
        put(TEST_METRIC_MODE, preferences.testMetricMode.name)
        preferences.simOverlayPreferences.forEach { (slotIndex, preference) ->
            put(simServingLineKey(slotIndex), preference.showServingLine)
            put(simNeighborLineKey(slotIndex), preference.showNeighborLines)
            put(simTestPointsKey(slotIndex), preference.showTestPoints)
        }
    }

    fun decode(
        raw: Map<String, Any?>,
        slotIndexes: List<Int>,
    ): MapControlPreferences {
        val defaults = defaultMapControlPreferences()
        return MapControlPreferences(
            selectedTechs = decodeRadioTechnologies(
                rawSet = raw[SELECTED_TECHS].asStringSetOrNull(),
                defaultValue = defaults.selectedTechs,
            ),
            selectedBands = raw[SELECTED_BANDS].asStringSetOrNull()?.toSortedSet() ?: defaults.selectedBands,
            showSectorFill = raw[SHOW_SECTOR_FILL].asBooleanOrNull() ?: defaults.showSectorFill,
            showSitePoints = raw[SHOW_SITE_POINTS].asBooleanOrNull() ?: defaults.showSitePoints,
            showSiteLabels = raw[SHOW_SITE_LABELS].asBooleanOrNull() ?: defaults.showSiteLabels,
            nearbyRecordLimit = (raw[NEARBY_RECORD_LIMIT] as? Int)?.takeIf { it > 0 } ?: defaults.nearbyRecordLimit,
            mapDisplayMode = decodeEnum<MapDisplayMode>(
                value = raw[MAP_DISPLAY_MODE].asStringOrNull(),
                defaultValue = defaults.mapDisplayMode,
            ),
            testMetricMode = decodeEnum<TestMetricMode>(
                value = raw[TEST_METRIC_MODE].asStringOrNull(),
                defaultValue = defaults.testMetricMode,
            ),
            simOverlayPreferences = slotIndexes.associateWith { slotIndex ->
                val defaultPreference = defaults.simOverlayPreferences[slotIndex] ?: SimOverlayPreferenceState()
                SimOverlayPreferenceState(
                    showServingLine = raw[simServingLineKey(slotIndex)].asBooleanOrNull()
                        ?: defaultPreference.showServingLine,
                    showNeighborLines = raw[simNeighborLineKey(slotIndex)].asBooleanOrNull()
                        ?: defaultPreference.showNeighborLines,
                    showTestPoints = raw[simTestPointsKey(slotIndex)].asBooleanOrNull()
                        ?: defaultPreference.showTestPoints,
                )
            },
        )
    }

    fun simServingLineKey(slotIndex: Int): String = "sim_${slotIndex}_show_serving_line"

    fun simNeighborLineKey(slotIndex: Int): String = "sim_${slotIndex}_show_neighbor_line"

    fun simTestPointsKey(slotIndex: Int): String = "sim_${slotIndex}_show_test_points"

    private fun decodeRadioTechnologies(
        rawSet: Set<String>?,
        defaultValue: Set<RadioTechnology>,
    ): Set<RadioTechnology> {
        rawSet ?: return defaultValue
        return rawSet.mapNotNullTo(linkedSetOf()) { raw ->
            runCatching { RadioTechnology.valueOf(raw) }.getOrNull()
        }
    }

    private inline fun <reified T : Enum<T>> decodeEnum(
        value: String?,
        defaultValue: T,
    ): T = value
        ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
        ?: defaultValue

    private fun Any?.asStringSetOrNull(): Set<String>? = when (this) {
        is Set<*> -> mapNotNullTo(linkedSetOf()) { it as? String }
        is Iterable<*> -> mapNotNullTo(linkedSetOf()) { it as? String }
        else -> null
    }

    private fun Any?.asStringOrNull(): String? = this as? String

    private fun Any?.asBooleanOrNull(): Boolean? = this as? Boolean
}
