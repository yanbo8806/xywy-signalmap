package com.xywy.signalmap.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.TestLogCellRecord
import com.xywy.signalmap.domain.model.TestLogPoint
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestLogSimRecord
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.model.TilePresetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestLogMapRenderPayloadFactoryTest {
    @Test
    fun `turning both SIM test point switches off yields no points`() {
        val sample = TestLogSample(
            timestampMillis = 1000, point = TestLogPoint(120.8, 34.4),
            sims = listOf(
                TestLogSimRecord(slotIndex = 0, label = "SIM1", servingCell = TestLogCellRecord(RadioTechnology.LTE, rsrp = -95)),
                TestLogSimRecord(slotIndex = 1, label = "SIM2", servingCell = TestLogCellRecord(RadioTechnology.NR, rsrp = -85)),
            ),
        )
        val payload = TestLogMapRenderPayloadFactory.create(
            listOf(sample), TestMetricMode.RSRP, emptySet(), TilePresetId.OPEN_STREET_MAP,
        )
        assertTrue(payload.features.features().orEmpty().isEmpty())
    }

    @Test
    fun `default map rendering retains every test sample beyond the former cap`() {
        val samples = (0 until 13_000).map { index ->
            TestLogSample(
                timestampMillis = index.toLong(),
                point = TestLogPoint(120.0 + index * .0001, 34.4),
                primaryRsrp = -95,
            )
        }
        val payload = TestLogMapRenderPayloadFactory.create(
            samples, TestMetricMode.RSRP, emptySet(), TilePresetId.OPEN_STREET_MAP,
        )
        assertEquals(13_000, payload.features.features().orEmpty().size)
        assertEquals(samples.map { it.timestampMillis }, payload.features.features().orEmpty().map { it.getNumberProperty("timestampMillis").toLong() })
    }

    @Test
    fun `payload factory filters visible slots before generating features`() {
        val sample = TestLogSample(
            timestampMillis = 1_000L,
            point = TestLogPoint(longitude = 120.8, latitude = 34.4),
            sims = listOf(
                TestLogSimRecord(
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.LTE,
                        rsrp = -95,
                    ),
                ),
                TestLogSimRecord(
                    slotIndex = 1,
                    label = "SIM2",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.NR,
                        rsrp = -85,
                    ),
                ),
            ),
        )

        val payload = TestLogMapRenderPayloadFactory.create(
            samples = listOf(sample),
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(1),
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
        )

        val features = payload.features.features().orEmpty()
        assertEquals(1, features.size)
        assertEquals(1, features.single().getNumberProperty("slotIndex").toInt())
    }

    @Test
    fun `payload signature changes when relevant render inputs change`() {
        val sample = TestLogSample(
            timestampMillis = 1_000L,
            point = TestLogPoint(longitude = 120.8, latitude = 34.4),
            sims = listOf(
                TestLogSimRecord(
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.LTE,
                        rsrp = -100,
                        sinr = 12,
                    ),
                ),
            ),
        )

        val rsrpPayload = TestLogMapRenderPayloadFactory.create(
            samples = listOf(sample),
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(0),
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
        )
        val sinrPayload = TestLogMapRenderPayloadFactory.create(
            samples = listOf(sample),
            metricMode = TestMetricMode.SINR,
            visibleSlotIndexes = setOf(0),
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
        )

        assertNotEquals(rsrpPayload.signature, sinrPayload.signature)
    }

    @Test
    fun `payload factory decimates large sample lists`() {
        val samples = (0 until 5_000).map { index ->
            TestLogSample(
                timestampMillis = index.toLong(),
                point = TestLogPoint(longitude = 120.8 + index * 0.00001, latitude = 34.4),
                sims = listOf(
                    TestLogSimRecord(
                        slotIndex = 0,
                        label = "SIM1",
                        servingCell = TestLogCellRecord(
                            radioTechnology = RadioTechnology.LTE,
                            rsrp = -95,
                        ),
                    ),
                    TestLogSimRecord(
                        slotIndex = 1,
                        label = "SIM2",
                        servingCell = TestLogCellRecord(
                            radioTechnology = RadioTechnology.NR,
                            rsrp = -85,
                        ),
                    ),
                ),
            )
        }

        val payload = TestLogMapRenderPayloadFactory.create(
            samples = samples,
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(0, 1),
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
            maxSamples = 1_000,
        )

        val features = payload.features.features().orEmpty()
        assertEquals(2_000, features.size)
        assertTrue(payload.signature.contains("5000"))
        assertTrue(payload.signature.contains("1000"))
    }

    @Test
    fun `payload factory keeps latest sample when decimated list reaches max size`() {
        val samples = (0 until 5).map { index ->
            TestLogSample(
                timestampMillis = index.toLong(),
                point = TestLogPoint(longitude = 120.8 + index * 0.00001, latitude = 34.4),
                sims = listOf(
                    TestLogSimRecord(
                        slotIndex = 0,
                        label = "SIM1",
                        servingCell = TestLogCellRecord(
                            radioTechnology = RadioTechnology.LTE,
                            rsrp = -95 + index,
                        ),
                    ),
                ),
            )
        }

        val payload = TestLogMapRenderPayloadFactory.create(
            samples = samples,
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(0),
            tilePresetId = TilePresetId.OPEN_STREET_MAP,
            maxSamples = 2,
        )

        val features = payload.features.features().orEmpty()
        assertEquals(2, features.size)
        assertEquals(4L, features.last().getNumberProperty("timestampMillis").toLong())
    }
}
