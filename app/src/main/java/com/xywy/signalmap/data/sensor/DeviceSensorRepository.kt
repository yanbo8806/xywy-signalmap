package com.xywy.signalmap.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.xywy.signalmap.domain.model.DeviceSensorSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

class DeviceSensorRepository(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observe(): Flow<DeviceSensorSnapshot> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        var current = DeviceSensorSnapshot.Empty
        var lastEmitMillis = 0L
        var lastEmittedKey = ""

        fun emitIfReady(force: Boolean = false) {
            val now = System.currentTimeMillis()
            val next = current.copy(observedAtMillis = now)
            val nextKey = next.sensorComparableKey()
            if (!force && now - lastEmitMillis < EMIT_INTERVAL_MILLIS) return
            if (!force && nextKey == lastEmittedKey && lastEmitMillis != 0L) return
            lastEmitMillis = now
            lastEmittedKey = nextKey
            trySend(next)
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val orientation = event.values.toOrientationDegrees()
                        current = current.copy(
                            verticalAngleDegrees = orientation.verticalAngleDegrees,
                            azimuthDegrees = orientation.azimuthDegrees,
                        )
                        emitIfReady()
                    }

                    Sensor.TYPE_PRESSURE -> {
                        val pressure = event.values.firstOrNull() ?: return
                        current = current.copy(
                            pressureHPa = pressure,
                            altitudeMeters = SensorManager.getAltitude(
                                SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                                pressure,
                            ),
                        )
                        emitIfReady()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var registered = false
        rotationSensor?.let { sensor ->
            registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) || registered
        }
        pressureSensor?.let { sensor ->
            registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) || registered
        }
        if (!registered) {
            trySend(DeviceSensorSnapshot.Empty)
        } else {
            emitIfReady(force = true)
        }
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged { previous, candidate ->
        previous.sensorComparableKey() == candidate.sensorComparableKey()
    }

    private data class OrientationDegrees(
        val verticalAngleDegrees: Float,
        val azimuthDegrees: Float,
    )

    private fun FloatArray.toOrientationDegrees(): OrientationDegrees {
        val rotationMatrix = FloatArray(9)
        val orientationRadians = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, this)
        SensorManager.getOrientation(rotationMatrix, orientationRadians)
        val azimuth = normalizeDegrees(Math.toDegrees(orientationRadians[0].toDouble()).toFloat())
        val vertical = Math.toDegrees(orientationRadians[1].toDouble()).toFloat()
        return OrientationDegrees(
            verticalAngleDegrees = vertical,
            azimuthDegrees = azimuth,
        )
    }

    private fun DeviceSensorSnapshot.sensorComparableKey(): String = listOf(
        verticalAngleDegrees?.roundToInt()?.toString().orEmpty(),
        pressureHPa?.roundToInt()?.toString().orEmpty(),
        altitudeMeters?.roundToInt()?.toString().orEmpty(),
        azimuthDegrees?.roundToInt()?.toString().orEmpty(),
    ).joinToString("|")

    private fun normalizeDegrees(value: Float): Float {
        var normalized = value % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }

    private companion object {
        const val EMIT_INTERVAL_MILLIS = 500L
    }
}
