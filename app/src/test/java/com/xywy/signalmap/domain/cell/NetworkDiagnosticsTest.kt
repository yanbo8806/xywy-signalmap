package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.*
import com.xywy.signalmap.data.local.TestLogFileCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.Json
import kotlin.test.*

class NetworkDiagnosticsTest {
    @Test fun `legacy out of range metrics are excluded without guessing units or changing raw samples`() {
        val raw = sample(cell = cell().copy(rsrp = Int.MAX_VALUE, sinr = 184))
        val stat = TestLogAnalysis.create(listOf(raw)).statistics.single()
        assertEquals(0, stat.rsrpCount)
        assertEquals(0, stat.sinrCount)
        assertNull(stat.averageSinr)
        assertEquals(184, raw.sims.single().servingCell?.sinr)
    }
    @Test fun `sample captures monotonic age network context and match explanation`() {
        val cell = CellSignal(RadioTechnology.NR, true, rsrp = -90, observedAtMillis = 100,
            observedAtElapsedMillis = 5000, measurementSource = "主动刷新返回")
        val snapshot = CellSnapshot(simCards = listOf(SimCellSnapshot(1, 0, "SIM1", cell,
            serviceState = "已注册", dataConnectionState = "已连接")), airplaneModeOn = false)
        val matches = listOf(SimCellMatch(1, 0, "SIM1", cell, matchDiagnostic = MatchDiagnostic("PCI 推测", 2, "需要核实")))
        val captured = TestLogSampleFactory.create(999999, null, matches, snapshot, 8000)
        val sim = captured.sims.single()
        assertEquals(3000L, sim.servingCell?.measurementAgeMillis)
        assertEquals("主动刷新返回", sim.servingCell?.measurementSource)
        assertEquals("已连接", sim.dataConnectionState)
        assertEquals("PCI 推测", sim.matchDiagnostic?.method)
        assertEquals(false, captured.airplaneModeOn)
    }
    private fun cell(id: String = "1", tech: RadioTechnology = RadioTechnology.LTE, stale: Boolean = false) =
        TestLogCellRecord(tech, cellId = id, channelNumber = "100", rsrp = -115, sinr = -2, isStale = stale)
    private fun sample(time: Long = 1000, cell: TestLogCellRecord? = cell(), subscription: Int = 1,
                       service: String? = "已注册", data: String? = "已连接") = TestLogSample(time,
        sims = listOf(TestLogSimRecord(subscription, 0, "SIM1", cell, serviceState = service, dataConnectionState = data)))

    @Test fun `first observation and unchanged values do not invent events`() {
        assertTrue(NetworkEventDetector.detect(null, sample()).isEmpty())
        assertTrue(NetworkEventDetector.detect(sample(), sample(2000)).isEmpty())
    }
    @Test fun `cell change is inferred and never reported as handover success`() {
        val event = NetworkEventDetector.detect(sample(), sample(2000, cell("2"))).single()
        assertEquals("参数变化推断", event.evidence)
        assertTrue(event.detail.contains("不代表已确认"))
    }
    @Test fun `system service and data connection changes are distinguished from inference`() {
        val events = NetworkEventDetector.detect(sample(), sample(2000, service = "未注册", data = "已断开"))
        assertEquals(2, events.size)
        assertTrue(events.all { it.evidence == "系统状态" })
    }
    @Test fun `flight mode survives empty sim list without claiming radio handover`() {
        val events = NetworkEventDetector.detect(sample().copy(airplaneModeOn = false),
            TestLogSample(2000, airplaneModeOn = true))
        assertEquals("飞行模式开启", events.single().title)
    }
    @Test fun `missing measurements are not labelled as loss of coverage`() {
        val event = NetworkEventDetector.detect(sample(), sample(2000, cell = null)).single()
        assertEquals("服务小区信息消失", event.title)
        assertTrue(event.detail.contains("不直接判定掉网"))
    }
    @Test fun `stale identities and replacement subscriptions are not compared as handovers`() {
        val staleEvents = NetworkEventDetector.detect(sample(), sample(2000, cell("2", stale = true)))
        assertEquals(listOf("信号测量过期"), staleEvents.map { it.title })
        assertTrue(NetworkEventDetector.detect(sample(), sample(2000, cell("2"), subscription = 2)).isEmpty())
    }
    @Test fun `sampling gaps and backward clocks do not invent intervening radio events`() {
        listOf(7000L, 500L).forEach { time ->
            assertEquals("采样不连续", NetworkEventDetector.detect(sample(), sample(time, cell("2"))).single().title)
        }
    }
    @Test fun `statistics exclude stale values and keep separate metric denominators and technologies`() {
        val samples = listOf(sample(), sample(2000, cell(stale = true)),
            sample(3000, cell().copy(rsrp = -90, sinr = null)), sample(4000, cell(tech = RadioTechnology.NR)),
            sample(5000, cell = null), TestLogSample(6000))
        val analysis = TestLogAnalysis.create(samples)
        val lte = analysis.statistics.first { it.technology == RadioTechnology.LTE }
        assertEquals(3, lte.total)
        assertEquals(1, lte.stale)
        assertEquals(2, lte.rsrpCount)
        assertEquals(1, lte.sinrCount)
        assertEquals(1, lte.weakCount)
        assertEquals(-102.5, lte.averageRsrp)
        assertEquals(3, analysis.statistics.size)
        assertEquals(1, analysis.samplesWithoutSimInfo)
        assertEquals(0, analysis.statistics.first { it.technology == RadioTechnology.UNKNOWN }.rsrpCount)
    }
    @Test fun `persisted events round trip and analysis does not duplicate inferred events`() {
        val first = sample()
        val next = sample(2000, cell("2"))
        val withEvents = next.copy(events = NetworkEventDetector.detect(first, next))
        val doc = TestLogDocument(TestLogMetadata("1", "记录", importedAtMillis = 1, startedAtMillis = 1, endedAtMillis = 2000, sampleCount = 2), listOf(first, withEvents))
        val bytes = ByteArrayOutputStream()
        TestLogFileCodec.write(bytes, doc)
        val loaded = TestLogFileCodec.read(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(doc, loaded)
        assertEquals(1, TestLogAnalysis.create(loaded.samples).events.size)
    }
    @Test fun `legacy samples without diagnostics still decode and show unknown instead of fabricated provenance`() {
        val legacy = Json.decodeFromString(TestLogSample.serializer(), "{\"timestampMillis\":1000,\"sims\":[]}")
        assertNull(legacy.airplaneModeOn)
        assertTrue(legacy.events.isEmpty())
    }
    @Test fun `replay clamps empty and last sample and live history stays bounded`() {
        val samples = listOf(sample(), sample(2000))
        val doc = TestLogDocument(TestLogMetadata("1", "记录", importedAtMillis = 1, startedAtMillis = 1, endedAtMillis = 2, sampleCount = 2), samples)
        val replay = LogReplayState(doc, TestLogAnalysis.create(samples))
        assertEquals(1, replay.seek(100).index)
        assertEquals(0, replay.seek(-1).index)
        var live = LiveNetworkEvents()
        repeat(150) { i -> live = live.append(sample((i + 1) * 1000L, cell(i.toString()))) }
        assertEquals(100, live.events.size)
    }
}
