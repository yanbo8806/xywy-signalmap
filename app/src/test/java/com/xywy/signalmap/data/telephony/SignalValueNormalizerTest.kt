package com.xywy.signalmap.data.telephony

import kotlin.test.*

class SignalValueNormalizerTest {
    @Test fun `legacy lte tenths are converted including small and negative values`() {
        assertEquals(18, SignalValueNormalizer.lteSinr(184, 29))
        assertEquals(2, SignalValueNormalizer.lteSinr(20, 28))
        assertEquals(-2, SignalValueNormalizer.lteSinr(-15, 26))
    }
    @Test fun `android 11 and newer use whole dB without double conversion`() {
        assertEquals(20, SignalValueNormalizer.lteSinr(20, 30))
        assertEquals(-15, SignalValueNormalizer.lteSinr(-15, 35))
        assertEquals(0, SignalValueNormalizer.lteSinr(0, 35))
        assertNull(SignalValueNormalizer.lteSinr(184, 35))
    }
    @Test fun `unavailable and out of range measurements stay unavailable`() {
        assertNull(SignalValueNormalizer.lteSinr(Int.MAX_VALUE, 29))
        assertNull(SignalValueNormalizer.lteSinr(Int.MIN_VALUE, 35))
        assertNull(SignalValueNormalizer.lteRsrp(99))
        assertNull(SignalValueNormalizer.nrRsrp(0))
        assertNull(SignalValueNormalizer.nrSinr(99))
        assertEquals(30, SignalValueNormalizer.nrSinr(30))
    }
}
