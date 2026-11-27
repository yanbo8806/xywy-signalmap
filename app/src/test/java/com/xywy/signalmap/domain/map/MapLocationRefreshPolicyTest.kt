package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.LocationFix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapLocationRefreshPolicyTest {
    @Test
    fun `first location fix becomes active immediately`() {
        val first = fix(longitude = 120.8, latitude = 34.4, timeMillis = 1_000L)

        val resolved = MapLocationRefreshPolicy.resolve(previous = null, candidate = first)

        assertEquals(first, resolved)
    }

    @Test
    fun `location within 100 meters stays on previous anchor even after 5 seconds`() {
        val previous = fix(longitude = 120.800000, latitude = 34.400000, timeMillis = 1_000L)
        val candidate = fix(longitude = 120.800500, latitude = 34.400100, timeMillis = 7_000L)

        val resolved = MapLocationRefreshPolicy.resolve(previous = previous, candidate = candidate)

        assertEquals(previous, resolved)
    }

    @Test
    fun `display location updates even when nearby render anchor stays unchanged`() {
        val previous = fix(longitude = 120.800000, latitude = 34.400000, timeMillis = 1_000L)
        val candidate = fix(longitude = 120.800010, latitude = 34.400010, timeMillis = 3_000L)

        val resolved = MapLocationRefreshPolicy.resolveDisplayLocation(previous = previous, candidate = candidate)

        assertEquals(candidate, resolved)
    }

    @Test
    fun `display location clears when gps becomes unavailable`() {
        val previous = fix(longitude = 120.800000, latitude = 34.400000, timeMillis = 1_000L)

        val resolved = MapLocationRefreshPolicy.resolveDisplayLocation(previous = previous, candidate = null)

        assertNull(resolved)
    }

    @Test
    fun `movement beyond 100 meters refreshes anchor after 5 seconds`() {
        val previous = fix(longitude = 120.800000, latitude = 34.400000, timeMillis = 1_000L)
        val candidate = fix(longitude = 120.802000, latitude = 34.401000, timeMillis = 7_000L)

        val resolved = MapLocationRefreshPolicy.resolve(previous = previous, candidate = candidate)

        assertEquals(candidate, resolved)
    }

    @Test
    fun `movement beyond 100 meters before 5 seconds still waits`() {
        val previous = fix(longitude = 120.800000, latitude = 34.400000, timeMillis = 1_000L)
        val candidate = fix(longitude = 120.802000, latitude = 34.401000, timeMillis = 4_000L)

        val resolved = MapLocationRefreshPolicy.resolve(previous = previous, candidate = candidate)

        assertEquals(previous, resolved)
    }

    @Test
    fun `null candidate clears active location`() {
        val previous = fix(longitude = 120.8, latitude = 34.4, timeMillis = 1_000L)

        val resolved = MapLocationRefreshPolicy.resolve(previous = previous, candidate = null)

        assertNull(resolved)
    }

    private fun fix(
        longitude: Double,
        latitude: Double,
        timeMillis: Long,
    ): LocationFix = LocationFix(
        longitude = longitude,
        latitude = latitude,
        accuracyMeters = 5f,
        timeMillis = timeMillis,
    )
}
