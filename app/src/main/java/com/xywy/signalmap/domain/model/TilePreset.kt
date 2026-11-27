package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class TilePresetId {
    AMAP_ROAD,
    AMAP_SATELLITE,
    GOOGLE_PROXY,
    OPEN_STREET_MAP,
}

@Serializable
enum class TileCoordinateSystem {
    GCJ02,
    WGS84,
}

@Serializable
data class CustomTileSourceConfig(
    val key: String,
    val title: String,
    val urlTemplate: String,
    val tileSize: Int = 256,
    val coordinateSystem: TileCoordinateSystem = TileCoordinateSystem.GCJ02,
) {
    fun toTilePreset(): TilePreset = TilePreset(
        id = null,
        key = key,
        title = title,
        attribution = "Custom raster tiles",
        tiles = listOf(urlTemplate),
        tileSize = tileSize,
        coordinateSystem = coordinateSystem,
        note = "自定义瓦片：$urlTemplate",
    )

    companion object {
        fun create(
            existingKey: String?,
            title: String,
            urlTemplate: String,
            tileSize: Int,
            coordinateSystem: TileCoordinateSystem,
            keySeed: String,
        ): CustomTileSourceConfig {
            val normalizedTitle = title.trim().ifBlank { "自定义底图" }
            val normalizedUrl = urlTemplate.trim()
            require(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                "瓦片 URL 必须以 http:// 或 https:// 开头"
            }
            require(listOf("{z}", "{x}", "{y}").all { it in normalizedUrl }) {
                "瓦片 URL 必须包含 {z}、{x}、{y} 占位符"
            }
            val normalizedSize = if (tileSize == 512) 512 else 256
            val normalizedKey = existingKey
                ?.trim()
                ?.takeIf { it.startsWith(CUSTOM_KEY_PREFIX) }
                ?: "$CUSTOM_KEY_PREFIX${keySeed.toStableSlug().ifBlank { "tile" }}"
            return CustomTileSourceConfig(
                key = normalizedKey,
                title = normalizedTitle,
                urlTemplate = normalizedUrl,
                tileSize = normalizedSize,
                coordinateSystem = coordinateSystem,
            )
        }

        const val CUSTOM_KEY_PREFIX = "custom-"
    }
}

data class TileOverlay(
    val id: String,
    val tiles: List<String>,
    val tileSize: Int = 256,
)

data class TilePreset(
    val id: TilePresetId?,
    val key: String,
    val title: String,
    val attribution: String,
    val tiles: List<String>,
    val tileSize: Int = 256,
    val overlays: List<TileOverlay> = emptyList(),
    val coordinateSystem: TileCoordinateSystem = TileCoordinateSystem.GCJ02,
    val note: String? = null,
) {
    val isCustom: Boolean get() = id == null
}

object TilePresets {
    fun keyOf(id: TilePresetId): String = when (id) {
        TilePresetId.AMAP_ROAD -> "amap-road"
        TilePresetId.AMAP_SATELLITE -> "amap-satellite"
        TilePresetId.GOOGLE_PROXY -> "google-proxy"
        TilePresetId.OPEN_STREET_MAP -> "open-street-map"
    }

    val all: List<TilePreset> = listOf(
        TilePreset(
            id = TilePresetId.AMAP_ROAD,
            key = keyOf(TilePresetId.AMAP_ROAD),
            title = "高德普通",
            attribution = "AMap community raster preset",
            tiles = listOf(
                "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
                "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}",
            ),
            note = "高德普通路网底图。",
        ),
        TilePreset(
            id = TilePresetId.AMAP_SATELLITE,
            key = keyOf(TilePresetId.AMAP_SATELLITE),
            title = "高德卫星",
            attribution = "AMap community raster preset",
            tiles = listOf(
                "https://webst01.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst02.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst03.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
                "https://webst04.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}",
            ),
            overlays = listOf(
                TileOverlay(
                    id = "amap-label",
                    tiles = listOf(
                        "https://webst01.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}",
                        "https://webst02.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}",
                        "https://webst03.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}",
                        "https://webst04.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}",
                    ),
                ),
            ),
            note = "高德卫星底图，叠加了标注字幕。",
        ),
        TilePreset(
            id = TilePresetId.OPEN_STREET_MAP,
            key = keyOf(TilePresetId.OPEN_STREET_MAP),
            title = "OSM",
            attribution = "OpenStreetMap contributors",
            tiles = listOf("https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
            coordinateSystem = TileCoordinateSystem.WGS84,
            note = "OpenStreetMap 公共底图。",
        ),
    )

    val selectable: List<TilePreset> = all.filterNot { it.id == TilePresetId.OPEN_STREET_MAP }

    fun byId(id: TilePresetId?): TilePreset = all.firstOrNull { it.id == id } ?: all.first()

    fun byKey(key: String?): TilePreset = selectable.firstOrNull { it.key == key }
        ?: key?.let { legacyKeyToPreset(it) }
        ?: selectable.first()

    fun mergeCustom(customSources: List<CustomTileSourceConfig>): List<TilePreset> =
        selectable + customSources.map { it.toTilePreset() }

    fun legacyKeyToPreset(key: String): TilePreset? =
        runCatching { TilePresetId.valueOf(key) }.getOrNull()?.let(::byId)
            ?: when (key) {
                "google-proxy" -> byId(TilePresetId.AMAP_ROAD)
                else -> selectable.firstOrNull { it.key == key }
            }
}

private fun String.toStableSlug(): String = lowercase(Locale.US)
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
