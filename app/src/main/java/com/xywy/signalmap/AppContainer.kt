package com.xywy.signalmap

import android.content.Context
import com.xywy.signalmap.data.importer.EngineeringParamImporter
import com.xywy.signalmap.data.local.LayerStore
import com.xywy.signalmap.data.local.TestLogStore
import com.xywy.signalmap.data.location.LocationRepository
import com.xywy.signalmap.data.sensor.DeviceSensorRepository
import com.xywy.signalmap.data.settings.SettingsStore
import com.xywy.signalmap.data.telephony.TelephonyRepository
import com.xywy.signalmap.data.tile.HttpTileUpstreamFetcher
import com.xywy.signalmap.data.tile.LocalTileProxyServer
import com.xywy.signalmap.data.tile.MbtilesCacheStore
import com.xywy.signalmap.data.tile.TileCacheRepository
import com.xywy.signalmap.data.tile.TileProxyConfig
import com.xywy.signalmap.data.tile.TileSourceCatalog
import com.xywy.signalmap.map.MapGlyphRepository
import java.io.File

class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context)
    val layerStore = LayerStore(context)
    val testLogStore = TestLogStore(context)
    val engineeringParamImporter = EngineeringParamImporter()
    val locationRepository = LocationRepository(context)
    val telephonyRepository = TelephonyRepository(context)
    val deviceSensorRepository = DeviceSensorRepository(context)
    val tileCacheStore = MbtilesCacheStore(File(context.filesDir, "tile-cache"))
    private val tileCacheRepository = TileCacheRepository(
        catalog = TileSourceCatalog,
        store = tileCacheStore,
        upstreamFetcher = HttpTileUpstreamFetcher(),
    )
    private val glyphRepository = MapGlyphRepository(context)
    private val tileProxyServer = LocalTileProxyServer(tileCacheRepository, glyphResolver = glyphRepository::getGlyphs)
    @Volatile
    private var tileProxyConfigInternal: TileProxyConfig? = null

    val tileProxyConfig: TileProxyConfig?
        get() = tileProxyConfigInternal

    fun startTileProxy(): TileProxyConfig? {
        val cached = tileProxyConfigInternal
        if (cached != null) return cached
        return runCatching { tileProxyServer.start() }
            .onSuccess { tileProxyConfigInternal = it }
            .getOrNull()
    }

    fun stopTileProxy() {
        runCatching { tileProxyServer.stop() }
        tileProxyConfigInternal = null
    }

    fun shutdown() {
        telephonyRepository.close()
        stopTileProxy()
        runCatching { tileCacheStore.close() }
    }
}
