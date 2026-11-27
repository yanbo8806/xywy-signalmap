package com.xywy.signalmap.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTabHostPlanTest {
    @Test
    fun `app tabs keep expected chinese labels`() {
        assertEquals("基站地图", AppTab.Map.label)
        assertEquals("占用小区", AppTab.Cells.label)
        assertEquals("我的", AppTab.Profile.label)
    }

    @Test
    fun `map tab keeps map mounted without overlay`() {
        val plan = AppTabHostPlan.from(currentTab = AppTab.Map)

        assertEquals(true, plan.keepMapMounted)
        assertEquals(null, plan.overlayTab)
        assertEquals(true, plan.collectMapSensorHud)
    }

    @Test
    fun `cells tab keeps map mounted and overlays cells`() {
        val plan = AppTabHostPlan.from(currentTab = AppTab.Cells)

        assertEquals(true, plan.keepMapMounted)
        assertEquals(AppTab.Cells, plan.overlayTab)
        assertEquals(false, plan.collectMapSensorHud)
    }

    @Test
    fun `profile tab keeps map mounted and overlays profile`() {
        val plan = AppTabHostPlan.from(currentTab = AppTab.Profile)

        assertEquals(true, plan.keepMapMounted)
        assertEquals(AppTab.Profile, plan.overlayTab)
        assertEquals(false, plan.collectMapSensorHud)
    }
}
