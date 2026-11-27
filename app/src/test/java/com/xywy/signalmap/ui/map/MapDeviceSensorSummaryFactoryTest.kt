package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapDeviceSensorSummaryFactoryTest {
    @Test
    fun `factory formats vertical angle altitude pressure and azimuth`() {
        val summary = MapDeviceSensorSummaryFactory.create(
            DeviceSensorSnapshot(
                verticalAngleDegrees = -12.4f,
                pressureHPa = 1000.2f,
                altitudeMeters = 109.6f,
                azimuthDegrees = 359.6f,
                observedAtMillis = 1_000L,
            ),
        )

        assertTrue(summary.available)
        assertEquals("垂直 -12°", summary.verticalAngleText)
        assertEquals("海拔 110 m", summary.altitudeText)
        assertEquals("气压 1000 hPa", summary.pressureText)
        assertEquals("方位 360°", summary.azimuthText)
    }

    @Test
    fun `factory shows placeholder when sensors are unavailable`() {
        val summary = MapDeviceSensorSummaryFactory.create(DeviceSensorSnapshot.Empty)

        assertEquals("垂直 --", summary.verticalAngleText)
        assertEquals("海拔 --", summary.altitudeText)
        assertEquals("气压 --", summary.pressureText)
        assertEquals("方位 --", summary.azimuthText)
    }
}
