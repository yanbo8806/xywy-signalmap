package com.xywy.signalmap.domain.model

import kotlin.math.roundToInt

object RasterLayerColorizer {
    fun colorize(
        alphaMask: ByteArray,
        colorHex: String,
        opacity: Float,
    ): IntArray {
        val rgb = parseRgb(colorHex)
        val visibleAlpha = (opacity.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255) shl 24
        return IntArray(alphaMask.size) { index ->
            if ((alphaMask[index].toInt() and 0xFF) == 0) {
                rgb
            } else {
                visibleAlpha or rgb
            }
        }
    }

    private fun parseRgb(colorHex: String): Int {
        val normalized = colorHex.removePrefix("#")
        return when (normalized.length) {
            6 -> normalized.toInt(16) and 0x00FFFFFF
            8 -> normalized.toLong(16).toInt() and 0x00FFFFFF
            else -> error("不支持的颜色值：$colorHex")
        }
    }
}
