package com.xywy.signalmap.data.tile

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileCacheRepositoryTest {
    @Test
    fun `unavailable disk cache still serves downloaded tiles and reuses memory cache`() {
        val upstream = RecordingTileUpstreamFetcher(TilePayload(byteArrayOf(7, 8), "image/png"))
        val failingStore = object : TileCacheStore {
            override fun read(key: TileCacheKey): TilePayload? = throw IllegalStateException("cache unavailable")
            override fun write(key: TileCacheKey, payload: TilePayload, fetchedAtMillis: Long) {
                throw IllegalStateException("disk full")
            }
        }
        val repository = TileCacheRepository(TileSourceCatalog, failingStore, upstreamFetcher = upstream)
        repeat(2) {
            assertContentEquals(byteArrayOf(7, 8), repository.getTile("preset-amap-road-base", 11, 1678, 775)?.bytes)
        }
        assertEquals(1, upstream.requests.size)
    }

    @Test
    fun `cache write failure does not discard an upstream response`() {
        val upstream = RecordingTileUpstreamFetcher(TilePayload(byteArrayOf(9), "image/png"))
        val store = object : TileCacheStore {
            override fun read(key: TileCacheKey): TilePayload? = null
            override fun write(key: TileCacheKey, payload: TilePayload, fetchedAtMillis: Long) {
                throw IllegalStateException("disk full")
            }
        }
        val repository = TileCacheRepository(TileSourceCatalog, store, upstreamFetcher = upstream)
        assertContentEquals(byteArrayOf(9), repository.getTile("preset-amap-road-base", 11, 1678, 775)?.bytes)
    }

    @Test
    fun `reuses in memory tile after first disk hit`() {
        val key = TileCacheKey(sourceId = "preset-amap-road-base", z = 11, x = 1678, y = 775)
        val store = FakeTileCacheStore().apply {
            write(
                key,
                TilePayload(bytes = byteArrayOf(1, 2, 3), contentType = "image/png"),
                fetchedAtMillis = 100L,
            )
        }
        val repository = TileCacheRepository(
            catalog = TileSourceCatalog,
            store = store,
            upstreamFetcher = RecordingTileUpstreamFetcher(
                response = TilePayload(bytes = byteArrayOf(9, 9, 9), contentType = "image/png"),
            ),
        )

        val first = repository.getTile(key.sourceId, key.z, key.x, key.y)
        val second = repository.getTile(key.sourceId, key.z, key.x, key.y)

        assertContentEquals(byteArrayOf(1, 2, 3), first?.bytes)
        assertContentEquals(byteArrayOf(1, 2, 3), second?.bytes)
        assertEquals(1, store.readCount)
    }

    @Test
    fun `returns cached tile without calling upstream`() {
        val store = FakeTileCacheStore().apply {
            write(
                TileCacheKey(sourceId = "preset-amap-road-base", z = 11, x = 1678, y = 775),
                TilePayload(bytes = byteArrayOf(1, 2, 3), contentType = "image/png"),
                fetchedAtMillis = 100L,
            )
        }
        val upstream = RecordingTileUpstreamFetcher(
            response = TilePayload(bytes = byteArrayOf(9, 9, 9), contentType = "image/png"),
        )
        val repository = TileCacheRepository(
            catalog = TileSourceCatalog,
            store = store,
            upstreamFetcher = upstream,
            clock = { 456L },
        )

        val result = repository.getTile("preset-amap-road-base", 11, 1678, 775)

        assertContentEquals(byteArrayOf(1, 2, 3), result?.bytes)
        assertEquals(0, upstream.requests.size)
    }

    @Test
    fun `downloads missing tile and writes it into cache`() {
        val store = FakeTileCacheStore()
        val upstream = RecordingTileUpstreamFetcher(
            response = TilePayload(bytes = byteArrayOf(7, 8, 9), contentType = "image/png"),
        )
        val repository = TileCacheRepository(
            catalog = TileSourceCatalog,
            store = store,
            upstreamFetcher = upstream,
            clock = { 789L },
        )

        val result = repository.getTile("preset-amap-road-base", 13, 2345, 1234)

        assertContentEquals(byteArrayOf(7, 8, 9), result?.bytes)
        assertEquals(1, upstream.requests.size)
        assertTrue(upstream.requests.single().contains("x=2345&y=1234&z=13"))
        val cached = store.read(TileCacheKey(sourceId = "preset-amap-road-base", z = 13, x = 2345, y = 1234))
        assertContentEquals(byteArrayOf(7, 8, 9), cached?.bytes)
        assertEquals(789L, store.lastFetchedAtMillis)
    }

    @Test
    fun `coalesces parallel requests for the same missing tile`() {
        val requestCount = 8
        val store = FakeTileCacheStore()
        val upstream = BlockingTileUpstreamFetcher(
            expectedRequests = requestCount,
            response = TilePayload(bytes = byteArrayOf(4, 5, 6), contentType = "image/png"),
        )
        val repository = TileCacheRepository(
            catalog = TileSourceCatalog,
            store = store,
            upstreamFetcher = upstream,
            clock = { 999L },
        )
        val executor = Executors.newFixedThreadPool(requestCount)
        try {
            val futures = (0 until requestCount).map {
                executor.submit(Callable {
                    repository.getTile("preset-amap-road-base", 13, 2345, 1234)
                })
            }
            upstream.allowFirstFetch.await(2, TimeUnit.SECONDS)

            val payloads = futures.map { it.get(3, TimeUnit.SECONDS) }

            assertEquals(1, upstream.fetchCount.get())
            payloads.forEach { payload ->
                assertContentEquals(byteArrayOf(4, 5, 6), payload?.bytes)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `returns null when source id is unknown`() {
        val repository = TileCacheRepository(
            catalog = TileSourceCatalog,
            store = FakeTileCacheStore(),
            upstreamFetcher = RecordingTileUpstreamFetcher(
                response = TilePayload(bytes = byteArrayOf(1), contentType = "image/png"),
            ),
        )

        val result = repository.getTile("missing-source", 1, 2, 3)

        assertNull(result)
    }
}

private class FakeTileCacheStore : TileCacheStore {
    private val values = linkedMapOf<TileCacheKey, TilePayload>()
    var lastFetchedAtMillis: Long? = null
    var readCount: Int = 0

    override fun read(key: TileCacheKey): TilePayload? {
        readCount += 1
        return values[key]
    }

    override fun write(key: TileCacheKey, payload: TilePayload, fetchedAtMillis: Long) {
        values[key] = payload
        lastFetchedAtMillis = fetchedAtMillis
    }
}

private class RecordingTileUpstreamFetcher(
    private val response: TilePayload,
) : TileUpstreamFetcher {
    val requests = mutableListOf<String>()

    override fun fetch(url: String): TilePayload {
        requests += url
        return response
    }
}

private class BlockingTileUpstreamFetcher(
    private val expectedRequests: Int,
    private val response: TilePayload,
) : TileUpstreamFetcher {
    val fetchCount = AtomicInteger(0)
    val allowFirstFetch = CountDownLatch(1)
    private val releaseFetch = CountDownLatch(1)

    override fun fetch(url: String): TilePayload {
        check(expectedRequests > 0)
        if (fetchCount.incrementAndGet() == 1) {
            allowFirstFetch.countDown()
            releaseFetch.countDown()
        }
        releaseFetch.await(2, TimeUnit.SECONDS)
        return response
    }
}
