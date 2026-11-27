package com.xywy.signalmap.map

import android.content.Context
import com.xywy.signalmap.data.tile.TilePayload
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Latin/numbers/punctuation are bundled; Chinese glyphs use MapLibre's local system font rasterizer. */
class MapGlyphRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val cache = ConcurrentHashMap<String, ByteArray>()

    fun getGlyphs(font: String, range: String): TilePayload? {
        if (font !in setOf("Noto Sans Regular", "Open Sans Regular", "Arial Unicode MS Regular")) return null
        if (!range.matches(Regex("[0-9]{1,5}-[0-9]{1,5}"))) return null
        val bytes = cache[range] ?: (runCatching {
            assets.open("map-fonts/$range.pbf").use { it.readBytes() }
        }.getOrNull() ?: loadRemoteRange(range))?.also { cache[range] = it } ?: return null
        return TilePayload(bytes, "application/x-protobuf")
    }

    private fun loadRemoteRange(range: String): ByteArray? = runCatching {
        val connection = URL("https://demotiles.maplibre.org/font/Noto%20Sans%20Regular/$range.pbf")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            if (connection.responseCode != 200) return@runCatching null
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
