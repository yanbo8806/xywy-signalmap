package com.xywy.signalmap

import android.app.Application
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class SignalMapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this, "", WellKnownTileServer.MapLibre)
        container = AppContainer(this)
        container.startTileProxy()
    }

    override fun onTerminate() {
        runCatching { container.shutdown() }
        super.onTerminate()
    }
}
