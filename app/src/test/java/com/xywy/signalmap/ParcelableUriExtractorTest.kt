package com.xywy.signalmap

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParcelableUriExtractorTest {
    @Test
    fun `uses legacy getter below android 13`() {
        var typedCalls = 0
        var legacyCalls = 0

        val result = ParcelableUriExtractor.extract(
            sdkInt = Build.VERSION_CODES.S,
            typedGetter = {
                typedCalls++
                "typed"
            },
            legacyGetter = {
                legacyCalls++
                "legacy"
            },
        )

        assertEquals("legacy", result)
        assertEquals(0, typedCalls)
        assertEquals(1, legacyCalls)
    }

    @Test
    fun `uses typed getter on android 13 and above`() {
        var typedCalls = 0
        var legacyCalls = 0

        val result = ParcelableUriExtractor.extract(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            typedGetter = {
                typedCalls++
                "typed"
            },
            legacyGetter = {
                legacyCalls++
                "legacy"
            },
        )

        assertEquals("typed", result)
        assertEquals(1, typedCalls)
        assertEquals(0, legacyCalls)
    }

    @Test
    fun `returns null when both getters have no value`() {
        val result = ParcelableUriExtractor.extract(
            sdkInt = Build.VERSION_CODES.S,
            typedGetter = { "typed" },
            legacyGetter = { null },
        )

        assertNull(result)
    }
}
