package com.xywy.signalmap.ui.map

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class MapPanelExpansionState(
    val searchExpanded: Boolean = false,
    val filterExpanded: Boolean = false,
    val testRecordExpanded: Boolean = false,
    val toolExpanded: Boolean = false,
    val createExpanded: Boolean = false,
) {
    fun withSearchExpanded(expanded: Boolean): MapPanelExpansionState =
        copy(searchExpanded = expanded)

    fun withFilterExpanded(expanded: Boolean): MapPanelExpansionState =
        copy(filterExpanded = expanded)

    fun withTestRecordExpanded(expanded: Boolean): MapPanelExpansionState =
        copy(testRecordExpanded = expanded)

    fun withToolExpanded(expanded: Boolean): MapPanelExpansionState =
        copy(toolExpanded = expanded)

    fun withCreateExpanded(expanded: Boolean): MapPanelExpansionState =
        copy(createExpanded = expanded)

    companion object {
        val Saver: Saver<MapPanelExpansionState, Any> = listSaver(
            save = { listOf(it.searchExpanded, it.filterExpanded, it.testRecordExpanded, it.toolExpanded, it.createExpanded) },
            restore = { values ->
                MapPanelExpansionState(
                    searchExpanded = values.getOrNull(0) as? Boolean ?: false,
                    filterExpanded = values.getOrNull(1) as? Boolean ?: false,
                    testRecordExpanded = values.getOrNull(2) as? Boolean ?: false,
                    toolExpanded = values.getOrNull(3) as? Boolean ?: false,
                    createExpanded = values.getOrNull(4) as? Boolean ?: false,
                )
            },
        )
    }
}
