package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.EngineeringLayerBinding
import com.xywy.signalmap.domain.model.EngineeringLayerBindingMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimEngineeringBindings
import com.xywy.signalmap.domain.model.SimTechEngineeringBinding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SectorMatchIndexTest {
    @Test
    fun `auto and specific scopes share the index for a single layer`() {
        val sector = SectorRecord("one", "layer-a", RadioTechnology.NR, siteId = "123456",
            cellId = "460-00-123456-1", longitude = 120.8, latitude = 34.4)
        val index = SectorMatchIndex.build(listOf(sector))
        val automatic = index.scope(0, RadioTechnology.NR, SimEngineeringBindings())
        val explicit = index.scope(0, RadioTechnology.NR, SimEngineeringBindings(perSlot = mapOf(
            0 to SimTechEngineeringBinding(nr = EngineeringLayerBinding(EngineeringLayerBindingMode.SPECIFIC_LAYER, "layer-a")))))
        kotlin.test.assertSame(automatic, explicit)
        assertEquals(listOf(sector), automatic.byCellId((123456L * 4096 + 1).toString()))
        assertEquals(listOf(sector), automatic.byCellId("1"))
    }
    @Test
    fun `specific layer scope only returns matching layer and technology`() {
        val index = SectorMatchIndex.build(
            listOf(
                SectorRecord(
                    id = "lte-layer-a",
                    layerId = "layer-a",
                    radioTechnology = RadioTechnology.LTE,
                    cellId = "101",
                    pci = "11",
                    longitude = 120.8,
                    latitude = 34.4,
                ),
                SectorRecord(
                    id = "lte-layer-b",
                    layerId = "layer-b",
                    radioTechnology = RadioTechnology.LTE,
                    cellId = "101",
                    pci = "11",
                    longitude = 120.9,
                    latitude = 34.5,
                ),
                SectorRecord(
                    id = "nr-layer-a",
                    layerId = "layer-a",
                    radioTechnology = RadioTechnology.NR,
                    cellId = "990001",
                    pci = "101",
                    longitude = 120.81,
                    latitude = 34.41,
                ),
            ),
        )

        val scope = index.scope(
            slotIndex = 0,
            technology = RadioTechnology.LTE,
            layerBindings = SimEngineeringBindings(
                perSlot = mapOf(
                    0 to SimTechEngineeringBinding(
                        lte = EngineeringLayerBinding(
                            mode = EngineeringLayerBindingMode.SPECIFIC_LAYER,
                            layerId = "layer-a",
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf("lte-layer-a"), scope.all.map { it.id })
        assertEquals(listOf("lte-layer-a"), scope.byCellId("101").map { it.id })
        assertEquals(listOf("lte-layer-a"), scope.byPci("11").map { it.id })
    }

    @Test
    fun `disabled scope returns no candidates`() {
        val index = SectorMatchIndex.build(
            listOf(
                SectorRecord(
                    id = "lte-layer-a",
                    layerId = "layer-a",
                    radioTechnology = RadioTechnology.LTE,
                    cellId = "101",
                    pci = "11",
                    longitude = 120.8,
                    latitude = 34.4,
                ),
            ),
        )

        val scope = index.scope(
            slotIndex = 0,
            technology = RadioTechnology.LTE,
            layerBindings = SimEngineeringBindings(
                perSlot = mapOf(
                    0 to SimTechEngineeringBinding(
                        lte = EngineeringLayerBinding.Disabled,
                    ),
                ),
            ),
        )

        assertTrue(scope.all.isEmpty())
        assertTrue(scope.byCellId("101").isEmpty())
        assertTrue(scope.byPci("11").isEmpty())
    }
}
