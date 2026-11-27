package com.xywy.signalmap.data.telephony

import kotlin.test.Test
import kotlin.test.assertEquals

class TelephonyRefreshPolicyTest {
    @Test
    fun `cell snapshot polling refreshes every second`() {
        assertEquals(1_000L, TelephonyRefreshPolicy.snapshotPollIntervalMillis)
    }
}
