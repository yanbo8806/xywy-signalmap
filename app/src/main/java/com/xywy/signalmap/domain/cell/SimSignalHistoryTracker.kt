package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.SimCellSnapshot

data class SignalHistoryRecord(
    val recordedAtMillis: Long,
    val servingCell: CellSignal,
)

data class SimSignalHistory(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
    val records: List<SignalHistoryRecord> = emptyList(),
)

object SimSignalHistoryTracker {
    private const val MAX_RECORDS_PER_SIM = 5

    fun update(
        histories: List<SimSignalHistory>,
        simSnapshots: List<SimCellSnapshot>,
    ): List<SimSignalHistory> {
        if (simSnapshots.isEmpty()) return emptyList()

        val existingByKey = histories.associateBy(::historyKey)
        return simSnapshots.map { snapshot ->
            val existing = existingByKey[historyKey(snapshot.subscriptionId, snapshot.slotIndex)]
            val previousRecords = existing?.records.orEmpty()
            val updatedRecords = snapshot.servingCell
                ?.let { servingCell ->
                    listOf(
                        SignalHistoryRecord(
                            recordedAtMillis = servingCell.observedAtMillis,
                            servingCell = servingCell.copy(),
                        ),
                    ) + previousRecords
                }
                ?: previousRecords

            SimSignalHistory(
                subscriptionId = snapshot.subscriptionId,
                slotIndex = snapshot.slotIndex,
                label = snapshot.label,
                records = updatedRecords.take(MAX_RECORDS_PER_SIM),
            )
        }
    }

    private fun historyKey(history: SimSignalHistory): String =
        historyKey(history.subscriptionId, history.slotIndex)

    private fun historyKey(subscriptionId: Int, slotIndex: Int): String =
        "$subscriptionId:$slotIndex"
}
