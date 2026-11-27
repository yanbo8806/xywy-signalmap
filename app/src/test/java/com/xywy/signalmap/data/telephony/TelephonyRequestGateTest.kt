package com.xywy.signalmap.data.telephony

import kotlin.test.*

class TelephonyRequestGateTest {
    @Test fun `screen and recorder polling share a single in flight request`() {
        val gate = TelephonyRequestGate()
        val ticket = assertNotNull(gate.begin(1000))
        assertNull(gate.begin(1000))
        assertNull(gate.begin(3000))
        assertTrue(gate.complete(ticket))
        assertNotNull(gate.begin(3000))
    }
    @Test fun `successful requests still obey the polling interval`() {
        val gate = TelephonyRequestGate()
        gate.complete(assertNotNull(gate.begin(1000)))
        assertNull(gate.begin(1999))
        assertNotNull(gate.begin(3000))
    }
    @Test fun `missing callback retries after timeout and rejects late result`() {
        val gate = TelephonyRequestGate()
        val old = assertNotNull(gate.begin(1000))
        assertTrue(gate.timedOut(6000))
        val retry = assertNotNull(gate.begin(6000))
        assertFalse(gate.complete(old))
        assertTrue(gate.complete(retry))
    }
    @Test fun `radio reset rejects outstanding callbacks and permits immediate refresh`() {
        val gate = TelephonyRequestGate()
        val old = assertNotNull(gate.begin(1000))
        gate.reset()
        val fresh = assertNotNull(gate.begin(1100))
        assertFalse(gate.complete(old))
        assertTrue(gate.complete(fresh))
    }
}
