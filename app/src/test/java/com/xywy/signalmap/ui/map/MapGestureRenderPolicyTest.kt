package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.map.MapOverlayRenderPolicyState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapGestureRenderPolicyTest {
    @Test
    fun `gesture interaction keeps all enabled overlays visible`() {
        val visibility = MapGestureRenderPolicy.resolve(
            isGestureInteracting = true,
            overlayPolicy = fullPolicy(),
        )

        assertTrue(visibility.sector)
        assertTrue(visibility.sitePoints)
        assertTrue(visibility.pointIcons)
        assertTrue(visibility.siteLabels)
        assertTrue(visibility.servingLine)
        assertTrue(visibility.neighborLine)
        assertTrue(visibility.testLog)
        assertTrue(visibility.measurement)
        assertTrue(visibility.userDraft)
        assertEquals(MapGestureRenderPolicy.resolve(false, fullPolicy()), visibility)
    }

    @Test
    fun `idle state follows overlay policy for engineering overlays`() {
        val visibility = MapGestureRenderPolicy.resolve(
            isGestureInteracting = false,
            overlayPolicy = fullPolicy().copy(
                showSiteLabels = false,
                showNeighborLines = false,
            ),
        )

        assertTrue(visibility.sector)
        assertTrue(visibility.sitePoints)
        assertFalse(visibility.siteLabels)
        assertTrue(visibility.servingLine)
        assertFalse(visibility.neighborLine)
        assertTrue(visibility.testLog)
        assertTrue(visibility.measurement)
        assertTrue(visibility.userDraft)
    }

    @Test
    fun `point-only icons stay visible during a gesture without enabling engineering sites`() {
        val policy = fullPolicy().copy(showSitePoints = false, showPointIcons = true)
        val moving = MapGestureRenderPolicy.resolve(true, policy)
        val idle = MapGestureRenderPolicy.resolve(false, policy)
        assertTrue(moving.pointIcons)
        assertTrue(idle.pointIcons)
        assertFalse(idle.sitePoints)
        assertTrue(idle.siteLabels)
        assertEquals(idle, moving)
    }

    private fun fullPolicy() = MapOverlayRenderPolicyState(
        sectorLimit = 700,
        pointLimit = 1_200,
        labelLimit = 160,
        showSectorFill = true,
        showSitePoints = true,
        showSiteLabels = true,
        showServingLine = true,
        showNeighborLines = true,
    )
}
