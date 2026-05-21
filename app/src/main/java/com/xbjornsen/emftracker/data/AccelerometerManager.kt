package com.xbjornsen.emftracker.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.xbjornsen.emftracker.data.models.MotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class AccelerometerManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _motionState = MutableStateFlow(MotionState())
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private val _gravity = MutableStateFlow<FloatArray?>(null)
    val gravity: StateFlow<FloatArray?> = _gravity.asStateFlow()

    val isAvailable: Boolean get() = accelerometer != null

    fun start(delay: Int = SensorManager.SENSOR_DELAY_GAME) {
        sensorManager.registerListener(this, accelerometer, delay)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        _motionState.value = MotionState()
        _gravity.value = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        _gravity.value = floatArrayOf(ax, ay, az)
        val total = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        val motion = abs(total - 9.81f)
        _motionState.value = MotionState(motionMs2 = motion, isUnstable = motion > 0.5f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
