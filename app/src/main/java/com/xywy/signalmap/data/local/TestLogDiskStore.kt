package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class TestLogDiskStore(
    private val rootDir: File,
    private val json: Json,
) {
    private val indexFile = File(rootDir, "index.json")
    val journal = TestLogJournal(rootDir, json)

    init {
        rootDir.mkdirs()
    }

    fun loadMetadata(): List<TestLogMetadata> {
        val indexed = runCatching {
            json.decodeFromString(ListSerializer(TestLogMetadata.serializer()), indexFile.readText(Charsets.UTF_8))
        }.getOrDefault(emptyList()).filter { logFile(it.id).isFile }
        // A crash between saving a log and its index must not make that log disappear.
        val indexedIds = indexed.map { it.id }.toSet()
        val orphaned = rootDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "json" && it != indexFile && it.nameWithoutExtension !in indexedIds }
            .mapNotNull { file ->
                runCatching {
                    json.decodeFromString(TestLogDocument.serializer(), file.readText(Charsets.UTF_8)).meta
                        .takeIf { it.id == file.nameWithoutExtension }
                }.getOrNull()
            }
        return indexed + orphaned
    }

    fun saveIndex(metadata: List<TestLogMetadata>) {
        indexFile.writeDurably(
            json.encodeToString(ListSerializer(TestLogMetadata.serializer()), metadata),
        )
    }

    fun loadLog(metadata: TestLogMetadata): TestLogDocument? {
        val file = logFile(metadata.id)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(TestLogDocument.serializer(), file.readText(Charsets.UTF_8))
                .copy(meta = metadata)
        }.getOrNull()
    }

    fun saveLog(document: TestLogDocument) {
        logFile(document.meta.id).writeDurably(
            json.encodeToString(TestLogDocument.serializer(), document),
        )
    }

    fun deleteLog(logId: String) {
        journal.delete(logId)
        val file = logFile(logId)
        check(!file.exists() || file.delete()) { "无法删除测试记录" }
    }

    fun recoverRecordings() {
        journal.pendingIds().forEach { id ->
            // Leave unreadable journals on disk for diagnosis or a later recovery attempt.
            runCatching {
                val finalized = runCatching {
                    json.decodeFromString(TestLogDocument.serializer(), logFile(id).readText(Charsets.UTF_8))
                }.getOrNull()
                if (finalized == null) {
                    val recovered = journal.read(id)
                    saveLog(recovered.copy(meta = recovered.meta.copy(name = recovered.meta.name + "（中断恢复）")))
                }
                saveIndex(loadMetadata())
                journal.delete(id)
            }
        }
    }

    private fun logFile(logId: String): File = File(rootDir, "$logId.json")
}
