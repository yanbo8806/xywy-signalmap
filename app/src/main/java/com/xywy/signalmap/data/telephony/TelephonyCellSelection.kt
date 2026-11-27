package com.xywy.signalmap.data.telephony

import com.xywy.signalmap.domain.model.CellConnectionStatus
import com.xywy.signalmap.domain.model.CellSignal

internal object TelephonyCellSelection {
    private const val MAX_NEIGHBOR_COUNT = 8

    fun selectServing(cells: List<CellSignal>): CellSignal? = cells
        .filter { it.registered || it.connectionStatus == CellConnectionStatus.PRIMARY_SERVING || it.connectionStatus == CellConnectionStatus.SECONDARY_SERVING }
        .sortedWith(
            compareBy<CellSignal>(
                { servingPriority(it) },
                { if (it.registered) 0 else 1 },
            ).thenByDescending { it.dbm ?: Int.MIN_VALUE },
        )
        .firstOrNull()

    fun selectNeighbors(
        cells: List<CellSignal>,
        serving: CellSignal? = selectServing(cells),
    ): List<CellSignal> {
        val servingIdentityKey = serving?.identityKey()
        return cells.asSequence()
            .filterNot { candidate ->
                candidate === serving ||
                    candidate.connectionStatus == CellConnectionStatus.PRIMARY_SERVING ||
                    (servingIdentityKey != null && candidate.identityKey() == servingIdentityKey)
            }
            .sortedWith(
                compareBy<CellSignal>(
                    { sameChannelPriority(candidate = it, serving = serving) },
                    { secondaryServingPriority(it) },
                    { sameTechnologyPriority(candidate = it, serving = serving) },
                    { sameBandPriority(candidate = it, serving = serving) },
                ).thenByDescending { it.dbm ?: Int.MIN_VALUE },
            )
            .take(MAX_NEIGHBOR_COUNT)
            .toList()
    }

    private fun servingPriority(signal: CellSignal): Int = when (signal.connectionStatus) {
        CellConnectionStatus.PRIMARY_SERVING -> 0
        CellConnectionStatus.SECONDARY_SERVING -> 1
        CellConnectionStatus.NONE -> if (signal.registered) 1 else 3
        CellConnectionStatus.UNKNOWN -> if (signal.registered) 2 else 3
    }

    private fun secondaryServingPriority(signal: CellSignal): Int = when (signal.connectionStatus) {
        CellConnectionStatus.SECONDARY_SERVING -> 0
        CellConnectionStatus.PRIMARY_SERVING -> 2
        CellConnectionStatus.NONE -> if (signal.registered) 1 else 2
        CellConnectionStatus.UNKNOWN -> if (signal.registered) 1 else 2
    }

    private fun sameTechnologyPriority(
        candidate: CellSignal,
        serving: CellSignal?,
    ): Int = if (serving != null && candidate.radioTechnology == serving.radioTechnology) 0 else 1

    private fun sameBandPriority(
        candidate: CellSignal,
        serving: CellSignal?,
    ): Int = if (
        serving?.band != null &&
        candidate.band != null &&
        candidate.band.equals(serving.band, ignoreCase = true)
    ) {
        0
    } else {
        1
    }

    private fun sameChannelPriority(
        candidate: CellSignal,
        serving: CellSignal?,
    ): Int = if (
        serving?.channelNumber != null &&
        candidate.radioTechnology == serving.radioTechnology &&
        candidate.channelNumber != null &&
        candidate.channelNumber == serving.channelNumber
    ) {
        0
    } else {
        1
    }

    private fun CellSignal.identityKey(): String? {
        val identity = cellId?.takeIf(String::isNotBlank)
            ?: pci?.takeIf(String::isNotBlank)
            ?: return null
        return buildString {
            append(radioTechnology.name)
            append('|')
            append(identity)
            tacOrLac?.takeIf(String::isNotBlank)?.let {
                append('|')
                append(it)
            }
        }
    }
}
