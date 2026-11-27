package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.CellSnapshot
import com.xywy.signalmap.domain.model.SimCellSnapshot

object CellSnapshotContentComparator {
    fun areEquivalent(
        previous: CellSnapshot,
        candidate: CellSnapshot,
    ): Boolean {
        if (previous.permissionMissing != candidate.permissionMissing) return false
        if (previous.airplaneModeOn != candidate.airplaneModeOn) return false
        if (previous.errorMessage != candidate.errorMessage) return false
        return previous.simCards.normalized() == candidate.simCards.normalized()
    }
}

private fun List<SimCellSnapshot>.normalized(): List<SimCellSnapshot> = map { snapshot ->
    snapshot.copy(
        servingCell = snapshot.servingCell?.normalized(),
        neighbors = snapshot.neighbors.map(CellSignal::normalized),
    )
}

private fun CellSignal.normalized(): CellSignal = copy(observedAtMillis = 0L, observedAtElapsedMillis = null)
