package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.MapDisplayMode
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimEngineeringBindings
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.SimCellSnapshot

data class MapPanelPlan(
    val id: String,
    val title: String,
    val simMatches: List<SimCellMatch>,
)

object SimCellMatcher {
    fun match(
        simSnapshots: List<SimCellSnapshot>,
        sectors: List<SectorRecord>,
        currentLocation: LocationFix? = null,
        layerBindings: SimEngineeringBindings = SimEngineeringBindings(),
        sectorIndex: SectorMatchIndex = SectorMatchIndex.build(sectors),
    ): List<SimCellMatch> = simSnapshots
        .sortedBy { it.slotIndex }
        .map { simSnapshot ->
            val servingSector = simSnapshot.servingCell?.let { signal ->
                findServingSectorMatch(
                    signal = signal,
                    scope = sectorIndex.scope(
                        slotIndex = simSnapshot.slotIndex,
                        technology = signal.radioTechnology,
                        layerBindings = layerBindings,
                    ),
                    anchor = currentLocation,
                )
            }
            val neighborMatches = simSnapshot.neighbors.map { signal ->
                findNeighborSectorMatch(
                    signal = signal,
                    scope = sectorIndex.scope(
                        slotIndex = simSnapshot.slotIndex,
                        technology = signal.radioTechnology,
                        layerBindings = layerBindings,
                    ),
                    anchor = currentLocation,
                )
            }
            SimCellMatch(
                subscriptionId = simSnapshot.subscriptionId,
                slotIndex = simSnapshot.slotIndex,
                label = simSnapshot.label,
                servingCell = simSnapshot.servingCell,
                servingSector = servingSector,
                neighbors = simSnapshot.neighbors,
                neighborMatches = neighborMatches,
                neighborSectors = neighborMatches
                    .mapNotNull { it }
                    .distinctBy { it.id }
                    .take(8),
                matchDiagnostic = EngineeringMatchDiagnostics.describe(
                    simSnapshot.servingCell, simSnapshot.slotIndex, layerBindings, sectorIndex, currentLocation,
                ),
            )
        }

    fun findNeighborDisplayMatch(
        signal: CellSignal,
        matchedSectors: List<SectorRecord>,
    ): SectorRecord? {
        val normalizedBand = signal.band.normalizedBand() ?: return null
        val normalizedChannel = signal.channelNumber.normalizedChannelOrNull() ?: return null
        val filtered = matchedSectors.filter { sector ->
            sector.radioTechnology == signal.radioTechnology &&
                sector.band.normalizedBand() == normalizedBand &&
                sector.channelNumber.normalizedChannelOrNull() == normalizedChannel
        }
        if (filtered.isEmpty()) return null

        val normalizedCellId = signal.cellId.normalizedOrNull()
        if (normalizedCellId != null) {
            filtered.firstOrNull { sector ->
                sector.cellId.normalizedOrNull() == normalizedCellId
            }?.let { return it }
        }

        val normalizedPci = signal.pci.normalizedOrNull()
        if (normalizedPci != null) {
            filtered.firstOrNull { sector ->
                sector.pci.normalizedOrNull() == normalizedPci
            }?.let { return it }
        }
        return null
    }
}

object MapDisplayPlanner {
    fun build(
        mode: MapDisplayMode,
        simMatches: List<SimCellMatch>,
    ): List<MapPanelPlan> {
        val ordered = simMatches.sortedBy { it.slotIndex }
        return if (mode == MapDisplayMode.Split && ordered.size > 1) {
            ordered.map { match ->
                MapPanelPlan(
                    id = "sim-${match.slotIndex}",
                    title = match.label.ifBlank { "SIM ${match.slotIndex + 1}" },
                    simMatches = listOf(match),
                )
            }
        } else {
            listOf(
                MapPanelPlan(
                    id = "overlay",
                    title = if (ordered.size > 1) "双卡叠加" else ordered.firstOrNull()?.label ?: "地图",
                    simMatches = ordered,
                ),
            )
        }
    }
}

object SimColorPalette {
    fun servingColor(slotIndex: Int): String = when (slotIndex) {
        0 -> "#FF4D4F"
        1 -> "#1677FF"
        else -> "#722ED1"
    }

    fun neighborColor(slotIndex: Int): String = when (slotIndex) {
        0 -> "#FFA39E"
        1 -> "#91CAFF"
        else -> "#D3ADF7"
    }
}

private fun findServingSectorMatch(
    signal: CellSignal,
    scope: SectorLookupScope,
    anchor: LocationFix?,
): SectorRecord? {
    val normalizedCellId = signal.cellId.normalizedOrNull()
    if (normalizedCellId != null) {
        rankCandidates(
            candidates = scope.byCellId(normalizedCellId),
            signal = signal,
            anchor = anchor,
        )?.let { return it }
    }

    val normalizedPci = signal.pci.normalizedOrNull()
    if (normalizedPci != null) {
        return rankCandidates(
            candidates = scope.byPci(normalizedPci),
            signal = signal,
            anchor = anchor,
        )
    }

    return null
}

private fun findNeighborSectorMatch(
    signal: CellSignal,
    scope: SectorLookupScope,
    anchor: LocationFix?,
): SectorRecord? {
    val normalizedPci = signal.pci.normalizedOrNull() ?: return null
    val normalizedBand = signal.band.normalizedBand()
    val normalizedChannel = signal.channelNumber.normalizedChannelOrNull()
    val normalizedCellId = signal.cellId.normalizedOrNull()
    val candidates = scope.byPci(normalizedPci)
        .filter { candidate ->
            compatibleIfKnown(candidate.band.normalizedBand(), normalizedBand) &&
                compatibleIfKnown(candidate.channelNumber.normalizedChannelOrNull(), normalizedChannel) &&
                EngineeringMatchDistance.isWithinRange(anchor, candidate)
        }
    if (candidates.isEmpty()) return null
    return candidates.minWithOrNull(
        compareBy<SectorRecord>(
            { EngineeringMatchDistance.meters(anchor, it) },
            { mismatchWeight(it.cellId.normalizedOrNull(), normalizedCellId) },
            { mismatchWeight(it.tacOrLac.normalizedOrNull(), signal.tacOrLac.normalizedOrNull()) },
            { blankNamePenalty(it) },
        ),
    )
}

private fun rankCandidates(
    candidates: List<SectorRecord>,
    signal: CellSignal,
    anchor: LocationFix?,
): SectorRecord? {
    if (candidates.isEmpty()) return null
    return candidates.filter { EngineeringMatchDistance.isWithinRange(anchor, it) }.minWithOrNull(
        compareBy<SectorRecord>(
            { mismatchWeight(it.band.normalizedBand(), signal.band.normalizedBand()) },
            { mismatchWeight(it.tacOrLac.normalizedOrNull(), signal.tacOrLac.normalizedOrNull()) },
            { identityMismatchWeight(it, signal.cellId.normalizedOrNull()) },
            { blankNamePenalty(it) },
            { mismatchWeight(it.pci.normalizedOrNull(), signal.pci.normalizedOrNull()) },
            { EngineeringMatchDistance.meters(anchor, it) },
        ),
    )
}

private fun mismatchWeight(
    candidate: String?,
    expected: String?,
): Int = when {
    expected == null -> 0
    candidate == expected -> 0
    else -> 1
}

private fun identityMismatchWeight(
    candidate: SectorRecord,
    expected: String?,
): Int = when {
    expected == null -> 0
    expected in candidate.cellIdentityAliases() -> 0
    else -> 1
}

private fun blankNamePenalty(record: SectorRecord): Int = when {
    record.cellName.isNullOrBlank() && record.siteName.isNullOrBlank() -> 1
    else -> 0
}

private fun compatibleIfKnown(candidate: String?, expected: String?): Boolean =
    candidate == null || expected == null || candidate == expected

private fun String?.normalizedOrNull(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.uppercase()

private fun String?.normalizedBand(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.replace(" ", "")
    ?.uppercase()

private fun String?.normalizedChannelOrNull(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { raw ->
        raw.toLongOrNull()?.toString()
            ?: raw.toDoubleOrNull()
                ?.takeIf { numeric ->
                    kotlin.math.abs(numeric - numeric.toLong().toDouble()) < 0.0001
                }
                ?.toLong()
                ?.toString()
            ?: raw.uppercase()
    }
