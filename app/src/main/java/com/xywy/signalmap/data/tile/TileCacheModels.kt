package com.xywy.signalmap.data.tile

import java.net.URLEncoder

data class TileCacheKey(
    val sourceId: String,
    val z: Int,
    val x: Int,
    val y: Int,
)

data class TilePayload(
    val bytes: ByteArray,
    val contentType: String,
)

data class TileProxyConfig(
    val host: String,
    val port: Int,
) {
    val baseUrl: String = "http://$host:$port"

    fun tileTemplate(sourceId: String): String {
        val encodedSource = URLEncoder.encode(sourceId, Charsets.UTF_8.name()).replace("+", "%20")
        return "$baseUrl/tiles/$encodedSource/{z}/{x}/{y}"
    }
}

data class TileSource(
    val sourceId: String,
    val urlTemplates: List<String>,
)

interface TileResolver {
    fun getTile(sourceId: String, z: Int, x: Int, y: Int): TilePayload?
}

interface TileCacheStore {
    fun read(key: TileCacheKey): TilePayload?

    fun write(key: TileCacheKey, payload: TilePayload, fetchedAtMillis: Long)
}

interface TileUpstreamFetcher {
    fun fetch(url: String): TilePayload
}
