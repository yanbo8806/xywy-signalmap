package com.xywy.signalmap.data.tile

import java.util.LinkedHashMap

class MemoryTileCache(
    private val maxBytes: Int = DEFAULT_MAX_BYTES,
) {
    private val lock = Any()
    private val entries = LinkedHashMap<TileCacheKey, TilePayload>(16, 0.75f, true)
    private var currentBytes: Int = 0

    fun read(key: TileCacheKey): TilePayload? = synchronized(lock) {
        entries[key]
    }

    fun write(key: TileCacheKey, payload: TilePayload) {
        if (maxBytes <= 0) return
        val payloadBytes = payload.bytes.size
        synchronized(lock) {
            entries.remove(key)?.let { existing ->
                currentBytes -= existing.bytes.size
            }
            if (payloadBytes > maxBytes) {
                trimToSizeLocked(maxBytes)
                return
            }
            entries[key] = payload
            currentBytes += payloadBytes
            trimToSizeLocked(maxBytes)
        }
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            currentBytes = 0
        }
    }

    private fun trimToSizeLocked(targetBytes: Int) {
        val iterator = entries.entries.iterator()
        while (currentBytes > targetBytes && iterator.hasNext()) {
            val entry = iterator.next()
            currentBytes -= entry.value.bytes.size
            iterator.remove()
        }
    }

    companion object {
        private const val DEFAULT_MAX_BYTES: Int = 8 * 1024 * 1024
    }
}
