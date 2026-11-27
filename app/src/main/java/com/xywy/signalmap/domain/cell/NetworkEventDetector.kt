package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*

object NetworkEventDetector {
    const val GAP_MILLIS = 5_000L
    fun detect(previous: TestLogSample?, current: TestLogSample): List<TestNetworkEvent> = buildList {
        if (previous == null) return@buildList
        fun event(slot: Int?, title: String, detail: String, evidence: String = "参数变化推断") {
            add(TestNetworkEvent(current.timestampMillis, slot, title, detail, evidence))
        }
        if (current.timestampMillis - previous.timestampMillis > GAP_MILLIS || current.timestampMillis < previous.timestampMillis) {
            event(null, "采样不连续", "前后采样间隔异常；不据此判断切换、掉网或持续覆盖", "采样时间")
            return@buildList
        }
        if (previous.airplaneModeOn != null && current.airplaneModeOn != null && previous.airplaneModeOn != current.airplaneModeOn) {
            event(null, if (current.airplaneModeOn) "飞行模式开启" else "飞行模式关闭", "Android 系统飞行模式状态变化", "系统状态")
        }
        current.sims.forEach { sim ->
            val old = previous.sims.firstOrNull { it.slotIndex == sim.slotIndex && it.subscriptionId == sim.subscriptionId }
                ?: return@forEach
            val slot = sim.slotIndex
            if (old.serviceState != null && sim.serviceState != null && old.serviceState != sim.serviceState)
                event(slot, "注册状态变化", "${old.serviceState} → ${sim.serviceState}（ServiceState，不等同于数据业务可用性）", "系统状态")
            if (old.dataConnectionState != null && sim.dataConnectionState != null && old.dataConnectionState != sim.dataConnectionState)
                event(slot, "数据连接变化", "${old.dataConnectionState} → ${sim.dataConnectionState}；连接状态不代表互联网可达", "系统状态")
            val before = old.servingCell
            val after = sim.servingCell
            if ((before == null) != (after == null)) {
                event(slot, if (after == null) "服务小区信息消失" else "服务小区信息恢复", "仅表示接口测量可用性变化，不直接判定掉网或恢复")
            }
            if (before != null && after != null) {
                if (before.isStale != after.isStale) event(slot, if (after.isStale) "信号测量过期" else "信号测量恢复", "按测量时间戳判断，不能仅凭数值不变判断异常", "采样诊断")
                if (before.isStale || after.isStale) return@forEach
                if (before.radioTechnology != after.radioTechnology) {
                    event(slot, "观测制式变化", "${before.radioTechnology.label} → ${after.radioTechnology.label}；可能是主辅小区显示对象变化")
                } else if (!before.cellId.isNullOrBlank() && !after.cellId.isNullOrBlank() && before.cellId != after.cellId) {
                    event(slot, "服务小区标识变化", "${before.cellId} → ${after.cellId}；不代表已确认的切换成功")
                }
                if (before.channelNumber != null && after.channelNumber != null && before.channelNumber != after.channelNumber)
                    event(slot, "观测频点变化", "${before.channelNumber} → ${after.channelNumber}")
            }
            if (old.errorMessage != sim.errorMessage && sim.errorMessage != null)
                event(slot, "采集提示", sim.errorMessage, "采样诊断")
        }
    }
}

data class LiveNetworkEvents(
    val previous: TestLogSample? = null,
    val events: List<TestNetworkEvent> = emptyList(),
) {
    fun append(sample: TestLogSample): LiveNetworkEvents = LiveNetworkEvents(sample,
        (events + NetworkEventDetector.detect(previous, sample)).takeLast(100))
}
