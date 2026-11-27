package com.xywy.signalmap.ui.cell

internal enum class NetworkStatusTone { SUCCESS, WARNING, NEUTRAL }

internal fun serviceStateTone(state: String?): NetworkStatusTone = when (state) {
    "已注册" -> NetworkStatusTone.SUCCESS
    "未注册", "仅紧急呼叫", "无线电关闭" -> NetworkStatusTone.WARNING
    else -> NetworkStatusTone.NEUTRAL
}

internal fun dataConnectionTone(state: String?): NetworkStatusTone = when (state) {
    "已连接" -> NetworkStatusTone.SUCCESS
    "已断开", "未连接", "连接中", "已挂起" -> NetworkStatusTone.WARNING
    else -> NetworkStatusTone.NEUTRAL
}
