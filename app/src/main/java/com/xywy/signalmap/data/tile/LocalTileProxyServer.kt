package com.xywy.signalmap.data.tile

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalTileProxyServer(
    private val resolver: TileResolver,
    private val backlog: Int = DEFAULT_BACKLOG,
    private val requestConcurrency: Int = DEFAULT_REQUEST_CONCURRENCY,
    private val glyphResolver: ((font: String, range: String) -> TilePayload?)? = null,
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var acceptExecutor: ExecutorService? = null
    private var requestExecutor: ExecutorService? = null

    fun start(): TileProxyConfig {
        if (running.get()) {
            val socket = requireNotNull(serverSocket)
            return TileProxyConfig(host = LOCAL_HOST, port = socket.localPort)
        }
        val socket = ServerSocket(0, backlog, java.net.InetAddress.getByName(LOCAL_HOST))
        serverSocket = socket
        requestExecutor = Executors.newFixedThreadPool(requestConcurrency)
        acceptExecutor = Executors.newSingleThreadExecutor()
        running.set(true)
        acceptExecutor?.execute {
            while (running.get()) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                val executor = requestExecutor
                if (executor == null) {
                    runCatching { client.close() }
                } else {
                    runCatching {
                        executor.execute { handleClientSafely(client) }
                    }.onFailure {
                        runCatching { client.close() }
                    }
                }
            }
        }
        return TileProxyConfig(host = LOCAL_HOST, port = socket.localPort)
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        acceptExecutor?.shutdownNow()
        requestExecutor?.shutdownNow()
        acceptExecutor = null
        requestExecutor = null
        serverSocket = null
    }

    private fun handleClientSafely(client: Socket) {
        try {
            handleClient(client)
        } catch (_: IOException) {
            runCatching { client.close() }
        }
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = SOCKET_TIMEOUT_MILLIS
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.ISO_8859_1))
            val requestLine = reader.readLine().orEmpty()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }
            val parts = requestLine.split(' ')
            if (parts.size < 2 || parts[0] != "GET") {
                writeStatus(socket, 405, "Method Not Allowed")
                return
            }
            val path = parts[1].substringBefore('?')
            val glyphRoute = parseGlyphRoute(path)
            if (glyphRoute != null && glyphResolver != null) {
                val payload = glyphResolver.invoke(glyphRoute.first, glyphRoute.second)
                if (payload == null) writeStatus(socket, 404, "Not Found") else writePayload(socket, payload)
                return
            }
            val route = parseRoute(path)
            if (route == null) {
                writeStatus(socket, 404, "Not Found")
                return
            }
            val payload = resolver.getTile(route.sourceId, route.z, route.x, route.y)
            if (payload == null) {
                writeStatus(socket, 502, "Bad Gateway")
                return
            }
            writePayload(socket, payload)
        }
    }

    private fun writeStatus(socket: Socket, code: Int, reason: String) {
        val body = reason.toByteArray(Charsets.UTF_8)
        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.ISO_8859_1)).apply {
            write("HTTP/1.1 $code $reason\r\n")
            write("Content-Type: text/plain; charset=utf-8\r\n")
            write("Content-Length: ${body.size}\r\n")
            write("Connection: close\r\n")
            write("\r\n")
            flush()
        }
        socket.getOutputStream().write(body)
    }

    private fun writePayload(socket: Socket, payload: TilePayload) {
        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.ISO_8859_1)).apply {
            write("HTTP/1.1 200 OK\r\n")
            write("Content-Type: ${payload.contentType}\r\n")
            write("Content-Length: ${payload.bytes.size}\r\n")
            write("Cache-Control: public, max-age=31536000\r\n")
            write("Connection: close\r\n")
            write("\r\n")
            flush()
        }
        socket.getOutputStream().write(payload.bytes)
    }

    data class Route(
        val sourceId: String,
        val z: Int,
        val x: Int,
        val y: Int,
    )

    companion object {
        private const val LOCAL_HOST = "127.0.0.1"
        private const val DEFAULT_BACKLOG = 256
        private const val DEFAULT_REQUEST_CONCURRENCY = 6
        private const val SOCKET_TIMEOUT_MILLIS = 10_000
        private const val MAX_ZOOM = 30

        internal fun parseGlyphRoute(path: String): Pair<String, String>? {
            val parts = path.trim('/').split('/')
            if (parts.size != 3 || parts[0] != "glyphs" || !parts[2].endsWith(".pbf")) return null
            val range = parts[2].removeSuffix(".pbf")
            val bounds = range.split('-')
            if (bounds.size != 2) return null
            val start = bounds[0].toIntOrNull() ?: return null
            val end = bounds[1].toIntOrNull() ?: return null
            if (start !in 0..65280 || start % 256 != 0 || end != start + 255) return null
            val font = runCatching { URLDecoder.decode(parts[1], Charsets.UTF_8.name()) }.getOrNull() ?: return null
            if (font !in setOf("Noto Sans Regular", "Open Sans Regular", "Arial Unicode MS Regular")) return null
            return font to range
        }

        fun parseRoute(path: String): Route? {
            val parts = path.trim('/').split('/')
            if (parts.size != 5 || parts[0] != "tiles") return null
            val sourceId = runCatching {
                URLDecoder.decode(parts[1], Charsets.UTF_8.name())
            }.getOrNull()?.takeIf(String::isNotBlank) ?: return null
            val z = parts[2].toIntOrNull() ?: return null
            val x = parts[3].toIntOrNull() ?: return null
            val y = parts[4].toIntOrNull() ?: return null
            if (z < 0 || z > MAX_ZOOM || x < 0 || y < 0) return null
            val tileCount = 1L shl z
            if (x >= tileCount || y >= tileCount) return null
            return Route(sourceId = sourceId, z = z, x = x, y = y)
        }
    }
}
