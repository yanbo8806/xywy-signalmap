package com.xywy.signalmap.domain.map

import kotlin.test.Test
import kotlin.test.assertEquals

class MapLayerRenderBudgetTest {
    @Test
    fun `legacy full layer budget is unlimited at far zoom`() {
        val budget = MapLayerRenderBudget.forAlwaysRenderAllLayer(zoom = 10.0)

        assertEquals(Int.MAX_VALUE, budget.sectorLimit)
        assertEquals(Int.MAX_VALUE, budget.pointLimit)
        assertEquals(Int.MAX_VALUE, budget.labelLimit)
        assertEquals(Int.MAX_VALUE, budget.candidateLimit)
    }

    @Test
    fun `legacy full layer budget does not change with zoom`() {
        val budget = MapLayerRenderBudget.forAlwaysRenderAllLayer(zoom = 16.8)

        assertEquals(MapLayerRenderBudget.forAlwaysRenderAllLayer(8.0), budget)
        assertEquals(Int.MAX_VALUE, budget.candidateLimit)
    }
}
