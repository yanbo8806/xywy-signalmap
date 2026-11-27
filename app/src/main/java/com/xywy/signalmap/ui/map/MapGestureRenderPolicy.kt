package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.map.MapOverlayRenderPolicyState

data class MapGestureLayerVisibility(
    val sector: Boolean,
    val sitePoints: Boolean,
    val siteLabels: Boolean,
    val servingLine: Boolean,
    val neighborLine: Boolean,
    val testLog: Boolean,
    val measurement: Boolean,
    val userDraft: Boolean,
    val pointIcons: Boolean = sitePoints,
)

object MapGestureRenderPolicy {
    fun resolve(
        @Suppress("UNUSED_PARAMETER")
        isGestureInteracting: Boolean,
        overlayPolicy: MapOverlayRenderPolicyState,
    ): MapGestureLayerVisibility {
        // Keep the existing sources visible throughout pan/pinch gestures.
        return MapGestureLayerVisibility(
            sector = overlayPolicy.showSectorFill,
            sitePoints = overlayPolicy.showSitePoints,
            pointIcons = overlayPolicy.showPointIcons,
            siteLabels = overlayPolicy.showSiteLabels,
            servingLine = overlayPolicy.showServingLine,
            neighborLine = overlayPolicy.showNeighborLines,
            testLog = true,
            measurement = true,
            userDraft = true,
        )
    }
}
