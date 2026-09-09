package com.freedu.personalgallary.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Lightweight shake detector (shuffle gesture). Register only while the grid is visible. */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeMs = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        val g = sqrt((x * x + y * y + z * z).toDouble())
        if (g > SHAKE_G) {
            val now = System.currentTimeMillis()
            if (now - lastShakeMs > 900) {
                lastShakeMs = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_G = 2.6
    }
}
