package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.TestLogCellRecord
import com.xywy.signalmap.domain.model.TestLogPoint
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestLogSimRecord
import com.xywy.signalmap.domain.model.CellSnapshot

object TestLogSampleFactory {
    fun create(
        timestampMillis: Long,
        locationFix: LocationFix?,
        simMatches: List<SimCellMatch>,
        snapshot: CellSnapshot? = null,
        elapsedRealtimeMillis: Long? = null,
    ): TestLogSample {
        val primaryMatch = simMatches
            .sortedBy { it.slotIndex }
            .firstOrNull { it.servingCell != null }

        return TestLogSample(
            timestampMillis = timestampMillis,
            point = locationFix?.let(::toTestLogPoint),
            primaryMetricSource = primaryMatch?.let { "SIM${it.slotIndex + 1}" },
            primaryRsrp = primaryMatch?.servingCell?.rsrp,
            primarySinr = primaryMatch?.servingCell?.sinr,
            sims = simMatches
                .sortedBy { it.slotIndex }
                .map { match ->
                    val sim = snapshot?.simCards?.firstOrNull { it.subscriptionId == match.subscriptionId && it.slotIndex == match.slotIndex }
                    toTestLogSimRecord(match).copy(
                        errorMessage = sim?.errorMessage, serviceState = sim?.serviceState,
                        dataConnectionState = sim?.dataConnectionState, matchDiagnostic = match.matchDiagnostic,
                        servingCell = match.servingCell?.let { cell ->
                            cell.toTestLogCellRecord(match.servingSector).copy(
                                measurementAgeMillis = if (elapsedRealtimeMillis != null && cell.observedAtElapsedMillis != null)
                                    (elapsedRealtimeMillis - cell.observedAtElapsedMillis).takeIf { it >= 0 } else null,
                            )
                        },
                    )
                },
            airplaneModeOn = snapshot?.airplaneModeOn,
            diagnosticMessage = snapshot?.errorMessage,
        )
    }

    private fun toTestLogPoint(locationFix: LocationFix): TestLogPoint = TestLogPoint(
        longitude = locationFix.longitude,
        latitude = locationFix.latitude,
        accuracyMeters = locationFix.accuracyMeters,
    )

    private fun toTestLogSimRecord(match: SimCellMatch): TestLogSimRecord = TestLogSimRecord(
        subscriptionId = match.subscriptionId,
        slotIndex = match.slotIndex,
        label = match.label,
        servingCell = match.servingCell?.toTestLogCellRecord(match.servingSector),
        neighbors = match.neighbors.mapIndexed { index, signal ->
            signal.toTestLogCellRecord(match.neighborMatches.getOrNull(index))
        },
    )

    private fun CellSignal.toTestLogCellRecord(sector: SectorRecord?): TestLogCellRecord = TestLogCellRecord(
        radioTechnology = radioTechnology,
        cellId = cellId,
        pci = pci,
        tacOrLac = tacOrLac,
        channelNumber = channelNumber,
        band = band,
        bandwidth = bandwidth,
        rsrp = rsrp,
        rsrq = rsrq,
        sinr = sinr,
        dbm = dbm,
        matchedSectorName = sector?.cellName,
        matchedSiteName = sector?.siteName,
        matchedSiteId = sector?.siteId,
        observedAtMillis = observedAtMillis,
        isStale = isStale,
        measurementSource = measurementSource,
    )
}
