package com.xywy.signalmap.data.tile

import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import java.util.Locale

object TileSourceCatalog {
    private val sources: List<TileSource> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        TilePresets.all.flatMap { preset ->
            buildList {
                add(TileSource(sourceId = baseSourceId(preset), urlTemplates = preset.tiles))
                preset.overlays.forEach { overlay ->
                    add(TileSource(sourceId = overlaySourceId(preset, overlay.id), urlTemplates = overlay.tiles))
                }
            }
        }
    }
    private val sourcesById: Map<String, TileSource> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        sources.associateBy { it.sourceId }
    }
    @Volatile
    private var customSourcesById: Map<String, TileSource> = emptyMap()

    fun allSources(): List<TileSource> = sources + customSourcesById.values

    fun findSource(sourceId: String): TileSource? = customSourcesById[sourceId] ?: sourcesById[sourceId]

    fun replaceCustomPresets(presets: List<TilePreset>) {
        customSourcesById = presets
            .filter { it.isCustom }
            .flatMap { preset ->
                buildList {
                    add(TileSource(sourceId = baseSourceId(preset), urlTemplates = preset.tiles))
                    preset.overlays.forEach { overlay ->
                        add(TileSource(sourceId = overlaySourceId(preset, overlay.id), urlTemplates = overlay.tiles))
                    }
                }
            }
            .associateBy { it.sourceId }
    }

    fun rewriteForProxy(
        preset: TilePreset,
        proxyConfig: TileProxyConfig?,
    ): TilePreset {
        if (proxyConfig == null) return preset
        return preset.copy(
            tiles = listOf(proxyConfig.tileTemplate(baseSourceId(preset))),
            overlays = preset.overlays.map { overlay ->
                overlay.copy(
                    tiles = listOf(proxyConfig.tileTemplate(overlaySourceId(preset, overlay.id))),
                )
            },
        )
    }

    fun baseSourceId(preset: TilePreset): String = "preset-${preset.key.toStableSlug()}-base"

    fun baseSourceId(presetId: TilePresetId): String = baseSourceId(TilePresets.byId(presetId))

    fun overlaySourceId(presetId: TilePresetId, overlayId: String): String =
        overlaySourceId(TilePresets.byId(presetId), overlayId)

    fun overlaySourceId(preset: TilePreset, overlayId: String): String =
        "preset-${preset.key.toStableSlug()}-overlay-${overlayId.toStableSlug()}"

    private fun String.toStableSlug(): String = lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
