package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.LocationFix

object MapInitialAutoCenterPolicy {
    const val ZOOM = 16.5

    fun shouldAutoCenter(
        currentLocation: LocationFix?,
        alreadyAutoCentered: Boolean,
        userGestureMovedMap: Boolean,
    ): Boolean = currentLocation != null && !alreadyAutoCentered && !userGestureMovedMap
}
