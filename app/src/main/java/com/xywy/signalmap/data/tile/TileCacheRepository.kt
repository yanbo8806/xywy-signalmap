package com.xywy.signalmap.data.tile

import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import java.io.InputStream

class TileCacheRepository(
    private val catalog: TileSourceCatalog,
    private val store: TileCacheStore,
    private val memoryCache: MemoryTileCache = MemoryTileCache(),
    private val upstreamFetcher: TileUpstreamFetcher = HttpTileUpstreamFetcher(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) : TileResolver {
    private val inFlightLockGuard = Any()
    private val inFlightLocks = linkedMapOf<TileCacheKey, Any>()

    override fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload? {
        val key = TileCacheKey(sourceId = sourceId, z = z, x = x, y = y)
        memoryCache.read(key)?.let { return it }
        readDiskCache(key)?.let {
            memoryCache.write(key, it)
            return it
        }

        val lock = lockFor(key)
        return try {
            synchronized(lock) {
                memoryCache.read(key)?.let { return@synchronized it }
                readDiskCache(key)?.let {
                    memoryCache.write(key, it)
                    return@synchronized it
                }

                val source = catalog.findSource(sourceId) ?: return@synchronized null
                val template = source.urlTemplates.pickTemplate(z, x, y) ?: return@synchronized null
                val url = template
                    .replace("{z}", z.toString())
                    .replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                val payload = runCatching { upstreamFetcher.fetch(url) }.getOrNull() ?: return@synchronized null
                memoryCache.write(key, payload)
                try {
                    store.write(key, payload, clock())
                } catch (_: Exception) {
                    // A full or unavailable disk cache must not discard a downloaded tile.
                }
                payload
            }
        } finally {
            releaseLock(key, lock)
        }
    }

    private fun readDiskCache(key: TileCacheKey): TilePayload? = try {
        store.read(key)
    } catch (_: Exception) {
        null
    }

    private fun lockFor(key: TileCacheKey): Any = synchronized(inFlightLockGuard) {
        inFlightLocks.getOrPut(key) { Any() }
    }

    private fun releaseLock(key: TileCacheKey, lock: Any) {
        synchronized(inFlightLockGuard) {
            if (inFlightLocks[key] === lock) {
                inFlightLocks.remove(key)
            }
        }
    }

    private fun List<String>.pickTemplate(z: Int, x: Int, y: Int): String? {
        if (isEmpty()) return null
        val index = abs((z * 31 + x * 17 + y) % size)
        return this[index]
    }
}

class HttpTileUpstreamFetcher : TileUpstreamFetcher {
    override fun fetch(url: String): TilePayload {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 10_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "XYWY-SignalMap/0.1")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("Tile upstream returned HTTP $code")
            }
            val contentType = connection.contentType
                ?.substringBefore(';')
                ?.takeIf(String::isNotBlank)
                ?: guessContentType(url)
            val bytes = connection.inputStream.use { TilePayloadSizeGuard.readBytes(it) }
            return TilePayload(bytes = bytes, contentType = contentType)
        } finally {
            connection.disconnect()
        }
    }

    private fun guessContentType(url: String): String = when {
        url.substringBefore('?').endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
        url.substringBefore('?').endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        url.substringBefore('?').endsWith(".webp", ignoreCase = true) -> "image/webp"
        else -> "image/png"
    }
}

object TilePayloadSizeGuard {
    private const val DEFAULT_MAX_BYTES = 8 * 1024 * 1024
    private const val BUFFER_SIZE = 16 * 1024

    fun readBytes(inputStream: InputStream, maxBytes: Int = DEFAULT_MAX_BYTES): ByteArray {
        require(maxBytes > 0) { "maxBytes must be positive" }
        val output = java.io.ByteArrayOutputStream(minOf(maxBytes, BUFFER_SIZE))
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = inputStream.read(buffer)
            if (read < 0) break
            total += read
            check(total <= maxBytes) { "Tile payload too large: $total bytes" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
