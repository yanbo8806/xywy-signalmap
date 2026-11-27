package com.xywy.signalmap.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xywy.signalmap.domain.model.EngineeringLayerBinding
import com.xywy.signalmap.domain.model.EngineeringLayerBindingMode
import com.xywy.signalmap.domain.model.CustomTileSourceConfig
import com.xywy.signalmap.domain.model.MapControlPreferences
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimEngineeringBindings
import com.xywy.signalmap.domain.model.SimTechEngineeringBinding
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "signal_map_settings")

class SettingsStore(private val context: Context) {
    private val tilePresetKey = stringPreferencesKey("tile_preset")
    private val tileSourceKey = stringPreferencesKey("tile_source_key")
    private val customTileSourcesKey = stringPreferencesKey("custom_tile_sources_json")
    private val importGcj02Key = booleanPreferencesKey("import_gcj02_to_wgs84")
    private val selectedTechsKey = stringSetPreferencesKey(MapControlPreferencesCodec.SELECTED_TECHS)
    private val selectedBandsKey = stringSetPreferencesKey(MapControlPreferencesCodec.SELECTED_BANDS)
    private val showSectorFillKey = booleanPreferencesKey(MapControlPreferencesCodec.SHOW_SECTOR_FILL)
    private val showSitePointsKey = booleanPreferencesKey(MapControlPreferencesCodec.SHOW_SITE_POINTS)
    private val showSiteLabelsKey = booleanPreferencesKey(MapControlPreferencesCodec.SHOW_SITE_LABELS)
    private val nearbyRecordLimitKey = intPreferencesKey(MapControlPreferencesCodec.NEARBY_RECORD_LIMIT)
    private val mapDisplayModeKey = stringPreferencesKey(MapControlPreferencesCodec.MAP_DISPLAY_MODE)
    private val testMetricModeKey = stringPreferencesKey(MapControlPreferencesCodec.TEST_METRIC_MODE)

    val tilePresetKeyFlow: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[tileSourceKey]
            ?: preferences[tilePresetKey]?.let { legacy ->
                runCatching { TilePresetId.valueOf(legacy) }.getOrNull()
                    ?.takeIf { it != TilePresetId.OPEN_STREET_MAP }
                    ?.let(TilePresets::keyOf)
            }
            ?: TilePresets.keyOf(TilePresetId.AMAP_ROAD)
    }

    val tilePresetFlow: Flow<TilePresetId> = tilePresetKeyFlow.map { key ->
        TilePresets.legacyKeyToPreset(key)?.id ?: TilePresetId.AMAP_ROAD
    }

    val customTileSourcesFlow: Flow<List<CustomTileSourceConfig>> = context.settingsDataStore.data.map { preferences ->
        decodeCustomTileSources(preferences[customTileSourcesKey])
    }

    val importGcj02ToWgs84Flow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[importGcj02Key] ?: false
    }

    val mapControlPreferencesFlow: Flow<MapControlPreferences> = context.settingsDataStore.data.map { preferences ->
        MapControlPreferencesCodec.decode(
            raw = buildMap {
                put(MapControlPreferencesCodec.SELECTED_TECHS, preferences[selectedTechsKey])
                put(MapControlPreferencesCodec.SELECTED_BANDS, preferences[selectedBandsKey])
                put(MapControlPreferencesCodec.SHOW_SECTOR_FILL, preferences[showSectorFillKey])
                put(MapControlPreferencesCodec.SHOW_SITE_POINTS, preferences[showSitePointsKey])
                put(MapControlPreferencesCodec.SHOW_SITE_LABELS, preferences[showSiteLabelsKey])
                put(MapControlPreferencesCodec.NEARBY_RECORD_LIMIT, preferences[nearbyRecordLimitKey])
                put(MapControlPreferencesCodec.MAP_DISPLAY_MODE, preferences[mapDisplayModeKey])
                put(MapControlPreferencesCodec.TEST_METRIC_MODE, preferences[testMetricModeKey])
                slotIndexes.forEach { slotIndex ->
                    val servingKey = MapControlPreferencesCodec.simServingLineKey(slotIndex)
                    val neighborKey = MapControlPreferencesCodec.simNeighborLineKey(slotIndex)
                    val testPointsKey = MapControlPreferencesCodec.simTestPointsKey(slotIndex)
                    put(servingKey, preferences[booleanPreferencesKey(servingKey)])
                    put(neighborKey, preferences[booleanPreferencesKey(neighborKey)])
                    put(testPointsKey, preferences[booleanPreferencesKey(testPointsKey)])
                }
            },
            slotIndexes = slotIndexes,
        )
    }

    val simEngineeringBindingsFlow: Flow<SimEngineeringBindings> = context.settingsDataStore.data.map { preferences ->
        SimEngineeringBindings(
            perSlot = buildMap {
                slotIndexes.forEach { slotIndex ->
                    put(
                        slotIndex,
                        SimTechEngineeringBinding(
                            nr = preferences[bindingPreferenceKey(slotIndex, RadioTechnology.NR)].decodeBindingPreference(),
                            lte = preferences[bindingPreferenceKey(slotIndex, RadioTechnology.LTE)].decodeBindingPreference(),
                        ),
                    )
                }
            },
        )
    }

    suspend fun setTilePreset(tilePresetId: TilePresetId) {
        setTilePresetKey(TilePresets.keyOf(tilePresetId))
    }

    suspend fun setTilePresetKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[tileSourceKey] = key
        }
    }

    suspend fun setCustomTileSources(sources: List<CustomTileSourceConfig>) {
        context.settingsDataStore.edit { preferences ->
            preferences[customTileSourcesKey] = encodeCustomTileSources(sources)
        }
    }

    suspend fun upsertCustomTileSource(source: CustomTileSourceConfig) {
        context.settingsDataStore.edit { preferences ->
            val current = decodeCustomTileSources(preferences[customTileSourcesKey])
            val next = current
                .filterNot { it.key == source.key }
                .plus(source)
            preferences[customTileSourcesKey] = encodeCustomTileSources(next)
            preferences[tileSourceKey] = source.key
        }
    }

    suspend fun deleteCustomTileSource(key: String) {
        context.settingsDataStore.edit { preferences ->
            val current = decodeCustomTileSources(preferences[customTileSourcesKey])
            preferences[customTileSourcesKey] = encodeCustomTileSources(current.filterNot { it.key == key })
            if (preferences[tileSourceKey] == key) {
                preferences[tileSourceKey] = TilePresets.keyOf(TilePresetId.AMAP_ROAD)
            }
        }
    }

    suspend fun setImportGcj02ToWgs84(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[importGcj02Key] = enabled
        }
    }

    suspend fun setMapControlPreferences(mapControlPreferences: MapControlPreferences) {
        val encoded = MapControlPreferencesCodec.encode(mapControlPreferences)
        context.settingsDataStore.edit { preferences ->
            preferences[selectedTechsKey] = encoded.getStringSet(MapControlPreferencesCodec.SELECTED_TECHS)
            preferences[selectedBandsKey] = encoded.getStringSet(MapControlPreferencesCodec.SELECTED_BANDS)
            preferences[showSectorFillKey] = encoded.getBoolean(MapControlPreferencesCodec.SHOW_SECTOR_FILL)
            preferences[showSitePointsKey] = encoded.getBoolean(MapControlPreferencesCodec.SHOW_SITE_POINTS)
            preferences[showSiteLabelsKey] = encoded.getBoolean(MapControlPreferencesCodec.SHOW_SITE_LABELS)
            preferences[nearbyRecordLimitKey] = encoded.getValue(MapControlPreferencesCodec.NEARBY_RECORD_LIMIT) as Int
            preferences[mapDisplayModeKey] = encoded.getString(MapControlPreferencesCodec.MAP_DISPLAY_MODE)
            preferences[testMetricModeKey] = encoded.getString(MapControlPreferencesCodec.TEST_METRIC_MODE)
            slotIndexes.forEach { slotIndex ->
                val servingKey = MapControlPreferencesCodec.simServingLineKey(slotIndex)
                val neighborKey = MapControlPreferencesCodec.simNeighborLineKey(slotIndex)
                val testPointsKey = MapControlPreferencesCodec.simTestPointsKey(slotIndex)
                preferences[booleanPreferencesKey(servingKey)] = encoded.getBoolean(servingKey)
                preferences[booleanPreferencesKey(neighborKey)] = encoded.getBoolean(neighborKey)
                preferences[booleanPreferencesKey(testPointsKey)] = encoded.getBoolean(testPointsKey)
            }
        }
    }

    suspend fun setSimEngineeringBinding(
        slotIndex: Int,
        technology: RadioTechnology,
        binding: EngineeringLayerBinding,
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[bindingPreferenceKey(slotIndex, technology)] = binding.encode()
        }
    }

    private fun bindingPreferenceKey(
        slotIndex: Int,
        technology: RadioTechnology,
    ) = stringPreferencesKey("sim_${slotIndex}_${technology.name.lowercase()}_binding")

    private fun String?.decodeBindingPreference(): EngineeringLayerBinding {
        val value = this?.trim().orEmpty()
        return when {
            value.equals(BINDING_AUTO, ignoreCase = true) || value.isBlank() -> EngineeringLayerBinding.Auto
            value.equals(BINDING_DISABLED, ignoreCase = true) -> EngineeringLayerBinding.Disabled
            value.startsWith(BINDING_LAYER_PREFIX) -> value.removePrefix(BINDING_LAYER_PREFIX)
                .takeIf(String::isNotBlank)
                ?.let(EngineeringLayerBinding::specificLayer)
                ?: EngineeringLayerBinding.Auto
            else -> EngineeringLayerBinding.Auto
        }
    }

    private fun EngineeringLayerBinding.encode(): String = when (mode) {
        EngineeringLayerBindingMode.AUTO -> BINDING_AUTO
        EngineeringLayerBindingMode.DISABLED -> BINDING_DISABLED
        EngineeringLayerBindingMode.SPECIFIC_LAYER -> layerId
            ?.takeIf(String::isNotBlank)
            ?.let { BINDING_LAYER_PREFIX + it }
            ?: BINDING_AUTO
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.getStringSet(key: String): Set<String> =
        this[key] as? Set<String> ?: emptySet()

    private fun Map<String, Any>.getBoolean(key: String): Boolean =
        this[key] as? Boolean ?: false

    private fun Map<String, Any>.getString(key: String): String =
        this[key] as? String ?: ""

    private fun encodeCustomTileSources(sources: List<CustomTileSourceConfig>): String =
        settingsJson.encodeToString(ListSerializer(CustomTileSourceConfig.serializer()), sources)

    private fun decodeCustomTileSources(raw: String?): List<CustomTileSourceConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            settingsJson.decodeFromString(ListSerializer(CustomTileSourceConfig.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val settingsJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        private val slotIndexes = listOf(0, 1)
        private const val BINDING_AUTO = "AUTO"
        private const val BINDING_DISABLED = "DISABLED"
        private const val BINDING_LAYER_PREFIX = "LAYER:"
    }
}
