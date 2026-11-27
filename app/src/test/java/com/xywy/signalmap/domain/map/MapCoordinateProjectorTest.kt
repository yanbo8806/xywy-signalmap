package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.TilePresetId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapCoordinateProjectorTest {
    @Test
    fun `amap display coordinates use gcj projection while osm keeps wgs84`() {
        val amapPoint = MapCoordinateProjector.toDisplayPoint(
            longitude = 120.75686,
            latitude = 34.37036,
            tilePresetId = TilePresetId.AMAP_ROAD,
        )
        val osmPoint = MapCoordinateProjector.toDisplayPoint(
            longitude = 120.75686,
            latitude = 34.37036,
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
        )

        assertTrue(amapPoint.longitude != 120.75686)
        assertTrue(amapPoint.latitude != 34.37036)
        assertEquals(120.75686, osmPoint.longitude)
        assertEquals(34.37036, osmPoint.latitude)
    }

    @Test
    fun `legacy tile IDs use the public AMap coordinate system`() {
        val googlePoint = MapCoordinateProjector.toDisplayPoint(
            longitude = 120.75686,
            latitude = 34.37036,
            tilePresetId = TilePresetId.GOOGLE_PROXY,
        )

        assertTrue(googlePoint.longitude != 120.75686)
        assertTrue(googlePoint.latitude != 34.37036)
    }

    @Test
    fun `amap raw and display projection can round trip`() {
        val displayPoint = MapCoordinateProjector.toDisplayPoint(
            longitude = 120.75686,
            latitude = 34.37036,
            tilePresetId = TilePresetId.AMAP_SATELLITE,
        )

        val rawPoint = MapCoordinateProjector.toRawPoint(
            longitude = displayPoint.longitude,
            latitude = displayPoint.latitude,
            tilePresetId = TilePresetId.AMAP_SATELLITE,
        )

        assertTrue(abs(rawPoint.longitude - 120.75686) < 0.0001)
        assertTrue(abs(rawPoint.latitude - 34.37036) < 0.0001)
    }

    @Test
    fun `amap projection transforms location sector polygon and sim matches`() {
        val sector = SectorRecord(
            id = "sector-1",
            layerId = "layer",
            radioTechnology = RadioTechnology.NR,
            siteName = "Site-A",
            longitude = 120.75686,
            latitude = 34.37036,
            polygonCoordinates = listOf(
                GeoPoint(120.75686, 34.37036),
                GeoPoint(120.75786, 34.37036),
                GeoPoint(120.75686, 34.37136),
            ),
        )
        val location = LocationFix(
            longitude = 120.75686,
            latitude = 34.37036,
            accuracyMeters = 5f,
            timeMillis = 1L,
        )
        val matches = listOf(
            SimCellMatch(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingSector = sector,
                neighborSectors = listOf(sector.copy(id = "neighbor-1")),
            ),
        )

        val projectedSector = MapCoordinateProjector.toDisplaySector(sector, TilePresetId.AMAP_ROAD)
        val projectedLocation = MapCoordinateProjector.toDisplayLocation(location, TilePresetId.AMAP_ROAD)
        val projectedMatches = MapCoordinateProjector.toDisplaySimMatches(matches, TilePresetId.AMAP_ROAD)
        val osmSector = MapCoordinateProjector.toDisplaySector(sector, TilePresetId.OPEN_STREET_MAP)

        assertTrue(projectedSector.longitude != sector.longitude)
        assertTrue(projectedSector.latitude != sector.latitude)
        assertTrue(projectedSector.polygonCoordinates.first().longitude != sector.polygonCoordinates.first().longitude)
        assertTrue(projectedLocation!!.longitude != location.longitude)
        assertTrue(projectedMatches.first().servingSector!!.longitude == projectedSector.longitude)
        assertTrue(projectedMatches.first().neighborSectors.first().latitude != sector.latitude)
        assertEquals(sector.longitude, osmSector.longitude)
        assertEquals(sector.latitude, osmSector.latitude)
    }
}
