package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.map.GeoBounds
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch

data class MapViewportSnapshot(
    val centerLongitude: Double,
    val centerLatitude: Double,
    val zoom: Double,
    val bounds: GeoBounds?,
)

internal fun resolveMapViewportSnapshot(
    retainedSnapshot: MapViewportSnapshot?,
    currentLocation: LocationFix?,
    simMatches: List<SimCellMatch>,
    visibleSectors: List<SectorRecord>,
): MapViewportSnapshot = retainedSnapshot ?: defaultMapViewportSnapshot(
    currentLocation = currentLocation,
    simMatches = simMatches,
    visibleSectors = visibleSectors,
)

internal fun defaultMapViewportSnapshot(
    currentLocation: LocationFix?,
    simMatches: List<SimCellMatch>,
    visibleSectors: List<SectorRecord>,
): MapViewportSnapshot {
    currentLocation?.let { location ->
        return MapViewportSnapshot(
            centerLongitude = location.longitude,
            centerLatitude = location.latitude,
            zoom = defaultMapZoom(currentLocation = currentLocation, simMatches = simMatches, visibleSectors = visibleSectors),
            bounds = null,
        )
    }
    simMatches.firstNotNullOfOrNull { it.servingSector }?.let { sector ->
        return MapViewportSnapshot(
            centerLongitude = sector.longitude,
            centerLatitude = sector.latitude,
            zoom = defaultMapZoom(currentLocation = currentLocation, simMatches = simMatches, visibleSectors = visibleSectors),
            bounds = null,
        )
    }
    visibleSectors.firstOrNull()?.let { sector ->
        return MapViewportSnapshot(
            centerLongitude = sector.longitude,
            centerLatitude = sector.latitude,
            zoom = defaultMapZoom(currentLocation = currentLocation, simMatches = simMatches, visibleSectors = visibleSectors),
            bounds = null,
        )
    }
    return MapViewportSnapshot(
        centerLongitude = 0.0,
        centerLatitude = 0.0,
        zoom = defaultMapZoom(currentLocation = currentLocation, simMatches = simMatches, visibleSectors = visibleSectors),
        bounds = null,
    )
}

internal fun defaultMapZoom(
    currentLocation: LocationFix?,
    simMatches: List<SimCellMatch>,
    visibleSectors: List<SectorRecord>,
): Double = when {
    simMatches.any { it.servingSector != null } -> 15.0
    currentLocation != null -> 15.0
    visibleSectors.isNotEmpty() -> 11.5
    else -> 5.2
}
