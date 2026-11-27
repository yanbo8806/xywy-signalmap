package com.xywy.signalmap.data.telephony

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.CellSnapshot
import com.xywy.signalmap.domain.model.SimCellSnapshot

data class TelephonyObservedSubscription(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
)

class TelephonyObservationStore {
    private val subscriptions = linkedMapOf<String, TelephonyObservedSubscription>()
    private val observations = linkedMapOf<String, TelephonyObservedCells>()

    @Synchronized
    fun refreshSubscriptions(latestSubscriptions: List<TelephonyObservedSubscription>) {
        val latestKeys = latestSubscriptions.map(::subscriptionKey).toSet()
        subscriptions.keys.retainAll(latestKeys)
        observations.keys.retainAll(latestKeys)

        latestSubscriptions
            .sortedBy { it.slotIndex }
            .forEach { subscription ->
                subscriptions[subscriptionKey(subscription)] = subscription
            }
    }

    @Synchronized
    fun updateCells(
        subscriptionId: Int,
        slotIndex: Int,
        label: String,
        cells: List<CellSignal>,
        errorMessage: String? = null,
    ) {
        val key = subscriptionKey(subscriptionId, slotIndex)
        if (key !in subscriptions) return
        val previous = observations[key]
        val measuredAt = cells.mapNotNull { it.observedAtElapsedMillis }.maxOrNull()
        if (measuredAt != null && previous?.cellInfoTimestamp != null && measuredAt < previous.cellInfoTimestamp) return
        val merged = cells.map { cell ->
            val cached = previous?.cells?.firstOrNull { it.sameIdentity(cell) }
            if (cached?.observedAtElapsedMillis != null && cell.observedAtElapsedMillis != null &&
                cached.observedAtElapsedMillis > cell.observedAtElapsedMillis) {
                cell.copy(rsrp = cached.rsrp, rsrq = cached.rsrq, sinr = cached.sinr, dbm = cached.dbm,
                    observedAtMillis = cached.observedAtMillis, observedAtElapsedMillis = cached.observedAtElapsedMillis,
                    measurementSource = cached.measurementSource)
            } else cell
        }
        observations[key] = TelephonyObservedCells(
            cells = merged,
            errorMessage = errorMessage,
            cellInfoTimestamp = measuredAt,
            serviceState = previous?.serviceState,
            dataConnectionState = previous?.dataConnectionState,
        )
    }

    @Synchronized
    fun clearCells(subscriptionId: Int, slotIndex: Int, message: String) {
        val key = subscriptionKey(subscriptionId, slotIndex)
        if (key in subscriptions) observations[key] = (observations[key] ?: TelephonyObservedCells(emptyList(), null))
            .copy(cells = emptyList(), errorMessage = message, cellInfoTimestamp = null)
    }

    @Synchronized
    fun updateNetworkState(subscriptionId: Int, slotIndex: Int, service: String? = null, data: String? = null) {
        val key = subscriptionKey(subscriptionId, slotIndex)
        if (key !in subscriptions) return
        val previous = observations[key] ?: TelephonyObservedCells(emptyList(), null)
        observations[key] = previous.copy(serviceState = service ?: previous.serviceState,
            dataConnectionState = data ?: previous.dataConnectionState)
    }

    @Synchronized
    fun setError(subscriptionId: Int, slotIndex: Int, message: String) {
        val key = subscriptionKey(subscriptionId, slotIndex)
        if (key in subscriptions) {
            observations[key] = (observations[key] ?: TelephonyObservedCells(emptyList(), null)).copy(errorMessage = message)
        }
    }

    @Synchronized
    internal fun updateServingSignal(
        subscriptionId: Int,
        slotIndex: Int,
        update: ServingSignalUpdate,
        measuredAtElapsedMillis: Long,
        observedAtMillis: Long,
    ): Boolean {
        val key = subscriptionKey(subscriptionId, slotIndex)
        val observed = observations[key] ?: return false
        val serving = TelephonyCellSelection.selectServing(observed.cells) ?: return false
        if (serving.radioTechnology != update.technology ||
            measuredAtElapsedMillis < (serving.observedAtElapsedMillis ?: 0)) return false
        val updated = serving.copy(
            rsrp = update.rsrp, rsrq = update.rsrq, sinr = update.sinr, dbm = update.dbm,
            observedAtElapsedMillis = measuredAtElapsedMillis, observedAtMillis = observedAtMillis,
            measurementSource = "SignalStrength 回调",
        )
        observations[key] = observed.copy(cells = observed.cells.map { if (it === serving) updated else it })
        return true
    }

    @Synchronized
    fun hasCells(
        subscriptionId: Int,
        slotIndex: Int,
    ): Boolean = observations[subscriptionKey(subscriptionId, slotIndex)]?.cells?.isNotEmpty() == true

    @Synchronized
    fun snapshot(
        collectedAtMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long? = null,
    ): CellSnapshot {
        val simCards = subscriptions.values
            .sortedBy { it.slotIndex }
            .map { subscription ->
                val observed = observations[subscriptionKey(subscription)]
                val cells = observed?.cells.orEmpty().map { cell ->
                    if (elapsedRealtimeMillis != null && !TelephonyRefreshPolicy.isFresh(cell.observedAtElapsedMillis, elapsedRealtimeMillis)) {
                        cell.copy(rsrp = null, rsrq = null, sinr = null, dbm = null, isStale = true)
                    } else cell
                }
                val serving = TelephonyCellSelection.selectServing(cells)
                SimCellSnapshot(
                    subscriptionId = subscription.subscriptionId,
                    slotIndex = subscription.slotIndex,
                    label = subscription.label,
                    servingCell = serving,
                    neighbors = TelephonyCellSelection.selectNeighbors(cells, serving),
                    errorMessage = if (serving?.isStale == true) "手机未上报新测量，信号已过期" else observed?.errorMessage,
                    serviceState = observed?.serviceState,
                    dataConnectionState = observed?.dataConnectionState,
                )
            }
        return CellSnapshot(
            simCards = simCards,
            collectedAtMillis = collectedAtMillis,
        )
    }

    private fun subscriptionKey(subscription: TelephonyObservedSubscription): String =
        subscriptionKey(subscription.subscriptionId, subscription.slotIndex)

    private fun subscriptionKey(subscriptionId: Int, slotIndex: Int): String =
        "$subscriptionId:$slotIndex"
}

private data class TelephonyObservedCells(
    val cells: List<CellSignal>,
    val errorMessage: String?,
    val cellInfoTimestamp: Long? = null,
    val serviceState: String? = null,
    val dataConnectionState: String? = null,
)

private fun CellSignal.sameIdentity(other: CellSignal): Boolean =
    radioTechnology == other.radioTechnology && cellId == other.cellId && pci == other.pci &&
        channelNumber == other.channelNumber && tacOrLac == other.tacOrLac &&
        registered == other.registered && connectionStatus == other.connectionStatus
