package com.xywy.signalmap.map

import com.xywy.signalmap.data.tile.TileProxyConfig
import com.xywy.signalmap.data.tile.TileSourceCatalog
import com.xywy.signalmap.domain.model.TilePreset
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object MapStyleFactory {
    private const val GLYPHS_URL = "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf"

    fun buildStyleJson(
        tilePreset: TilePreset,
        proxyConfig: TileProxyConfig? = null,
    ): String {
        val resolvedPreset = TileSourceCatalog.rewriteForProxy(
            preset = tilePreset,
            proxyConfig = proxyConfig,
        )
        val style = buildJsonObject {
            put("version", 8)
            put("name", resolvedPreset.title)
            put("glyphs", proxyConfig?.let { "${it.baseUrl}/glyphs/{fontstack}/{range}.pbf" } ?: GLYPHS_URL)
            putJsonObject("sources") {
                putJsonObject("base-raster") {
                    put("type", "raster")
                    putJsonArray("tiles") { resolvedPreset.tiles.forEach { add(JsonPrimitive(it)) } }
                    put("tileSize", resolvedPreset.tileSize)
                    put("attribution", resolvedPreset.attribution)
                    put("maxzoom", 20)
                }
                resolvedPreset.overlays.forEach { overlay ->
                    putJsonObject(overlay.id) {
                        put("type", "raster")
                        putJsonArray("tiles") { overlay.tiles.forEach { add(JsonPrimitive(it)) } }
                        put("tileSize", overlay.tileSize)
                        put("maxzoom", 20)
                    }
                }
            }
            put("layers", buildJsonArray {
                add(buildJsonObject {
                    put("id", "background")
                    put("type", "background")
                    putJsonObject("paint") { put("background-color", "#EFF3F8") }
                })
                add(buildJsonObject {
                    put("id", "base-raster-layer")
                    put("type", "raster")
                    put("source", "base-raster")
                })
                resolvedPreset.overlays.forEach { overlay ->
                    add(buildJsonObject {
                        put("id", "overlay-${overlay.id}")
                        put("type", "raster")
                        put("source", overlay.id)
                    })
                }
            })
        }
        return style.toString()
    }
}
