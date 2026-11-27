package com.xywy.signalmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MapPanelExpansionStateTest {
    @Test
    fun `defaults match current map page expectations`() {
        val state = MapPanelExpansionState()

        assertFalse(state.searchExpanded)
        assertFalse(state.filterExpanded)
        assertFalse(state.testRecordExpanded)
        assertFalse(state.toolExpanded)
        assertFalse(state.createExpanded)
    }

    @Test
    fun `updates keep other sections unchanged`() {
        val state = MapPanelExpansionState()
            .withSearchExpanded(false)
            .withFilterExpanded(true)
            .withTestRecordExpanded(true)
            .withToolExpanded(true)
            .withCreateExpanded(true)

        assertEquals(
            MapPanelExpansionState(
                searchExpanded = false,
                filterExpanded = true,
                testRecordExpanded = true,
                toolExpanded = true,
                createExpanded = true,
            ),
            state,
        )
    }
}
