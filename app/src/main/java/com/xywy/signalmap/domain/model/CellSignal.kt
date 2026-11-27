package com.xywy.signalmap.domain.model

enum class CellConnectionStatus {
    PRIMARY_SERVING,
    SECONDARY_SERVING,
    NONE,
    UNKNOWN,
}

data class CellSignal(
    val radioTechnology: RadioTechnology,
    val registered: Boolean,
    val connectionStatus: CellConnectionStatus = CellConnectionStatus.UNKNOWN,
    val subscriptionId: Int? = null,
    val slotIndex: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val cellId: String? = null,
    val pci: String? = null,
    val tacOrLac: String? = null,
    val channelNumber: String? = null,
    val band: String? = null,
    val bandwidth: String? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val dbm: Int? = null,
    val observedAtMillis: Long = System.currentTimeMillis(),
    val observedAtElapsedMillis: Long? = null,
    val isStale: Boolean = false,
    val measurementSource: String = "未知来源",
)

data class SimCellSnapshot(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
    val servingCell: CellSignal? = null,
    val neighbors: List<CellSignal> = emptyList(),
    val errorMessage: String? = null,
    val serviceState: String? = null,
    val dataConnectionState: String? = null,
)

data class SimCellMatch(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
    val servingCell: CellSignal? = null,
    val servingSector: SectorRecord? = null,
    val neighbors: List<CellSignal> = emptyList(),
    val neighborMatches: List<SectorRecord?> = emptyList(),
    val neighborSectors: List<SectorRecord> = emptyList(),
    val matchDiagnostic: MatchDiagnostic = MatchDiagnostic(),
)

enum class MapDisplayMode {
    Overlay,
    Split,
}

data class CellSnapshot(
    val simCards: List<SimCellSnapshot> = emptyList(),
    val collectedAtMillis: Long = System.currentTimeMillis(),
    val permissionMissing: Boolean = false,
    val airplaneModeOn: Boolean? = null,
    val errorMessage: String? = null,
) {
    val servingCell: CellSignal?
        get() = simCards.firstNotNullOfOrNull { it.servingCell }

    val neighbors: List<CellSignal>
        get() = simCards.flatMap { it.neighbors }

    companion object {
        val Empty = CellSnapshot()
    }
}
