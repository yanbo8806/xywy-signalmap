package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.data.importer.ImportReadBudget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object TestLogFileCodec {
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val SESSION_ENTRY = "session.json"
    private const val FORMAT_NAME = "xywy-test-log"
    private const val FORMAT_VERSION = 1
    private const val MAX_SESSION_BYTES = 64 * 1024 * 1024

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun write(
        file: File,
        document: TestLogDocument,
    ) {
        file.parentFile?.mkdirs()
        file.outputStream().buffered().use { output ->
            write(output, document)
        }
    }

    fun write(
        outputStream: OutputStream,
        document: TestLogDocument,
    ) {
        ZipOutputStream(outputStream).use { zip ->
            zip.writeEntry(
                name = MANIFEST_ENTRY,
                payload = json.encodeToString(
                    ArchiveManifest.serializer(),
                    ArchiveManifest(
                        format = FORMAT_NAME,
                        version = FORMAT_VERSION,
                        exportedAtMillis = System.currentTimeMillis(),
                    ),
                ),
            )
            zip.writeEntry(
                name = SESSION_ENTRY,
                payload = json.encodeToString(TestLogDocument.serializer(), document),
            )
        }
    }

    fun read(file: File): TestLogDocument {
        file.inputStream().buffered().use { input ->
            return read(input)
        }
    }

    fun read(inputStream: InputStream): TestLogDocument {
        var manifest: ArchiveManifest? = null
        var sessionJson: String? = null
        val budget = ImportReadBudget(128L * 1024 * 1024)
        var entryCount = 0
        ZipInputStream(inputStream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entryCount <= 1_024) { "测试记录压缩包包含过多文件" }
                when (entry.name) {
                    MANIFEST_ENTRY -> {
                        require(manifest == null) { "测试记录包含重复的 manifest.json" }
                        val payload = budget.read(zip, 64 * 1024, "测试记录说明").toString(Charsets.UTF_8)
                        manifest = json.decodeFromString(ArchiveManifest.serializer(), payload)
                    }
                    SESSION_ENTRY -> {
                        require(sessionJson == null) { "测试记录包含重复的 session.json" }
                        sessionJson = budget.read(zip, MAX_SESSION_BYTES, "测试记录").toString(Charsets.UTF_8)
                    }
                    else -> budget.discard(zip)
                }
                zip.closeEntry()
            }
        }
        val resolvedManifest = requireNotNull(manifest) { "缺少 manifest.json" }
        require(resolvedManifest.format == FORMAT_NAME) { "不是有效的 XYWY 测试记录文件" }
        require(resolvedManifest.version == FORMAT_VERSION) { "不支持的 XYWY 文件版本" }
        return json.decodeFromString(
            TestLogDocument.serializer(),
            requireNotNull(sessionJson) { "缺少 session.json" },
        )
    }

    private fun ZipOutputStream.writeEntry(
        name: String,
        payload: String,
    ) {
        putNextEntry(ZipEntry(name))
        write(payload.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    @Serializable
    private data class ArchiveManifest(
        val format: String,
        val version: Int,
        val exportedAtMillis: Long,
    )
}
