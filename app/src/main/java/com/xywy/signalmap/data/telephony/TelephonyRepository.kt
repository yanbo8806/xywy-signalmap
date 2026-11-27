@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
@file:android.annotation.SuppressLint("MissingPermission", "NewApi")

package com.xywy.signalmap.data.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.*
import androidx.core.content.ContextCompat
import com.xywy.signalmap.domain.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.util.concurrent.Executor

class TelephonyRepository(private val context: Context) {
    private val observationStore = TelephonyObservationStore()
    private val registrations = linkedMapOf<String, Registration>()
    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbackExecutor = Executor { mainHandler.post(it) }
    private var radioEpochMillis = 0L
    private val radioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            synchronized(lock) {
                // Invalidate pending requests and callbacks from the previous radio session.
                radioEpochMillis = SystemClock.elapsedRealtime()
                clearRegistrations()
            }
        }
    }

    init {
        ContextCompat.registerReceiver(context, radioReceiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
    }

    fun observe(): Flow<CellSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(readSnapshotNow())
            delay(TelephonyRefreshPolicy.snapshotPollIntervalMillis)
        }
    }

    /** Polling and recording share one throttled, non-blocking request per subscription. */
    fun readSnapshotNow(): CellSnapshot = synchronized(lock) {
        if (airplaneModeOn()) {
            clearRegistrations()
            return@synchronized CellSnapshot(errorMessage = "飞行模式已开启，等待恢复移动网络", airplaneModeOn = true)
        }
        val baseManager = context.getSystemService(TelephonyManager::class.java)
            ?: return@synchronized CellSnapshot(errorMessage = "无法读取电话服务")
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val subscriptions = runCatching { subscriptionManager?.activeSubscriptionInfoList.orEmpty() }
            .getOrDefault(emptyList()).distinctBy { it.subscriptionId }.sortedBy { it.simSlotIndex }
        val observed = subscriptions.map {
            TelephonyObservedSubscription(it.subscriptionId, it.simSlotIndex,
                "SIM${it.simSlotIndex + 1} ${it.displayName ?: ""}".trim())
        }.ifEmpty { listOf(TelephonyObservedSubscription(-1, 0, "SIM1")) }
        observationStore.refreshSubscriptions(observed)
        val desired = observed.map(::subscriptionKey).toSet()
        (registrations.keys - desired).forEach { registrations.remove(it)?.stop() }
        observed.forEach { subscription ->
            val key = subscriptionKey(subscription)
            val registration = registrations.getOrPut(key) {
                Registration(
                    if (subscription.subscriptionId >= 0) baseManager.createForSubscriptionId(subscription.subscriptionId) else baseManager,
                    subscription,
                ).also { it.start() }
            }
            registration.refresh()
        }
        val snapshot = observationStore.snapshot(elapsedRealtimeMillis = SystemClock.elapsedRealtime()).copy(airplaneModeOn = false)
        if (snapshot.simCards.all { it.servingCell == null && it.neighbors.isEmpty() }) {
            snapshot.copy(errorMessage = "正在等待手机上报小区信息")
        } else snapshot
    }

    fun close() {
        synchronized(lock) { clearRegistrations() }
        runCatching { context.unregisterReceiver(radioReceiver) }
    }

    private fun airplaneModeOn(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    private fun clearRegistrations() {
        registrations.values.forEach { it.stop() }
        registrations.clear()
        observationStore.refreshSubscriptions(emptyList())
    }

    private fun toCellSignal(
        cellInfo: CellInfo,
        subscriptionId: Int,
        slotIndex: Int,
    ): CellSignal? = when (cellInfo) {
        is CellInfoLte -> {
            val identity = cellInfo.cellIdentity
            CellSignal(
                radioTechnology = RadioTechnology.LTE,
                registered = cellInfo.isRegistered,
                connectionStatus = cellInfo.toConnectionStatus(),
                subscriptionId = subscriptionId,
                slotIndex = slotIndex,
                mcc = identity.mccString.cleanString(),
                mnc = identity.mncString.cleanString(),
                cellId = identity.ci.validIntString(),
                pci = identity.pci.validIntString(),
                tacOrLac = identity.tac.validIntString(),
                channelNumber = identity.earfcn.validIntString(),
                band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) identity.bands.firstOrNull()?.let { "B$it" } else null,
                bandwidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) identity.bandwidth.toBandwidthLabel() else null,
                rsrp = SignalValueNormalizer.lteRsrp(cellInfo.cellSignalStrength.rsrp),
                rsrq = SignalValueNormalizer.lteRsrq(cellInfo.cellSignalStrength.rsrq),
                sinr = SignalValueNormalizer.lteSinr(cellInfo.cellSignalStrength.rssnr, Build.VERSION.SDK_INT),
                dbm = SignalValueNormalizer.lteRsrp(cellInfo.cellSignalStrength.dbm),
            )
        }

        is CellInfoNr -> {
            val identity = cellInfo.cellIdentity as? CellIdentityNr ?: return null
            val signal = cellInfo.cellSignalStrength as? CellSignalStrengthNr ?: return null
            CellSignal(
                radioTechnology = RadioTechnology.NR,
                registered = cellInfo.isRegistered,
                connectionStatus = cellInfo.toConnectionStatus(),
                subscriptionId = subscriptionId,
                slotIndex = slotIndex,
                mcc = identity.mccString.cleanString(),
                mnc = identity.mncString.cleanString(),
                cellId = identity.nci.validLongString(),
                pci = identity.pci.validIntString(),
                tacOrLac = identity.tac.validIntString(),
                channelNumber = identity.nrarfcn.validIntString(),
                band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) identity.bands.firstOrNull()?.let { "N$it" } else null,
                rsrp = SignalValueNormalizer.nrRsrp(signal.ssRsrp),
                rsrq = SignalValueNormalizer.nrRsrq(signal.ssRsrq),
                sinr = SignalValueNormalizer.nrSinr(signal.ssSinr),
                dbm = SignalValueNormalizer.nrRsrp(signal.dbm),
            )
        }

        is CellInfoWcdma -> CellSignal(
            radioTechnology = RadioTechnology.WCDMA,
            registered = cellInfo.isRegistered,
            connectionStatus = cellInfo.toConnectionStatus(),
            subscriptionId = subscriptionId,
            slotIndex = slotIndex,
            mcc = cellInfo.cellIdentity.mccString.cleanString(),
            mnc = cellInfo.cellIdentity.mncString.cleanString(),
            cellId = cellInfo.cellIdentity.cid.validIntString(),
            pci = cellInfo.cellIdentity.psc.validIntString(),
            tacOrLac = cellInfo.cellIdentity.lac.validIntString(),
            channelNumber = cellInfo.cellIdentity.uarfcn.validIntString(),
            rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cellInfo.cellSignalStrength.ecNo.validSignalValue() else null,
            dbm = cellInfo.cellSignalStrength.dbm.validSignalValue(),
        )

        is CellInfoGsm -> CellSignal(
            radioTechnology = RadioTechnology.GSM,
            registered = cellInfo.isRegistered,
            connectionStatus = cellInfo.toConnectionStatus(),
            subscriptionId = subscriptionId,
            slotIndex = slotIndex,
            mcc = cellInfo.cellIdentity.mccString.cleanString(),
            mnc = cellInfo.cellIdentity.mncString.cleanString(),
            cellId = cellInfo.cellIdentity.cid.validIntString(),
            pci = cellInfo.cellIdentity.bsic.validIntString(),
            tacOrLac = cellInfo.cellIdentity.lac.validIntString(),
            channelNumber = cellInfo.cellIdentity.arfcn.validIntString(),
            dbm = cellInfo.cellSignalStrength.dbm.validSignalValue(),
        )

        is CellInfoCdma -> CellSignal(
            radioTechnology = RadioTechnology.CDMA,
            registered = cellInfo.isRegistered,
            connectionStatus = cellInfo.toConnectionStatus(),
            subscriptionId = subscriptionId,
            slotIndex = slotIndex,
            cellId = cellInfo.cellIdentity.basestationId.validIntString(),
            pci = cellInfo.cellIdentity.networkId.validIntString(),
            tacOrLac = cellInfo.cellIdentity.systemId.validIntString(),
            dbm = cellInfo.cellSignalStrength.dbm.validSignalValue(),
        )

        else -> null
    }

    private fun subscriptionKey(subscription: TelephonyObservedSubscription): String =
        "${subscription.subscriptionId}:${subscription.slotIndex}"

    private fun Int.validIntString(): String? =
        takeUnless { it == Int.MAX_VALUE || it == Int.MIN_VALUE || it < 0 }?.toString()

    private fun Long.validLongString(): String? =
        takeUnless { it == Long.MAX_VALUE || it == Long.MIN_VALUE || it < 0 }?.toString()

    private fun String?.cleanString(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }

    private fun Int.validSignalValue(): Int? =
        takeUnless { it == Int.MAX_VALUE || it == Int.MIN_VALUE }

    private fun Int.toBandwidthLabel(): String? =
        takeUnless { it == Int.MAX_VALUE || it <= 0 }?.let { "${it / 1000.0} MHz" }

    private fun CellInfo.toConnectionStatus(): CellConnectionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return if (isRegistered) CellConnectionStatus.UNKNOWN else CellConnectionStatus.NONE
        }
        return when (cellConnectionStatus) {
            CellInfo.CONNECTION_PRIMARY_SERVING -> CellConnectionStatus.PRIMARY_SERVING
            CellInfo.CONNECTION_SECONDARY_SERVING -> CellConnectionStatus.SECONDARY_SERVING
            CellInfo.CONNECTION_NONE -> CellConnectionStatus.NONE
            else -> if (isRegistered) CellConnectionStatus.UNKNOWN else CellConnectionStatus.NONE
        }
    }

    private inner class Registration(
        val manager: TelephonyManager,
        val subscription: TelephonyObservedSubscription,
    ) {
        private var active = true
        private val requests = TelephonyRequestGate()
        private var minimumMeasurementAt = radioEpochMillis
        private var radioOff = false
        private var callback: TelephonyCallback? = null
        private var listener: PhoneStateListener? = null

        fun start() {
            // Legacy PhoneStateListener requires a Looper. All listener work uses the main queue.
            mainHandler.post {
                synchronized(lock) {
                    if (!active) return@synchronized
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val registered = SubscriptionCallback(this)
                            manager.registerTelephonyCallback(callbackExecutor, registered)
                            callback = registered
                        } else {
                            val registered = SubscriptionListener(this)
                            manager.listen(registered, PhoneStateListener.LISTEN_CELL_INFO or
                                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE or
                                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE)
                            listener = registered
                        }
                    }.onFailure { setError("手机信号监听不可用，将继续主动刷新") }
                }
            }
        }

        fun stop() {
            active = false
            requests.reset()
            callback?.let { runCatching { manager.unregisterTelephonyCallback(it) } }
            listener?.let { runCatching { manager.listen(it, PhoneStateListener.LISTEN_NONE) } }
        }

        fun refresh() {
            if (!active || radioOff) return
            val now = SystemClock.elapsedRealtime()
            if (requests.timedOut(now)) setError("小区刷新超时，正在重试")
            val serial = requests.begin(now) ?: return
            // allCellInfo may be cached; never relabel its measurement timestamp as new.
            if (!observationStore.hasCells(subscription.subscriptionId, subscription.slotIndex)) readCachedCells()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                readCachedCells()
                requests.complete(serial)
                return
            }
            runCatching {
                manager.requestCellInfoUpdate(callbackExecutor, object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: List<CellInfo>) {
                        synchronized(lock) {
                            if (!active || !requests.complete(serial)) return@synchronized
                            acceptCells(cellInfo, "主动刷新返回")
                        }
                    }
                    override fun onError(errorCode: Int, detail: Throwable?) {
                        synchronized(lock) {
                            if (!active || !requests.complete(serial)) return@synchronized
                            readCachedCells()
                            setError("手机未完成小区刷新（" + errorCode + "），将自动重试")
                        }
                    }
                })
            }.onFailure {
                requests.complete(serial)
                readCachedCells()
                setError("小区刷新失败，将自动重试")
            }
        }

        private fun readCachedCells() {
            runCatching { manager.allCellInfo }.onSuccess { cells ->
                if (!cells.isNullOrEmpty()) acceptCells(cells, "系统缓存读取")
            }
        }

        fun acceptCells(cells: List<CellInfo>, source: String = "CellInfo 回调") {
            if (!active || radioOff || airplaneModeOn()) return
            val nowElapsed = SystemClock.elapsedRealtime()
            val nowWall = System.currentTimeMillis()
            val mapped = cells.mapNotNull { cell ->
                val measured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cell.timestampMillis else cell.timeStamp / 1_000_000L
                // Discard delayed results from before flight mode or radio power cycling.
                if (measured < minimumMeasurementAt) return@mapNotNull null
                toCellSignal(cell, subscription.subscriptionId, subscription.slotIndex)?.copy(
                    observedAtElapsedMillis = measured.takeIf { it in 1..nowElapsed },
                    observedAtMillis = if (measured in 1..nowElapsed) nowWall - (nowElapsed - measured) else 0L,
                    measurementSource = source,
                )
            }
            if (cells.isNotEmpty() && mapped.isEmpty()) return
            observationStore.updateCells(subscription.subscriptionId, subscription.slotIndex, subscription.label, mapped,
                if (cells.isEmpty()) "当前订阅无小区信息" else null)
        }

        fun acceptSignal(signal: SignalStrength) {
            if (!active || radioOff || airplaneModeOn()) return
            val now = SystemClock.elapsedRealtime()
            val measured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) signal.timestampMillis else now
            if (measured < minimumMeasurementAt || !TelephonyRefreshPolicy.isFresh(measured, now)) return
            ServingSignalUpdate.extract(signal).forEach { update ->
                observationStore.updateServingSignal(subscription.subscriptionId, subscription.slotIndex, update, measured,
                    System.currentTimeMillis() - (now - measured))
            }
            // Signal callbacks carry no cell identity. Refresh identity separately.
            refresh()
        }

        fun acceptService(state: ServiceState) {
            if (!active) return
            observationStore.updateNetworkState(subscription.subscriptionId, subscription.slotIndex, service = when (state.state) {
                ServiceState.STATE_IN_SERVICE -> "已注册"
                ServiceState.STATE_OUT_OF_SERVICE -> "未注册"
                ServiceState.STATE_EMERGENCY_ONLY -> "仅紧急呼叫"
                ServiceState.STATE_POWER_OFF -> "无线电关闭"
                else -> "未知"
            })
            if (state.state == ServiceState.STATE_POWER_OFF) {
                radioOff = true
                minimumMeasurementAt = SystemClock.elapsedRealtime()
                requests.reset()
                observationStore.clearCells(subscription.subscriptionId, subscription.slotIndex, "移动网络无线电已关闭")
            } else if (radioOff) {
                radioOff = false
                minimumMeasurementAt = SystemClock.elapsedRealtime()
                requests.reset()
                refresh()
            }
        }

        private fun setError(message: String) = observationStore.setError(subscription.subscriptionId, subscription.slotIndex, message)

        fun acceptData(state: Int) {
            if (!active) return
            observationStore.updateNetworkState(subscription.subscriptionId, subscription.slotIndex, data = when (state) {
                TelephonyManager.DATA_CONNECTED -> "已连接"
                TelephonyManager.DATA_DISCONNECTED -> "已断开"
                TelephonyManager.DATA_CONNECTING -> "连接中"
                TelephonyManager.DATA_SUSPENDED -> "已挂起"
                else -> "未知（$state）"
            })
        }
    }

    private inner class SubscriptionCallback(private val registration: Registration) : TelephonyCallback(),
        TelephonyCallback.CellInfoListener, TelephonyCallback.SignalStrengthsListener, TelephonyCallback.ServiceStateListener,
        TelephonyCallback.DataConnectionStateListener {
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) = synchronized(lock) { registration.acceptData(state) }
        override fun onCellInfoChanged(cellInfo: List<CellInfo>) = synchronized(lock) { registration.acceptCells(cellInfo) }
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) = synchronized(lock) { registration.acceptSignal(signalStrength) }
        override fun onServiceStateChanged(serviceState: ServiceState) = synchronized(lock) { registration.acceptService(serviceState) }
    }

    private inner class SubscriptionListener(private val registration: Registration) : PhoneStateListener() {
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) = synchronized(lock) { registration.acceptData(state) }
        override fun onCellInfoChanged(cellInfo: List<CellInfo>?) = synchronized(lock) { registration.acceptCells(cellInfo.orEmpty()) }
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            signalStrength?.let { synchronized(lock) { registration.acceptSignal(it) } }
        }
        override fun onServiceStateChanged(serviceState: ServiceState?) {
            serviceState?.let { synchronized(lock) { registration.acceptService(it) } }
        }
    }
}
