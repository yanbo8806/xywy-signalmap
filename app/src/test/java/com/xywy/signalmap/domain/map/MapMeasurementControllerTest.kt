package com.xywy.signalmap.domain.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapMeasurementControllerTest {
    @Test
    fun `distance mode starts empty and accumulates after second point`() {
        var state = MapMeasurementController.startDistance()

        assertEquals(MapMeasurementMode.DISTANCE, state.mode)
        assertTrue(state.points.isEmpty())
        assertFalse(state.canFinish)

        state = MapMeasurementController.addPoint(state, longitude = 120.8000, latitude = 34.4000)
        assertEquals(1, state.points.size)
        assertEquals(0.0, state.distanceMeters)
        assertFalse(state.canFinish)

        state = MapMeasurementController.addPoint(state, longitude = 120.8010, latitude = 34.4010)
        assertEquals(2, state.points.size)
        assertTrue((state.distanceMeters ?: 0.0) > 0.0)
        assertTrue(state.canFinish)
    }

    @Test
    fun `area mode previews area from third point and finish locks result`() {
        var state = MapMeasurementController.startArea()
        state = MapMeasurementController.addPoint(state, longitude = 120.8000, latitude = 34.4000)
        state = MapMeasurementController.addPoint(state, longitude = 120.8010, latitude = 34.4000)

        assertEquals(null, state.areaSquareMeters)
        assertFalse(state.canFinish)

        state = MapMeasurementController.addPoint(state, longitude = 120.8010, latitude = 34.4010)
        assertNotNull(state.areaSquareMeters)
        assertTrue((state.areaSquareMeters ?: 0.0) > 0.0)
        assertTrue(state.canFinish)

        val finished = MapMeasurementController.finish(state)
        assertTrue(finished.isFinished)
        assertEquals(state.areaSquareMeters, finished.areaSquareMeters)
    }

    @Test
    fun `clear resets measurement state`() {
        var state = MapMeasurementController.startDistance()
        state = MapMeasurementController.addPoint(state, longitude = 120.8, latitude = 34.4)
        state = MapMeasurementController.addPoint(state, longitude = 120.81, latitude = 34.41)

        val cleared = MapMeasurementController.clear()

        assertEquals(MapMeasurementState(), cleared)
    }
}
