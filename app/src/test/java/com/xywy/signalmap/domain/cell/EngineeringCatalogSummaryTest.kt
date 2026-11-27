package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*
import kotlin.test.*

class EngineeringCatalogSummaryTest {
    private fun record(id: String, tech: RadioTechnology, band: String?) = SectorRecord(
        id, "layer", tech, band = band, longitude = 120.8, latitude = 34.4,
    )
    @Test fun `summary retains mixed layer technologies and sorted distinct bands`() {
        val records = listOf(record("1", RadioTechnology.NR, "N41"),
            record("2", RadioTechnology.LTE, "B3"), record("3", RadioTechnology.LTE, "B3"),
            record("4", RadioTechnology.UNKNOWN, " "))
        val doc = LayerDocument(LayerMetadata("layer", "工参", "test.xlsx", 1, true, 4), records)
        val summary = EngineeringCatalogSummary.create(listOf(doc))
        assertEquals(listOf("B3", "N41"), summary.bands)
        assertEquals(setOf(RadioTechnology.NR, RadioTechnology.LTE, RadioTechnology.UNKNOWN), summary.technologiesByLayer["layer"])
    }
    @Test fun `unchanged record lists skip rebuilding but edits visibility and reordering invalidate`() {
        val rows = listOf(record("1", RadioTechnology.NR, "N41"))
        val other = listOf(record("2", RadioTechnology.LTE, "B3"))
        assertTrue(sameSectorLists(listOf(rows), listOf(rows)))
        assertFalse(sameSectorLists(listOf(rows), listOf(emptyList())))
        assertFalse(sameSectorLists(listOf(rows), listOf(listOf(rows[0].copy(cellName = "changed")))))
        assertFalse(sameSectorLists(listOf(rows, other), listOf(other, rows)))
    }
}
