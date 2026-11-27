package com.xywy.signalmap.domain.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapOverlayRenderPolicyTest {
    @Test
    fun `overview zoom shows every user-enabled overlay without count caps`() {
        val policy = MapOverlayRenderPolicy.resolve(
            zoom = 10.8,
            wantSectorFill = true,
            wantSitePoints = true,
            wantSiteLabels = true,
            wantServingLine = true,
            wantNeighborLines = true,
        )

        assertEquals(Int.MAX_VALUE, policy.sectorLimit)
        assertEquals(Int.MAX_VALUE, policy.pointLimit)
        assertEquals(Int.MAX_VALUE, policy.labelLimit)
        assertTrue(policy.showSectorFill)
        assertTrue(policy.showSitePoints)
        assertEquals(Int.MAX_VALUE, policy.candidateLimit)
        assertTrue(policy.showSiteLabels)
        assertTrue(policy.showServingLine)
        assertTrue(policy.showNeighborLines)
    }

    @Test
    fun `mid zoom also keeps labels and sectors`() {
        val policy = MapOverlayRenderPolicy.resolve(
            zoom = 12.4,
            wantSectorFill = true,
            wantSitePoints = true,
            wantSiteLabels = true,
            wantServingLine = true,
            wantNeighborLines = true,
        )

        assertEquals(Int.MAX_VALUE, policy.sectorLimit)
        assertEquals(Int.MAX_VALUE, policy.pointLimit)
        assertEquals(Int.MAX_VALUE, policy.labelLimit)
        assertTrue(policy.showSectorFill)
        assertTrue(policy.showSitePoints)
        assertTrue(policy.showSiteLabels)
        assertTrue(policy.showServingLine)
        assertTrue(policy.showNeighborLines)
    }

    @Test
    fun `detail zoom allows all fills points and labels`() {
        val policy = MapOverlayRenderPolicy.resolve(
            zoom = 14.2,
            wantSectorFill = true,
            wantSitePoints = true,
            wantSiteLabels = true,
            wantServingLine = true,
            wantNeighborLines = true,
        )

        assertEquals(Int.MAX_VALUE, policy.sectorLimit)
        assertEquals(Int.MAX_VALUE, policy.pointLimit)
        assertEquals(Int.MAX_VALUE, policy.labelLimit)
        assertTrue(policy.showSectorFill)
        assertTrue(policy.showSitePoints)
        assertTrue(policy.showSiteLabels)
        assertTrue(policy.showServingLine)
        assertTrue(policy.showNeighborLines)
    }

    @Test
    fun `close zoom respects full user switches without capped labels`() {
        val policy = MapOverlayRenderPolicy.resolve(
            zoom = 16.0,
            wantSectorFill = true,
            wantSitePoints = true,
            wantSiteLabels = true,
            wantServingLine = true,
            wantNeighborLines = true,
        )

        assertEquals(Int.MAX_VALUE, policy.sectorLimit)
        assertEquals(Int.MAX_VALUE, policy.pointLimit)
        assertEquals(Int.MAX_VALUE, policy.labelLimit)
        assertTrue(policy.showSectorFill)
        assertTrue(policy.showSitePoints)
        assertTrue(policy.showSiteLabels)
        assertTrue(policy.showServingLine)
        assertTrue(policy.showNeighborLines)
    }

    @Test
    fun `zoom changes never change visibility or record budgets`() {
        val policy = MapOverlayRenderPolicy.resolve(15.0, true, true, true, true, true)
        listOf(0.0, 5.0, 8.0, 11.49, 11.5, 13.49, 13.5, 15.0, 20.0).forEach { zoom ->
            assertEquals(policy, MapOverlayRenderPolicy.resolve(zoom, true, true, true, true, true))
        }
    }

    @Test
    fun `disabled overlays request no candidates at any zoom`() {
        listOf(6.2, 10.0, 12.0, 14.0, 16.0).forEach { zoom ->
            val policy = MapOverlayRenderPolicy.resolve(zoom, false, false, false, false, false)
            assertEquals(0, policy.candidateLimit)
        }
    }

    @Test
    fun `manual switches still win at full city zoom`() {
        val policy = MapOverlayRenderPolicy.resolve(8.0, false, false, false, false, false, wantPointIcons = true)
        assertFalse(policy.showSectorFill)
        assertFalse(policy.showSitePoints)
        assertFalse(policy.showSiteLabels)
        assertFalse(policy.showServingLine)
        assertFalse(policy.showNeighborLines)
        assertTrue(policy.showPointIcons)
        assertEquals(0, policy.sectorLimit)
        assertEquals(0, policy.labelLimit)
        assertEquals(Int.MAX_VALUE, policy.pointLimit)
    }

    @Test
    fun `imported icons keep a point budget with engineering sites switched off`() {
        listOf(6.2, 10.0, 12.0, 14.0, 16.0).forEach { zoom ->
            val policy = MapOverlayRenderPolicy.resolve(
                zoom, false, false, false, false, false, wantPointIcons = true,
            )
            assertFalse(policy.showSitePoints)
            assertTrue(policy.showPointIcons)
            assertTrue(policy.pointLimit > 0)
            assertEquals(policy.pointLimit, policy.candidateLimit)
        }
    }
}
