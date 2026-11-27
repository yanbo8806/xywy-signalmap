package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import kotlin.test.Test
import kotlin.test.assertEquals

class MapViewportSnapshotTest {
    @Test
    fun `retained viewport wins over current gps location`() {
        val retained = MapViewportSnapshot(
            centerLongitude = 120.8123,
            centerLatitude = 34.2567,
            zoom = 13.4,
            bounds = null,
        )
        val currentLocation = LocationFix(
            longitude = 120.9398,
            latitude = 34.3416,
            accuracyMeters = 8f,
            timeMillis = 1L,
        )

        val resolved = resolveMapViewportSnapshot(
            retainedSnapshot = retained,
            currentLocation = currentLocation,
            simMatches = emptyList(),
            visibleSectors = emptyList(),
        )

        assertEquals(retained, resolved)
    }

    @Test
    fun `defaults to current location when no retained viewport exists`() {
        val currentLocation = LocationFix(
            longitude = 120.9398,
            latitude = 34.3416,
            accuracyMeters = 8f,
            timeMillis = 1L,
        )

        val resolved = resolveMapViewportSnapshot(
            retainedSnapshot = null,
            currentLocation = currentLocation,
            simMatches = emptyList(),
            visibleSectors = emptyList(),
        )

        assertEquals(
            MapViewportSnapshot(
                centerLongitude = 120.9398,
                centerLatitude = 34.3416,
                zoom = 15.0,
                bounds = null,
            ),
            resolved,
        )
    }

    @Test
    fun `falls back to serving sector when current location is missing`() {
        val servingSector = SectorRecord(
            id = "sector-1",
            layerId = "layer-a",
            radioTechnology = RadioTechnology.LTE,
            longitude = 120.701,
            latitude = 34.401,
        )
        val match = SimCellMatch(
            subscriptionId = 1,
            slotIndex = 0,
            label = "SIM 1",
            servingSector = servingSector,
        )

        val resolved = resolveMapViewportSnapshot(
            retainedSnapshot = null,
            currentLocation = null,
            simMatches = listOf(match),
            visibleSectors = emptyList(),
        )

        assertEquals(
            MapViewportSnapshot(
                centerLongitude = 120.701,
                centerLatitude = 34.401,
                zoom = 15.0,
                bounds = null,
            ),
            resolved,
        )
    }
}
