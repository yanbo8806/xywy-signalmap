package com.xywy.signalmap.domain.geo

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SectorGeometryTest {
    @Test
    fun `sector ring begins and ends at origin`() {
        val ring = SectorGeometry.buildSectorRing(
            longitude = 120.8,
            latitude = 34.4,
            azimuth = 90.0,
            radiusMeters = 600.0,
            beamWidthDegrees = 60.0,
            segments = 12,
        )
        assertEquals(120.8, ring.first().first)
        assertEquals(34.4, ring.first().second)
        assertEquals(ring.first(), ring.last())
        assertEquals(15, ring.size)
    }

    @Test
    fun `style catalog yields positive radius and beam width`() {
        val record = SectorRecord(
            id = "1",
            layerId = "layer",
            radioTechnology = RadioTechnology.LTE,
            longitude = 120.8,
            latitude = 34.4,
            band = "B3",
        )
        assertTrue(SectorStyleCatalog.radiusMeters(record) > 0)
        assertTrue(SectorStyleCatalog.beamWidthDegrees(record) > 0)
    }

    @Test
    fun `triangle ring starts and ends at apex`() {
        val ring = SectorGeometry.buildTriangleRing(
            longitude = 120.8,
            latitude = 34.4,
            azimuth = 90.0,
            radiusMeters = 600.0,
            beamWidthDegrees = 40.0,
        )

        assertEquals(4, ring.size)
        assertEquals(120.8, ring.first().first)
        assertEquals(34.4, ring.first().second)
        assertEquals(ring.first(), ring.last())
    }

    @Test
    fun `style catalog distinguishes lte and nr triangle styles`() {
        val lte = SectorRecord(
            id = "1",
            layerId = "layer",
            radioTechnology = RadioTechnology.LTE,
            longitude = 120.8,
            latitude = 34.4,
            band = "B8",
        )
        val nr = SectorRecord(
            id = "2",
            layerId = "layer",
            radioTechnology = RadioTechnology.NR,
            longitude = 120.8,
            latitude = 34.4,
            band = "N41",
        )

        assertTrue(SectorStyleCatalog.radiusMeters(lte) != SectorStyleCatalog.radiusMeters(nr))
        assertTrue(SectorStyleCatalog.beamWidthDegrees(lte) != SectorStyleCatalog.beamWidthDegrees(nr))
        assertTrue(SectorStyleCatalog.color(lte) != SectorStyleCatalog.color(nr))
    }

    @Test
    fun `style catalog caps rendered radius at one hundred meters`() {
        val records = listOf(
            SectorRecord(
                id = "lte-low",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                longitude = 120.8,
                latitude = 34.4,
                band = "B8",
            ),
            SectorRecord(
                id = "lte-mid",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                longitude = 120.8,
                latitude = 34.4,
                band = "B3",
            ),
            SectorRecord(
                id = "nr",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                longitude = 120.8,
                latitude = 34.4,
                band = "N41",
            ),
        )

        records.forEach { record ->
            assertTrue(SectorStyleCatalog.radiusMeters(record) <= 100.0)
        }
    }
}
