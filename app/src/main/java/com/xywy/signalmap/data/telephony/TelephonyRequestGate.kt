package com.xywy.signalmap.data.telephony

/** Called under the repository lock; late replies carry an obsolete ticket. */
internal class TelephonyRequestGate {
    private var serial = 0L
    private var pendingSince: Long? = null
    private var lastRequestAt: Long? = null

    fun timedOut(now: Long): Boolean = pendingSince?.let { now - it >= TelephonyRefreshPolicy.requestTimeoutMillis } == true

    fun begin(now: Long): Long? {
        if (pendingSince != null && !timedOut(now)) return null
        if (lastRequestAt?.let { now - it < TelephonyRefreshPolicy.cellInfoRequestIntervalMillis } == true) return null
        pendingSince = now
        lastRequestAt = now
        return ++serial
    }

    fun complete(ticket: Long): Boolean {
        if (ticket != serial || pendingSince == null) return false
        pendingSince = null
        return true
    }

    fun reset() {
        serial++
        pendingSince = null
        lastRequestAt = null
    }
}
