package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.TestLogCellRecord
import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogOrigin
import com.xywy.signalmap.domain.model.TestLogPoint
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestLogSimRecord
import com.xywy.signalmap.domain.model.TestMetricMode
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

class TestLogFileCodecTest {
    @Test
    fun `unknown archive entries do not alter the recorded session`() {
        val document = TestLogDocument(
            TestLogMetadata("minimal", "记录", importedAtMillis = 1, startedAtMillis = 1, endedAtMillis = 2, sampleCount = 0),
            emptyList(),
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, payload: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(payload)
                zip.closeEntry()
            }
            entry("ignored.bin", ByteArray(1024 * 1024))
            entry("manifest.json", """{"format":"xywy-test-log","version":1,"exportedAtMillis":0}""".toByteArray())
            entry("session.json", Json.encodeToString(TestLogDocument.serializer(), document).toByteArray())
        }
        assertEquals(document, TestLogFileCodec.read(output.toByteArray().inputStream()))
    }

    @Test
    fun `oversized manifest is rejected before json parsing`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(ByteArray(65_537) { ' '.code.toByte() })
            zip.closeEntry()
        }
        val error = assertFailsWith<IllegalArgumentException> { TestLogFileCodec.read(output.toByteArray().inputStream()) }
        assertTrue(error.message.orEmpty().contains("内容过大"))
    }

    @Test
    fun `xywy codec round trips log document`() {
        val document = TestLogDocument(
            meta = TestLogMetadata(
                id = "log-1",
                name = "测试路测",
                sourceFileName = "test-route.xywy",
                importedAtMillis = 1_713_676_800_000,
                startedAtMillis = 1_713_676_800_000,
                endedAtMillis = 1_713_676_860_000,
                sampleCount = 1,
                isVisible = true,
                defaultMetricMode = TestMetricMode.RSRP,
                origin = TestLogOrigin.RECORDED,
            ),
            samples = listOf(
                TestLogSample(
                    timestampMillis = 1_713_676_805_000,
                    point = TestLogPoint(
                        longitude = 120.9398,
                        latitude = 34.3416,
                        accuracyMeters = 5.5f,
                    ),
                    primaryMetricSource = "SIM1",
                    primaryRsrp = -97,
                    primarySinr = 16,
                    sims = listOf(
                        TestLogSimRecord(
                            subscriptionId = 1,
                            slotIndex = 0,
                            label = "SIM1 示例运营商",
                            servingCell = TestLogCellRecord(
                                radioTechnology = RadioTechnology.NR,
                                cellId = "49152",
                                pci = "704",
                                tacOrLac = "1234",
                                channelNumber = "504990",
                                band = "N41",
                                bandwidth = "100 MHz",
                                rsrp = -97,
                                rsrq = -11,
                                sinr = 16,
                                dbm = -87,
                                matchedSectorName = "示例地区湖-1",
                                matchedSiteName = "示例地区湖站",
                                matchedSiteId = "SITE-001",
                            ),
                            neighbors = listOf(
                                TestLogCellRecord(
                                    radioTechnology = RadioTechnology.LTE,
                                    cellId = "61706",
                                    pci = "10",
                                    tacOrLac = "1001",
                                    channelNumber = "40490",
                                    band = "B41",
                                    bandwidth = "20 MHz",
                                    rsrp = -108,
                                    rsrq = -15,
                                    sinr = 4,
                                    dbm = -96,
                                    matchedSectorName = "示例地区湖-L1",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val archive = createTempFile(suffix = ".xywy").toFile()

        TestLogFileCodec.write(archive, document)
        val decoded = TestLogFileCodec.read(archive)

        assertTrue(archive.length() > 0)
        assertEquals(document.meta, decoded.meta)
        assertEquals(document.samples.single().primaryRsrp, decoded.samples.single().primaryRsrp)
        assertEquals(document.samples.single().sims.single().servingCell?.matchedSectorName, decoded.samples.single().sims.single().servingCell?.matchedSectorName)
        assertEquals(document.samples.single().sims.single().neighbors.single().band, decoded.samples.single().sims.single().neighbors.single().band)
    }
}
