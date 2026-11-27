package com.xywy.signalmap.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogOrigin
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestMetricMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class TestLogStore(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val diskStore = TestLogDiskStore(
        rootDir = File(context.filesDir, "test-logs"),
        json = json,
    )
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val documentsState = MutableStateFlow<List<TestLogDocument>>(emptyList())
    private val mutex = Mutex()

    val documents: StateFlow<List<TestLogDocument>> = documentsState.asStateFlow()

    private val initialization = storeScope.launch {
        mutex.withLock {
            diskStore.recoverRecordings()
            documentsState.value = loadMetadataSkeleton()
            preloadVisibleLogs()
        }
    }

    suspend fun beginRecording(name: String, startedAtMillis: Long, defaultMetricMode: TestMetricMode): String = onDisk {
        val metadata = TestLogMetadata(
            id = UUID.randomUUID().toString(),
            name = name,
            sourceFileName = "$name.xywy",
            importedAtMillis = startedAtMillis,
            startedAtMillis = startedAtMillis,
            endedAtMillis = startedAtMillis,
            sampleCount = 0,
            defaultMetricMode = defaultMetricMode,
        )
        diskStore.journal.begin(metadata)
        metadata.id
    }

    suspend fun appendSample(logId: String, sample: TestLogSample) = onDisk {
        diskStore.journal.append(logId, sample)
    }

    suspend fun finishRecording(logId: String): TestLogDocument = onDisk {
        val document = addDocument(diskStore.journal.read(logId))
        diskStore.journal.delete(logId)
        document
    }

    private suspend fun <T> onDisk(block: () -> T): T = withContext(Dispatchers.IO) {
        initialization.join()
        mutex.withLock { block() }
    }

    suspend fun addRecordedLog(
        name: String,
        sourceFileName: String,
        startedAtMillis: Long,
        endedAtMillis: Long,
        samples: List<TestLogSample>,
        defaultMetricMode: TestMetricMode,
    ): TestLogDocument = onDisk {
        val metadata = TestLogMetadata(
            id = UUID.randomUUID().toString(),
            name = name,
            sourceFileName = sourceFileName,
            importedAtMillis = System.currentTimeMillis(),
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
            sampleCount = samples.size,
            isVisible = true,
            defaultMetricMode = defaultMetricMode,
            origin = TestLogOrigin.RECORDED,
        )
        addDocument(TestLogDocument(meta = metadata, samples = samples))
    }

    suspend fun importArchive(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
    ): TestLogDocument = onDisk {
        val imported = resolver.openInputStream(uri)?.use(TestLogFileCodec::read)
            ?: error("无法读取测试记录文件")
        val now = System.currentTimeMillis()
        val metadata = imported.meta.copy(
            id = UUID.randomUUID().toString(),
            name = imported.meta.name.takeIf(String::isNotBlank)
                ?: displayName.substringBeforeLast('.').ifBlank { "测试记录" },
            sourceFileName = displayName,
            importedAtMillis = now,
            sampleCount = imported.samples.size,
            isVisible = true,
            origin = TestLogOrigin.IMPORTED,
        )
        addDocument(imported.copy(meta = metadata))
    }

    suspend fun setVisibility(
        logId: String,
        visible: Boolean,
    ) = onDisk {
        publishDocuments(
            documentsState.value.map { document ->
                if (document.meta.id != logId) {
                    document
                } else {
                    document.copy(
                        meta = document.meta.copy(isVisible = visible),
                        samples = if (visible) document.samples else emptyList(),
                    )
                }
            },
        )
        if (visible) {
            loadLogIntoState(logId)
        }
    }

    suspend fun deleteLog(logId: String) = onDisk {
        diskStore.deleteLog(logId)
        publishDocuments(documentsState.value.filterNot { it.meta.id == logId })
    }

    suspend fun exportArchive(
        logId: String,
        destination: File,
    ): File = onDisk {
        val document = loadFullDocument(logId) ?: error("测试记录不存在")
        TestLogFileCodec.write(destination, document)
        destination
    }

    suspend fun findLog(logId: String): TestLogDocument? = onDisk {
        loadFullDocument(logId)
    }

    private fun addDocument(document: TestLogDocument): TestLogDocument {
        diskStore.saveLog(document)
        publishDocuments(documentsState.value.filterNot { it.meta.id == document.meta.id } + document)
        return document
    }

    private fun loadMetadataSkeleton(): List<TestLogDocument> =
        sortedDocuments(
            diskStore.loadMetadata().map { metadata ->
                TestLogDocument(
                    meta = metadata,
                    samples = emptyList(),
                )
            },
        )

    private fun preloadVisibleLogs() {
        documentsState.value
            .filter { it.meta.isVisible }
            .forEach { document ->
                loadLogIntoState(document.meta.id)
            }
    }

    private fun loadLogIntoState(logId: String) {
        val currentDocument = documentsState.value.firstOrNull { it.meta.id == logId } ?: return
        if (!currentDocument.meta.isVisible || currentDocument.samples.isNotEmpty()) return
        val loadedDocument = diskStore.loadLog(currentDocument.meta) ?: return
        documentsState.update { documents ->
            sortedDocuments(
                documents.map { document ->
                    if (document.meta.id == logId && document.meta.isVisible) {
                        loadedDocument.copy(meta = document.meta)
                    } else {
                        document
                    }
                },
            )
        }
    }

    private fun loadFullDocument(logId: String): TestLogDocument? {
        val current = documentsState.value.firstOrNull { it.meta.id == logId } ?: return null
        return if (current.samples.isNotEmpty()) {
            current
        } else {
            diskStore.loadLog(current.meta)
        }
    }

    private fun publishDocuments(documents: List<TestLogDocument>) {
        val sorted = sortedDocuments(documents)
        diskStore.saveIndex(sorted.map { it.meta })
        documentsState.value = sorted
    }

    private fun sortedDocuments(documents: List<TestLogDocument>): List<TestLogDocument> =
        documents.sortedByDescending { it.meta.importedAtMillis }
}
