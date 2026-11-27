package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapImportFocusPlannerTest {
    @Test
    fun `widely separated imports focus an actual record at a visible zoom`() {
        val records = listOf(record(120.0, 34.3), record(110.0, 36.0))
        val focus = assertNotNull(MapImportFocusPlanner.resolve(records))
        assertTrue(records.any { it.longitude == focus.longitude && it.latitude == focus.latitude })
        val policy = MapOverlayRenderPolicy.resolve(focus.zoom, true, true, true, true, true)
        assertTrue(policy.showSitePoints)
        assertTrue(policy.showSectorFill)
    }

    @Test
    fun `single point import retains its exact coordinates`() {
        assertEquals(MapImportFocus(120.8, 34.4, 15.0), MapImportFocusPlanner.resolve(listOf(record(120.8, 34.4))))
    }

    @Test
    fun `invalid or absent coordinates do not move the camera`() {
        assertNull(MapImportFocusPlanner.resolve(emptyList()))
        val invalid = listOf(record(Double.NaN, 34.4), record(120.8, 134.4), record(Double.POSITIVE_INFINITY, 34.4))
        assertNull(MapImportFocusPlanner.resolve(invalid))
        assertEquals(MapImportFocus(120.8, 34.4, 15.0), MapImportFocusPlanner.resolve(invalid + record(120.8, 34.4)))
    }

    private fun record(lon: Double, lat: Double) = SectorRecord(
        id = "$lon,$lat", layerId = "sheet", radioTechnology = RadioTechnology.UNKNOWN, longitude = lon, latitude = lat,
    )
}
