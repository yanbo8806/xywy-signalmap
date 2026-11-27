package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.TestMetricMode
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPointColorScaleTest {
    @Test
    fun `rsrp scale maps invalid and poor values to gray and red`() {
        assertEquals("#9AA0A6", TestPointColorScale.colorHex(TestMetricMode.RSRP, null))
        assertEquals("#D93025", TestPointColorScale.colorHex(TestMetricMode.RSRP, -123))
    }

    @Test
    fun `rsrp scale maps mid and good values to orange and green`() {
        assertEquals("#F29900", TestPointColorScale.colorHex(TestMetricMode.RSRP, -111))
        assertEquals("#34A853", TestPointColorScale.colorHex(TestMetricMode.RSRP, -97))
    }

    @Test
    fun `sinr scale maps bad and excellent values distinctly`() {
        assertEquals("#D93025", TestPointColorScale.colorHex(TestMetricMode.SINR, -3))
        assertEquals("#FABB05", TestPointColorScale.colorHex(TestMetricMode.SINR, 8))
        assertEquals("#34A853", TestPointColorScale.colorHex(TestMetricMode.SINR, 18))
    }
}
