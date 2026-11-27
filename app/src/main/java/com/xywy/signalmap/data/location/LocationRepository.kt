package com.xywy.signalmap.data.location

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.xywy.signalmap.domain.model.LocationFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationRepository(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun observe(): Flow<LocationFix?> = callbackFlow {
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val providerLock = Any()
        var registeredProviders = emptySet<String>()

        fun enabledProviders(): Set<String> =
            LOCATION_PROVIDERS
                .filterTo(linkedSetOf()) { provider ->
                    runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
                }

        val listener = LocationListener { location -> trySend(location.toFix()) }

        fun sendLastKnown(providers: Set<String>) {
            val latest = providers
                .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull(Location::getTime)
                ?.toFix()
            trySend(latest)
        }

        fun refreshProviderRegistrations(force: Boolean = false) {
            synchronized(providerLock) {
                val nextProviders = enabledProviders()
                if (!force && nextProviders == registeredProviders) {
                    sendLastKnown(nextProviders)
                    return
                }
                runCatching { locationManager.removeUpdates(listener) }
                registeredProviders = emptySet()
                if (nextProviders.isEmpty()) {
                    trySend(null)
                    return
                }
                nextProviders.forEach { provider ->
                    runCatching {
                        locationManager.requestLocationUpdates(
                            provider,
                            LOCATION_UPDATE_INTERVAL_MILLIS,
                            LOCATION_UPDATE_DISTANCE_METERS,
                            listener,
                            Looper.getMainLooper(),
                        )
                    }.onSuccess {
                        registeredProviders = registeredProviders + provider
                    }
                }
                sendLastKnown(registeredProviders.ifEmpty { nextProviders })
            }
        }

        val providerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    refreshProviderRegistrations()
                }
            }
        }
        runCatching {
            context.registerReceiver(providerReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        }
        val providerPollJob = launch {
            while (isActive) {
                delay(PROVIDER_RECHECK_INTERVAL_MILLIS)
                refreshProviderRegistrations()
            }
        }

        refreshProviderRegistrations(force = true)

        awaitClose {
            providerPollJob.cancel()
            runCatching { context.unregisterReceiver(providerReceiver) }
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.distinctUntilChanged()

    private companion object {
        val LOCATION_PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 1_000L
        const val LOCATION_UPDATE_DISTANCE_METERS = 1f
        const val PROVIDER_RECHECK_INTERVAL_MILLIS = 5_000L
    }
}

private fun Location.toFix(): LocationFix = LocationFix(
    longitude = longitude,
    latitude = latitude,
    accuracyMeters = accuracy,
    timeMillis = time,
)
