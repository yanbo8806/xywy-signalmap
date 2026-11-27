package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapRecordFilterTest {
    private val generic = SectorRecord(
        id = "generic", layerId = "sheet", radioTechnology = RadioTechnology.UNKNOWN,
        longitude = 120.8, latitude = 34.4, azimuth = 60.0,
    )

    @Test
    fun `generic sectors and points survive network and band filters`() {
        val techs = setOf(RadioTechnology.NR, RadioTechnology.LTE)
        assertTrue(MapRecordFilter.matches(generic, techs, setOf("N41")))
        assertTrue(MapRecordFilter.matches(generic.copy(isPointOnly = true), techs, setOf("N41")))
    }

    @Test
    fun `engineering sectors still obey technology and band choices`() {
        val lte = generic.copy(radioTechnology = RadioTechnology.LTE, band = "B8")
        assertFalse(MapRecordFilter.matches(lte, setOf(RadioTechnology.NR), emptySet()))
        assertFalse(MapRecordFilter.matches(lte, setOf(RadioTechnology.LTE), setOf("B3")))
        assertTrue(MapRecordFilter.matches(lte, setOf(RadioTechnology.LTE), setOf("B8")))
        assertTrue(MapRecordFilter.matches(lte, emptySet(), emptySet()))
    }

    @Test
    fun `explicit geometry and point-only engineering sheets are not radio filters`() {
        val lte = generic.copy(radioTechnology = RadioTechnology.LTE)
        assertTrue(MapRecordFilter.matches(lte.copy(isPointOnly = true), setOf(RadioTechnology.NR), setOf("N41")))
        assertTrue(MapRecordFilter.matches(lte.copy(polygonCoordinates = listOf(GeoPoint(120.8, 34.4))), setOf(RadioTechnology.NR), setOf("N41")))
    }
}
