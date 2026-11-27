package com.xywy.signalmap.ui.cell

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkStatusToneTest {
    @Test
    fun `only registered service is shown as healthy`() {
        assertEquals(NetworkStatusTone.SUCCESS, serviceStateTone("已注册"))
        listOf("未注册", "仅紧急呼叫", "无线电关闭").forEach {
            assertEquals(NetworkStatusTone.WARNING, serviceStateTone(it))
        }
        listOf(null, "未知", "注册状态未知").forEach {
            assertEquals(NetworkStatusTone.NEUTRAL, serviceStateTone(it))
        }
    }

    @Test
    fun `connecting disconnected and suspended data are not shown as connected`() {
        assertEquals(NetworkStatusTone.SUCCESS, dataConnectionTone("已连接"))
        listOf("连接中", "未连接", "已断开", "已挂起").forEach {
            assertEquals(NetworkStatusTone.WARNING, dataConnectionTone(it))
        }
        listOf(null, "未知（-1）", "连接状态未知").forEach {
            assertEquals(NetworkStatusTone.NEUTRAL, dataConnectionTone(it))
        }
    }
}
