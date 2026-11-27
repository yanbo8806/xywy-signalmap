package com.xywy.signalmap.domain.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoordinateParserTest {
    @Test
    fun `parse supports comma format`() {
        val parsed = CoordinateParser.parse("120.8,34.4")
        assertEquals(120.8, parsed?.longitude)
        assertEquals(34.4, parsed?.latitude)
    }

    @Test
    fun `parse supports blank separated format`() {
        val parsed = CoordinateParser.parse("120.8 34.4")
        assertEquals(120.8, parsed?.longitude)
        assertEquals(34.4, parsed?.latitude)
    }

    @Test
    fun `parse rejects out of range values`() {
        assertNull(CoordinateParser.parse("280.1,34.4"))
    }
}
