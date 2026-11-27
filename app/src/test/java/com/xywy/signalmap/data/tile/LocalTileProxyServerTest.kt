package com.xywy.signalmap.data.tile

import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LocalTileProxyServerTest {
    @Test
    fun `glyph routes only allow known font names and valid unicode blocks`() {
        assertEquals("Noto Sans Regular" to "0-255", LocalTileProxyServer.parseGlyphRoute("/glyphs/Noto%20Sans%20Regular/0-255.pbf"))
        assertNull(LocalTileProxyServer.parseGlyphRoute("/glyphs/../../0-255.pbf"))
        assertNull(LocalTileProxyServer.parseGlyphRoute("/glyphs/unknown/0-255.pbf"))
        assertNull(LocalTileProxyServer.parseGlyphRoute("/glyphs/Noto%20Sans%20Regular/1-256.pbf"))
        assertNull(LocalTileProxyServer.parseGlyphRoute("/glyphs/Noto%20Sans%20Regular/0-999.pbf"))
    }

    @Test
    fun `native map text loads glyph bytes over the local proxy`() {
        val resolver = object : TileResolver {
            override fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload? = null
        }
        val bytes = byteArrayOf(10, 5, 18, 3, 65, 66, 67)
        val server = LocalTileProxyServer(resolver, glyphResolver = { _, _ -> TilePayload(bytes, "application/x-protobuf") })
        val config = server.start()
        try {
            val connection = URL("${config.baseUrl}/glyphs/Noto%20Sans%20Regular/0-255.pbf").openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            assertContentEquals(bytes, connection.inputStream.use { it.readBytes() })
            assertEquals(200, connection.responseCode)
        } finally { server.stop() }
    }

    @Test
    fun `parse route accepts valid tile path`() {
        val route = LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/13/2345/1234")

        assertNotNull(route)
        assertEquals("preset-amap-road-base", route.sourceId)
        assertEquals(13, route.z)
        assertEquals(2345, route.x)
        assertEquals(1234, route.y)
    }

    @Test
    fun `parse route rejects malformed path`() {
        assertNull(LocalTileProxyServer.parseRoute("/tile/preset-amap-road-base/13/2345/1234"))
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/not-a-number/2345/1234"))
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/13/2345"))
    }

    @Test
    fun `parse route rejects out of range tile coordinates`() {
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/31/0/0"))
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/2/4/0"))
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/2/0/4"))
        assertNull(LocalTileProxyServer.parseRoute("/tiles/preset-amap-road-base/2/-1/0"))
    }

    @Test
    fun `server returns repository bytes over localhost`() {
        val repository = object : TileResolver {
            override fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload? {
                return TilePayload(byteArrayOf(4, 5, 6), "image/png")
            }
        }
        val server = LocalTileProxyServer(repository)
        val config = server.start()
        try {
            val connection = URL("${config.baseUrl}/tiles/preset-amap-road-base/10/1/2")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
            connection.requestMethod = "GET"

            val bytes = connection.inputStream.use { BufferedInputStream(it).readBytes() }

            assertEquals(200, connection.responseCode)
            assertEquals("image/png", connection.contentType)
            assertContentEquals(byteArrayOf(4, 5, 6), bytes)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `server ignores broken pipe when map cancels tile request`() {
        val uncaught = AtomicReference<Throwable?>()
        val uncaughtLatch = CountDownLatch(1)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            uncaught.set(throwable)
            uncaughtLatch.countDown()
        }
        val repository = object : TileResolver {
            override fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload {
                return TilePayload(ByteArray(2 * 1024 * 1024) { 1 }, "image/png")
            }
        }
        val server = LocalTileProxyServer(repository)
        val config = server.start()
        try {
            Socket(config.host, config.port).use { socket ->
                socket.setSoLinger(true, 0)
                socket.getOutputStream().write(
                    "GET /tiles/preset-amap-road-base/10/1/2 HTTP/1.1\r\nHost: localhost\r\n\r\n"
                        .toByteArray(Charsets.ISO_8859_1),
                )
                socket.getOutputStream().flush()
            }

            uncaughtLatch.await(800, TimeUnit.MILLISECONDS)

            assertNull(uncaught.get())
        } finally {
            server.stop()
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    @Test
    fun `server handles burst of parallel tile requests`() {
        val requestCount = 6
        val started = CountDownLatch(requestCount)
        val release = CountDownLatch(1)
        val repository = object : TileResolver {
            override fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload {
                started.countDown()
                release.await(2, TimeUnit.SECONDS)
                return TilePayload(byteArrayOf(7, 8, 9), "image/png")
            }
        }
        val server = LocalTileProxyServer(repository)
        val config = server.start()
        val executor = Executors.newFixedThreadPool(requestCount)
        try {
            val futures = (0 until requestCount).map { index ->
                executor.submit(Callable {
                    val connection = URL("${config.baseUrl}/tiles/preset-amap-road-base/10/$index/2")
                        .openConnection() as HttpURLConnection
                    connection.connectTimeout = 2_000
                    connection.readTimeout = 4_000
                    connection.requestMethod = "GET"
                    val bytes = connection.inputStream.use { BufferedInputStream(it).readBytes() }
                    connection.responseCode to bytes
                })
            }

            assertEquals(true, started.await(700, TimeUnit.MILLISECONDS))
            release.countDown()

            futures.forEach { future ->
                val (code, bytes) = future.get(4, TimeUnit.SECONDS)
                assertEquals(200, code)
                assertContentEquals(byteArrayOf(7, 8, 9), bytes)
            }
        } finally {
            release.countDown()
            executor.shutdownNow()
            server.stop()
        }
    }
}
