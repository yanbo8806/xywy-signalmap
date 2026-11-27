package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogSample
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/** One synced line per sample. A killed writer can damage only the unfinished last line. */
internal class TestLogJournal(private val rootDir: File, private val json: Json) {
    fun begin(metadata: TestLogMetadata) {
        file(metadata.id).writeDurably(json.encodeToString(TestLogMetadata.serializer(), metadata) + "\n")
    }

    fun append(logId: String, sample: TestLogSample) {
        val target = file(logId)
        check(target.isFile) { "测试记录未初始化" }
        val line = json.encodeToString(TestLogSample.serializer(), sample) + "\n"
        FileOutputStream(target, true).use { output ->
            output.write(line.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
    }

    fun pendingIds(): List<String> = rootDir.listFiles().orEmpty()
        .filter { it.extension == "jsonl" }.map { it.nameWithoutExtension }

    fun read(logId: String): TestLogDocument {
        file(logId).bufferedReader(Charsets.UTF_8).use { reader ->
            val meta = json.decodeFromString(TestLogMetadata.serializer(), requireNotNull(reader.readLine()))
            require(meta.id == logId) { "测试记录标识不匹配" }
            val samples = mutableListOf<TestLogSample>()
            while (true) {
                val line = reader.readLine() ?: break
                val sample = runCatching { json.decodeFromString(TestLogSample.serializer(), line) }
                if (sample.isFailure) {
                    // Preserve all committed points if the process died halfway through the tail.
                    check(reader.readLine() == null) { "测试记录中间的数据损坏，已保留原文件" }
                    break
                }
                samples += sample.getOrThrow()
            }
            return TestLogDocument(
                meta = meta.copy(sampleCount = samples.size, endedAtMillis = samples.lastOrNull()?.timestampMillis ?: meta.startedAtMillis),
                samples = samples,
            )
        }
    }

    fun delete(logId: String) {
        val target = file(logId)
        check(!target.exists() || target.delete()) { "无法清理测试记录临时文件" }
    }

    private fun file(logId: String) = File(rootDir, "$logId.jsonl")
}
