package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchDiagnostic(
    val method: String = "未知",
    val candidateCount: Int = 0,
    val message: String = "旧记录未包含匹配诊断",
)

@Serializable
data class TestNetworkEvent(
    val timestampMillis: Long,
    val slotIndex: Int? = null,
    val title: String,
    val detail: String,
    val evidence: String,
)
