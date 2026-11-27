package com.xywy.signalmap.domain.map

data class MapLayerRenderBudgetState(
    val sectorLimit: Int,
    val pointLimit: Int,
    val labelLimit: Int,
) {
    val candidateLimit: Int
        get() = maxOf(sectorLimit, pointLimit, labelLimit)

    companion object {
        val Empty = MapLayerRenderBudgetState(
            sectorLimit = 0,
            pointLimit = 0,
            labelLimit = 0,
        )
    }
}

object MapLayerRenderBudget {
    /** Legacy full-layer callers also receive an unlimited budget, independent of zoom. */
    fun forAlwaysRenderAllLayer(@Suppress("UNUSED_PARAMETER") zoom: Double): MapLayerRenderBudgetState =
        MapLayerRenderBudgetState(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
}
