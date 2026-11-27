package com.xywy.signalmap.data.tile

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TilePayloadSizeGuardTest {
    @Test
    fun `reads small tile payload`() {
        val bytes = byteArrayOf(1, 2, 3)

        assertEquals(bytes.toList(), TilePayloadSizeGuard.readBytes(ByteArrayInputStream(bytes), maxBytes = 8).toList())
    }

    @Test
    fun `rejects oversized tile payload before allocating unbounded memory`() {
        val bytes = ByteArray(9) { it.toByte() }

        assertFailsWith<IllegalStateException> {
            TilePayloadSizeGuard.readBytes(ByteArrayInputStream(bytes), maxBytes = 8)
        }
    }
}
