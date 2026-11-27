package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*
import kotlin.test.*

class EngineeringMatchDiagnosticsTest {
    private fun row(id: String = "1") = SectorRecord(id, "layer", RadioTechnology.LTE, cellId = "1000", pci = "5", longitude = 120.8, latitude = 34.4)
    private val signal = CellSignal(RadioTechnology.LTE, true, cellId = "1000", pci = "5")
    private val location = LocationFix(120.8, 34.4, 5f, 1L)
    @Test fun `unique ECI match is distinguished from ambiguous IDs and PCI fallback`() {
        val single = SectorMatchIndex.build(listOf(row()))
        assertEquals("ECI/NCI 精确匹配", EngineeringMatchDiagnostics.describe(signal, 0, SimEngineeringBindings(), single, location).method)
        val duplicate = SectorMatchIndex.build(listOf(row(), row("2")))
        assertEquals(2, EngineeringMatchDiagnostics.describe(signal, 0, SimEngineeringBindings(), duplicate, location).candidateCount)
        assertEquals("PCI 推测", EngineeringMatchDiagnostics.describe(signal.copy(cellId = "other"), 0, SimEngineeringBindings(), single, location).method)
    }
    @Test fun `disabled missing catalog and missing IDs have different explanations`() {
        val disabled = SimEngineeringBindings(perSlot = mapOf(0 to SimTechEngineeringBinding(lte = EngineeringLayerBinding.Disabled)))
        assertEquals("已禁用", EngineeringMatchDiagnostics.describe(signal, 0, disabled, SectorMatchIndex.Empty).method)
        assertEquals("无可用工参", EngineeringMatchDiagnostics.describe(signal, 0, SimEngineeringBindings(), SectorMatchIndex.Empty).method)
        assertEquals("未匹配", EngineeringMatchDiagnostics.describe(signal.copy(cellId = "2", pci = "6"), 0, SimEngineeringBindings(), SectorMatchIndex.build(listOf(row())), location).method)
    }
}
