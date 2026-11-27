package com.xywy.signalmap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RasterLayerColorizerTest {
    @Test
    fun `colorizer applies target rgb with uniform configured opacity`() {
        val pixels = RasterLayerColorizer.colorize(
            alphaMask = byteArrayOf(0, 1, 127.toByte(), 255.toByte()),
            colorHex = "#3366FF",
            opacity = 0.5f,
        )

        assertEquals(4, pixels.size)
        assertEquals(0x003366FF, pixels[0])
        assertEquals(0x803366FF.toInt(), pixels[1])
        assertEquals(0x803366FF.toInt(), pixels[2])
        assertEquals(0x803366FF.toInt(), pixels[3])
    }
}
