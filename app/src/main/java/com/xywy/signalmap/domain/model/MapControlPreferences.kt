package com.xywy.signalmap.domain.model

data class SimOverlayPreferenceState(
    val showServingLine: Boolean = true,
    val showNeighborLines: Boolean = true,
    val showTestPoints: Boolean = true,
)

data class MapControlPreferences(
    val selectedTechs: Set<RadioTechnology> = setOf(RadioTechnology.NR, RadioTechnology.LTE),
    val selectedBands: Set<String> = emptySet(),
    val showSectorFill: Boolean = true,
    val showSitePoints: Boolean = true,
    val showSiteLabels: Boolean = true,
    val nearbyRecordLimit: Int = 200,
    val mapDisplayMode: MapDisplayMode = MapDisplayMode.Overlay,
    val testMetricMode: TestMetricMode = TestMetricMode.RSRP,
    val simOverlayPreferences: Map<Int, SimOverlayPreferenceState> = defaultSimOverlayPreferenceStates(),
)

fun defaultMapControlPreferences(): MapControlPreferences = MapControlPreferences()

fun defaultSimOverlayPreferenceStates(): Map<Int, SimOverlayPreferenceState> =
    listOf(0, 1).associateWith { SimOverlayPreferenceState() }
