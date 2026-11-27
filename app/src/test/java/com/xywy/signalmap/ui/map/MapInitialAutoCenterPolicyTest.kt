package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.LocationFix
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapInitialAutoCenterPolicyTest {
    @Test
    fun `auto center runs once when first location fix arrives`() {
        val fix = LocationFix(
            longitude = 120.9398,
            latitude = 34.3416,
            accuracyMeters = 8f,
            timeMillis = 1L,
        )

        assertTrue(
            MapInitialAutoCenterPolicy.shouldAutoCenter(
                currentLocation = fix,
                alreadyAutoCentered = false,
                userGestureMovedMap = false,
            ),
        )
    }

    @Test
    fun `auto center does not override manual map movement`() {
        val fix = LocationFix(
            longitude = 120.9398,
            latitude = 34.3416,
            accuracyMeters = 8f,
            timeMillis = 1L,
        )

        assertFalse(
            MapInitialAutoCenterPolicy.shouldAutoCenter(
                currentLocation = fix,
                alreadyAutoCentered = false,
                userGestureMovedMap = true,
            ),
        )
    }

    @Test
    fun `auto center does not repeat after first run`() {
        val fix = LocationFix(
            longitude = 120.9398,
            latitude = 34.3416,
            accuracyMeters = 8f,
            timeMillis = 1L,
        )

        assertFalse(
            MapInitialAutoCenterPolicy.shouldAutoCenter(
                currentLocation = fix,
                alreadyAutoCentered = true,
                userGestureMovedMap = false,
            ),
        )
    }
}
