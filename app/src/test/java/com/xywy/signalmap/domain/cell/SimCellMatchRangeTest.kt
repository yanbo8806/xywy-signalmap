package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*
import com.xywy.signalmap.map.GeoJsonFeatureFactory
import kotlin.test.*

class SimCellMatchRangeTest {
    private val location = LocationFix(0.0, 0.0, 5f, 1L)
    private val signal = CellSignal(RadioTechnology.LTE, true, cellId = "1000", pci = "33")

    private fun sector(id: String, meters: Double) = SectorRecord(
        id = id, layerId = "layer", radioTechnology = RadioTechnology.LTE,
        cellId = "1000", pci = "33", cellName = id,
        longitude = Math.toDegrees(meters / 6_371_000.0), latitude = 0.0,
    )

    private fun match(
        sectors: List<SectorRecord>,
        serving: CellSignal = signal,
        neighbors: List<CellSignal> = listOf(serving.copy(registered = false)),
        anchor: LocationFix? = location,
        bindings: SimEngineeringBindings = SimEngineeringBindings(),
    ) = SimCellMatcher.match(
        listOf(SimCellSnapshot(1, 0, "SIM1", servingCell = serving, neighbors = neighbors)),
        sectors, anchor, bindings,
    ).single()

    @Test fun `serving and neighbors include five kilometers but reject anything beyond it`() {
        for (technology in listOf(RadioTechnology.LTE, RadioTechnology.NR)) {
            for (meters in listOf(4_999.99, 5_000.0, 5_000.01, 50_000.0)) {
                val record = sector("cell", meters).copy(radioTechnology = technology)
                val result = match(listOf(record), serving = signal.copy(radioTechnology = technology))
                val expected = if (meters <= 5_000.0) record else null
                assertEquals(expected, result.servingSector, "$technology at $meters m")
                assertEquals(listOf(expected), result.neighborMatches)
                assertEquals(listOfNotNull(expected), result.neighborSectors)
            }
        }
    }

    @Test fun `distant exact identity never supplies a name line or recorded engineering data`() {
        val result = match(listOf(sector("远方小区", 5_001.0)))
        assertNull(result.servingSector)
        assertNull(result.neighborMatches.single())
        assertEquals("超出匹配范围", result.matchDiagnostic.method)
        assertEquals(0, result.matchDiagnostic.candidateCount)
        assertTrue(GeoJsonFeatureFactory.servingLineFeatures(location, listOf(result)).features().isNullOrEmpty())
        assertTrue(GeoJsonFeatureFactory.neighborLineFeatures(location, listOf(result)).features().isNullOrEmpty())
        val sample = TestLogSampleFactory.create(1L, location, listOf(result)).sims.single()
        assertNull(sample.servingCell?.matchedSectorName)
        assertNull(sample.neighbors.single().matchedSectorName)
        assertEquals(signal.cellId, sample.servingCell?.cellId)
    }

    @Test fun `nearby pci remains eligible when exact identity only exists outside the region`() {
        val near = sector("本地 PCI 小区", 200.0).copy(cellId = "2000")
        val result = match(listOf(sector("外地精确标识", 100_000.0), near))
        assertEquals(near, result.servingSector)
        assertEquals(near, result.neighborMatches.single())
        assertEquals("PCI 推测", result.matchDiagnostic.method)
        assertEquals(1, result.matchDiagnostic.candidateCount)
    }

    @Test fun `distance gate runs before serving candidate ranking`() {
        val near = sector("near", 100.0).copy(band = null, cellName = null)
        val far = sector("far", 8_000.0).copy(band = "B3")
        val result = match(listOf(far, near), serving = signal.copy(band = "B3"))
        assertEquals(near, result.servingSector)
        assertEquals(1, result.matchDiagnostic.candidateCount)
        assertEquals("ECI/NCI 精确匹配", result.matchDiagnostic.method)
    }

    @Test fun `neighbor chooses nearest same pci before names or incomplete identity hints`() {
        val near = sector("near", 100.0).copy(cellId = "2000", cellName = null, siteName = "最近站点", tacOrLac = "2")
        val far = sector("far", 900.0).copy(tacOrLac = "1")
        val result = match(listOf(far, near), neighbors = listOf(signal.copy(registered = false, tacOrLac = "1")))
        assertEquals(near, result.neighborMatches.single())
    }

    @Test fun `missing engineering band or channel still permits pci nearest matching`() {
        val near = sector("near", 100.0)
        val result = match(listOf(near), neighbors = listOf(signal.copy(registered = false, band = "B3", channelNumber = "1850")))
        assertEquals(near, result.neighborMatches.single())
    }

    @Test fun `known frequency conflicts and another technology are excluded`() {
        val right = sector("right", 1_000.0).copy(band = "B3", channelNumber = "1850.0")
        val records = listOf(
            right.copy(id = "wrong-channel", longitude = 0.0001, channelNumber = "1650"),
            right.copy(id = "wrong-band", longitude = 0.0002, band = "B1"),
            right.copy(id = "wrong-radio", longitude = 0.0003, radioTechnology = RadioTechnology.NR),
            right,
        )
        val result = match(records, neighbors = listOf(signal.copy(registered = false, band = "b3", channelNumber = "1850")))
        assertEquals(right, result.neighborMatches.single())
    }

    @Test fun `missing or invalid position never guesses names using the serving station location`() {
        for (anchor in listOf(null, location.copy(latitude = Double.NaN), location.copy(longitude = 181.0))) {
            val result = match(listOf(sector("cell", 100.0)), anchor = anchor)
            assertNull(result.servingSector)
            assertEquals(listOf(null), result.neighborMatches)
            assertTrue(result.neighborSectors.isEmpty())
            assertEquals("等待定位", result.matchDiagnostic.method)
        }
        val result = match(listOf(sector("bad-coordinates", 100.0).copy(latitude = Double.NaN)))
        assertNull(result.servingSector)
        assertNull(result.neighborMatches.single())
    }

    @Test fun `unmatched neighbor keeps its position without borrowing another name`() {
        val near = sector("near", 100.0)
        val far = sector("far", 6_000.0).copy(pci = "44")
        val result = match(listOf(near, far), neighbors = listOf(
            signal.copy(registered = false, pci = "44"),
            signal.copy(registered = false),
            signal.copy(registered = false, pci = null),
        ))
        assertEquals(listOf(null, near, null), result.neighborMatches)
        assertEquals(listOf(near), result.neighborSectors)
        val recorded = TestLogSampleFactory.create(1L, location, listOf(result)).sims.single().neighbors
        assertEquals(listOf(null, "near", null), recorded.map { it.matchedSectorName })
    }

    @Test fun `moving outside coverage clears previously matched names and lines`() {
        val record = sector("cell", 100.0)
        val before = match(listOf(record))
        assertEquals(record, before.servingSector)
        assertEquals(1, GeoJsonFeatureFactory.servingLineFeatures(location, listOf(before)).features()?.size)
        assertEquals(1, GeoJsonFeatureFactory.neighborLineFeatures(location, listOf(before)).features()?.size)
        val after = match(listOf(record), anchor = location.copy(latitude = 1.0))
        assertNull(after.servingSector)
        assertTrue(after.neighborSectors.isEmpty())
    }

    @Test fun `neighbor pci fallback respects explicit and disabled layer bindings`() {
        val near = sector("near", 100.0).copy(layerId = "other")
        val selected = sector("selected", 1_000.0)
        val bindings = SimEngineeringBindings(perSlot = mapOf(0 to SimTechEngineeringBinding(
            lte = EngineeringLayerBinding(EngineeringLayerBindingMode.SPECIFIC_LAYER, "layer"),
        )))
        assertEquals(selected, match(listOf(near, selected), bindings = bindings).neighborMatches.single())
        val disabled = SimEngineeringBindings(perSlot = mapOf(0 to SimTechEngineeringBinding(lte = EngineeringLayerBinding.Disabled)))
        assertNull(match(listOf(near, selected), bindings = disabled).neighborMatches.single())
    }
}
