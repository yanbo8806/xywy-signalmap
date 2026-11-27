package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.SectorRecord

data class MapOverlayRenderPolicyState(
    val sectorLimit: Int,
    val pointLimit: Int,
    val labelLimit: Int,
    val showSectorFill: Boolean,
    val showSitePoints: Boolean,
    val showSiteLabels: Boolean,
    val showServingLine: Boolean,
    val showNeighborLines: Boolean,
    val showPointIcons: Boolean = showSitePoints,
) {
    fun showsPoint(record: SectorRecord): Boolean =
        if (record.isPointOnly) showPointIcons else showSitePoints

    val candidateLimit: Int
        get() = maxOf(
            if (showSectorFill) sectorLimit else 0,
            if (showSitePoints || showPointIcons) pointLimit else 0,
            if (showSiteLabels) labelLimit else 0,
        )
}

object MapOverlayRenderPolicy {
    fun resolve(
        @Suppress("UNUSED_PARAMETER")
        zoom: Double,
        wantSectorFill: Boolean,
        wantSitePoints: Boolean,
        wantSiteLabels: Boolean,
        wantServingLine: Boolean,
        wantNeighborLines: Boolean,
        wantPointIcons: Boolean = wantSitePoints,
    ): MapOverlayRenderPolicyState = MapOverlayRenderPolicyState(
        // The user controls visibility. Neither zoom nor record count reduces the layer.
        sectorLimit = if (wantSectorFill) Int.MAX_VALUE else 0,
        pointLimit = if (wantSitePoints || wantPointIcons) Int.MAX_VALUE else 0,
        labelLimit = if (wantSiteLabels) Int.MAX_VALUE else 0,
        showSectorFill = wantSectorFill,
        showSitePoints = wantSitePoints,
        showPointIcons = wantSitePoints || wantPointIcons,
        showSiteLabels = wantSiteLabels,
        showServingLine = wantServingLine,
        showNeighborLines = wantNeighborLines,
    )
}
