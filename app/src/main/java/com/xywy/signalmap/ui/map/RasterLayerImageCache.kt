package com.xywy.signalmap.ui.map

import java.io.File

/**
 * Small LRU cache for already-decoded raster layer images.
 *
 * The key includes file path, mtime and length, so recolored/re-rendered TIF assets
 * automatically invalidate without forcing MapScreen to decode on every style sync.
 */
class RasterLayerImageCache<T : Any>(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val loader: (File) -> T?,
) {
    private val lock = Any()
    private val entries = LinkedHashMap<RasterLayerImageCacheKey, T>(16, 0.75f, true)

    fun get(file: File, contentRevision: Any? = null): T? {
        if (maxEntries <= 0) return loader(file)
        val key = RasterLayerImageCacheKey.from(file, contentRevision)
        synchronized(lock) {
            entries[key]?.let { return it }
        }
        val loaded = loader(file) ?: return null
        synchronized(lock) {
            entries[key]?.let { return it }
            entries[key] = loaded
            trimLocked()
            return loaded
        }
    }

    fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun trimLocked() {
        val iterator = entries.entries.iterator()
        while (entries.size > maxEntries && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    companion object {
        private const val DEFAULT_MAX_ENTRIES = 4
    }
}

data class RasterLayerImageCacheKey(
    val absolutePath: String,
    val lastModifiedMillis: Long,
    val lengthBytes: Long,
    val contentRevision: Any? = null,
) {
    companion object {
        fun from(file: File, contentRevision: Any? = null): RasterLayerImageCacheKey = RasterLayerImageCacheKey(
            absolutePath = file.absolutePath,
            lastModifiedMillis = file.lastModified(),
            lengthBytes = file.length(),
            contentRevision = contentRevision,
        )
    }
}
