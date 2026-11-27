package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*

data class SignalStatistics(
    val slotIndex: Int, val subscriptionId: Int?, val technology: RadioTechnology,
    val total: Int, val stale: Int, val rsrpCount: Int, val sinrCount: Int,
    val weakCount: Int, val lowSinrCount: Int, val averageRsrp: Double?, val averageSinr: Double?,
)
data class IndexedNetworkEvent(val sampleIndex: Int, val event: TestNetworkEvent)
data class TestLogAnalysis(
    val statistics: List<SignalStatistics>,
    val events: List<IndexedNetworkEvent>,
    val locatedSamples: Int,
    val gaps: Int,
    val samplesWithoutSimInfo: Int,
) {
    companion object {
        fun create(samples: List<TestLogSample>): TestLogAnalysis {
            data class Key(val slot: Int, val subscription: Int?, val tech: RadioTechnology)
            class Counter { var total = 0; var stale = 0; var rsrp = 0; var sinr = 0; var weak = 0; var low = 0; var rsrpSum = 0L; var sinrSum = 0L }
            val counters = linkedMapOf<Key, Counter>()
            val events = mutableListOf<IndexedNetworkEvent>()
            var gaps = 0
            samples.forEachIndexed { index, sample ->
                val detected = NetworkEventDetector.detect(samples.getOrNull(index - 1), sample)
                if (detected.any { it.title == "采样不连续" }) gaps++
                // Old archives have no saved events. New ones retain the original diagnostic context.
                (sample.events + detected).distinct().forEach { events += IndexedNetworkEvent(index, it) }
                sample.sims.forEach { sim ->
                    val cell = sim.servingCell
                    val count = counters.getOrPut(Key(sim.slotIndex, sim.subscriptionId, cell?.radioTechnology ?: RadioTechnology.UNKNOWN)) { Counter() }
                    count.total++
                    if (cell?.isStale == true) count.stale++
                    if (cell == null || cell.isStale) return@forEach
                    val rsrpRange = when (cell.radioTechnology) {
                        RadioTechnology.LTE -> -140..-43
                        RadioTechnology.NR -> -156..-31
                        else -> IntRange.EMPTY
                    }
                    val sinrRange = when (cell.radioTechnology) {
                        RadioTechnology.LTE -> -20..30
                        RadioTechnology.NR -> -23..40
                        else -> IntRange.EMPTY
                    }
                    cell.rsrp?.takeIf { it in rsrpRange }?.let { count.rsrp++; count.rsrpSum += it; if (it < -110) count.weak++ }
                    cell.sinr?.takeIf { it in sinrRange }?.let { count.sinr++; count.sinrSum += it; if (it < 0) count.low++ }
                }
            }
            return TestLogAnalysis(counters.map { (key, c) -> SignalStatistics(key.slot, key.subscription, key.tech, c.total, c.stale,
                c.rsrp, c.sinr, c.weak, c.low, if (c.rsrp > 0) c.rsrpSum.toDouble() / c.rsrp else null,
                if (c.sinr > 0) c.sinrSum.toDouble() / c.sinr else null) }, events, samples.count { it.point != null }, gaps, samples.count { it.sims.isEmpty() })
        }
    }
}

data class LogReplayState(val document: TestLogDocument, val analysis: TestLogAnalysis, val index: Int = 0, val playing: Boolean = false, val mapMode: Boolean = false) {
    val sample: TestLogSample? get() = document.samples.getOrNull(index)
    fun seek(index: Int) = copy(index = index.coerceIn(0, (document.samples.size - 1).coerceAtLeast(0)))
}
