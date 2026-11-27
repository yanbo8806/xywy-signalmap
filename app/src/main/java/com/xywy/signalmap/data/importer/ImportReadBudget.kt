package com.xywy.signalmap.data.importer

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Counts decompressed bytes, including skipped ZIP entries, before allocating their contents. */
internal class ImportReadBudget(private val maxBytes: Long) {
    private var total = 0L

    fun read(input: InputStream, entryLimit: Int, label: String): ByteArray {
        val output = ByteArrayOutputStream(minOf(entryLimit, DEFAULT_BUFFER_SIZE))
        consume(input, entryLimit.toLong(), label) { buffer, count -> output.write(buffer, 0, count) }
        return output.toByteArray()
    }

    fun discard(input: InputStream) {
        consume(input, maxBytes, "压缩包") { _, _ -> }
    }

    private inline fun consume(input: InputStream, entryLimit: Long, label: String, accept: (ByteArray, Int) -> Unit) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var entryBytes = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return
            entryBytes += count
            total += count
            require(entryBytes <= entryLimit && total <= maxBytes) { "$label 内容过大，请拆分文件后导入" }
            accept(buffer, count)
        }
    }
}
