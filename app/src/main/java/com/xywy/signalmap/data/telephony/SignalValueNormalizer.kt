package com.xywy.signalmap.data.telephony

internal object SignalValueNormalizer {
    // Android 11 changed the public LTE RSSNR API from tenths of dB to whole dB.
    // Use the API contract, never guess the unit from the magnitude of a reading.
    fun lteSinr(raw: Int, sdkInt: Int): Int? = if (sdkInt <= 29) {
        raw.takeIf { it in -200..300 }?.let { Math.floorDiv(it, 10) }
    } else {
        raw.takeIf { it in -20..30 }
    }

    fun lteRsrp(raw: Int): Int? = raw.takeIf { it in -140..-43 }
    fun lteRsrq(raw: Int): Int? = raw.takeIf { it in -34..3 }
    fun nrRsrp(raw: Int): Int? = raw.takeIf { it in -156..-31 }
    fun nrRsrq(raw: Int): Int? = raw.takeIf { it in -43..20 }
    fun nrSinr(raw: Int): Int? = raw.takeIf { it in -23..40 }
}
