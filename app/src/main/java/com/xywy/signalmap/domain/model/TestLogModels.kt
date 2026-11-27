package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TestMetricMode {
    RSRP,
    SINR,
}

@Serializable
enum class TestLogOrigin {
    RECORDED,
    IMPORTED,
}

@Serializable
data class TestLogMetadata(
    val id: String,
    val name: String,
    val sourceFileName: String? = null,
    val importedAtMillis: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val sampleCount: Int,
    val isVisible: Boolean = true,
    val defaultMetricMode: TestMetricMode = TestMetricMode.RSRP,
    val origin: TestLogOrigin = TestLogOrigin.RECORDED,
)

@Serializable
data class TestLogPoint(
    val longitude: Double,
    val latitude: Double,
    val accuracyMeters: Float? = null,
)

@Serializable
data class TestLogCellRecord(
    val radioTechnology: RadioTechnology,
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
    val matchedSectorName: String? = null,
    val matchedSiteName: String? = null,
    val matchedSiteId: String? = null,
    val observedAtMillis: Long? = null,
    val isStale: Boolean = false,
    val measurementSource: String? = null,
    val measurementAgeMillis: Long? = null,
)

@Serializable
data class TestLogSimRecord(
    val subscriptionId: Int? = null,
    val slotIndex: Int,
    val label: String,
    val servingCell: TestLogCellRecord? = null,
    val neighbors: List<TestLogCellRecord> = emptyList(),
    val errorMessage: String? = null,
    val serviceState: String? = null,
    val dataConnectionState: String? = null,
    val matchDiagnostic: MatchDiagnostic? = null,
)

@Serializable
data class TestLogSample(
    val timestampMillis: Long,
    val point: TestLogPoint? = null,
    val primaryMetricSource: String? = null,
    val primaryRsrp: Int? = null,
    val primarySinr: Int? = null,
    val sims: List<TestLogSimRecord> = emptyList(),
    val airplaneModeOn: Boolean? = null,
    val diagnosticMessage: String? = null,
    val events: List<TestNetworkEvent> = emptyList(),
)

@Serializable
data class TestLogDocument(
    val meta: TestLogMetadata,
    val samples: List<TestLogSample>,
)
