package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.TestMetricMode

object TestPointColorScale {
    private const val COLOR_INVALID = "#9AA0A6"
    private const val COLOR_BAD = "#D93025"
    private const val COLOR_WARN = "#F29900"
    private const val COLOR_FAIR = "#FABB05"
    private const val COLOR_GOOD = "#34A853"

    fun colorHex(
        metricMode: TestMetricMode,
        value: Int?,
    ): String {
        if (value == null) return COLOR_INVALID
        return when (metricMode) {
            TestMetricMode.RSRP -> rsrpColor(value)
            TestMetricMode.SINR -> sinrColor(value)
        }
    }

    private fun rsrpColor(value: Int): String = when {
        value >= -100 -> COLOR_GOOD
        value >= -108 -> COLOR_FAIR
        value >= -118 -> COLOR_WARN
        else -> COLOR_BAD
    }

    private fun sinrColor(value: Int): String = when {
        value >= 15 -> COLOR_GOOD
        value >= 5 -> COLOR_FAIR
        value >= 0 -> COLOR_WARN
        else -> COLOR_BAD
    }
}
