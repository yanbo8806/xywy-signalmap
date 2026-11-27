package com.xywy.signalmap.data.telephony

object TelephonyRefreshPolicy {
    val snapshotPollIntervalMillis: Long = 1_000L
    const val cellInfoRequestIntervalMillis: Long = 2_000L
    const val requestTimeoutMillis: Long = 5_000L
    const val staleAfterMillis: Long = 15_000L

    fun isFresh(measuredAtMillis: Long?, nowMillis: Long): Boolean =
        measuredAtMillis != null && measuredAtMillis > 0 && nowMillis - measuredAtMillis in 0..staleAfterMillis
}
