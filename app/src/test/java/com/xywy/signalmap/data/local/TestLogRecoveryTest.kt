package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogSample
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

class TestLogRecoveryTest {
    @Test fun `interrupted deletion does not leave a ghost record in stale index`() {
        val disk = store()
        val document = TestLogDocument(metadata(), emptyList())
        disk.saveLog(document)
        disk.saveIndex(listOf(document.meta))
        disk.deleteLog(document.meta.id)
        assertTrue(store().loadMetadata().isEmpty())
    }

    @get:Rule val temporary = TemporaryFolder()
    private val json = Json { ignoreUnknownKeys = true }
    private fun store() = TestLogDiskStore(temporary.root, json)
    private fun metadata(id: String = "route") = TestLogMetadata(id, "路测", importedAtMillis = 1000,
        startedAtMillis = 1000, endedAtMillis = 1000, sampleCount = 0)
    private fun sample(time: Long) = TestLogSample(time, primaryRsrp = -96, primarySinr = 12)

    @Test fun `each appended point survives recreation without stopping or saving`() {
        store().journal.begin(metadata())
        val samples = listOf(sample(1001), sample(1002), sample(1003))
        samples.forEach { store().journal.append("route", it) }
        val reopened = store()
        reopened.recoverRecordings()
        val meta = reopened.loadMetadata().single()
        assertEquals(3, meta.sampleCount)
        assertEquals(1003, meta.endedAtMillis)
        assertTrue(meta.name.contains("中断恢复"))
        assertEquals(samples, reopened.loadLog(meta)?.samples)
        assertTrue(reopened.journal.pendingIds().isEmpty())
    }

    @Test fun `killed writer with partial tail loses no complete points`() {
        val disk = store()
        disk.journal.begin(metadata())
        disk.journal.append("route", sample(1001))
        disk.journal.append("route", sample(1002))
        File(temporary.root, "route.jsonl").appendText("{\"timestampMillis\":1003,\"sims\":[")
        store().recoverRecordings()
        val meta = store().loadMetadata().single()
        assertEquals(listOf(sample(1001), sample(1002)), store().loadLog(meta)?.samples)
    }

    @Test fun `crash before index replacement does not hide committed log`() {
        val document = TestLogDocument(metadata().copy(sampleCount = 1), listOf(sample(1001)))
        store().saveLog(document)
        File(temporary.root, "index.json.tmp").writeText("[")
        assertEquals(listOf(document.meta), store().loadMetadata())
    }

    @Test fun `crash after final save before journal cleanup does not duplicate or rename log`() {
        val disk = store()
        disk.journal.begin(metadata())
        disk.journal.append("route", sample(1001))
        val document = disk.journal.read("route")
        disk.saveLog(document)
        disk.saveIndex(listOf(document.meta))
        repeat(2) { store().recoverRecordings() }
        assertEquals(listOf(document.meta), store().loadMetadata())
        assertEquals(document.samples, store().loadLog(document.meta)?.samples)
    }

    @Test fun `damaged index can be rebuilt from saved logs`() {
        val document = TestLogDocument(metadata(), emptyList())
        store().saveLog(document)
        File(temporary.root, "index.json").writeText("[broken")
        assertEquals(listOf(document.meta), store().loadMetadata())
    }

    @Test fun `corruption in the middle preserves journal instead of silently dropping later points`() {
        val disk = store()
        disk.journal.begin(metadata())
        disk.journal.append("route", sample(1001))
        File(temporary.root, "route.jsonl").appendText("broken\n")
        disk.journal.append("route", sample(1003))
        store().recoverRecordings()
        assertTrue(File(temporary.root, "route.jsonl").isFile)
        assertTrue(store().loadMetadata().isEmpty())
    }

    @Test fun `recovered records remain exportable with the original archive format`() {
        val disk = store()
        disk.journal.begin(metadata())
        disk.journal.append("route", sample(1001))
        disk.recoverRecordings()
        val document = requireNotNull(disk.loadLog(disk.loadMetadata().single()))
        val archive = temporary.newFile("route.xywy")
        TestLogFileCodec.write(archive, document)
        assertEquals(document, TestLogFileCodec.read(archive))
    }
}
