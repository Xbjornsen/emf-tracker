package com.xbjornsen.emftracker.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.xbjornsen.emftracker.data.models.SensorReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class MagnetometerManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _reading = MutableStateFlow(SensorReading())
    val reading: StateFlow<SensorReading> = _reading.asStateFlow()

    private val _history = MutableStateFlow<List<Float>>(emptyList())
    val history: StateFlow<List<Float>> = _history.asStateFlow()

    // FFT / waveform ring buffer
    private val ringBuffer = FloatArray(256)
    private var ringHead = 0
    private var ringCount = 0
    private var waveformCounter = 0
    private val _fftWindow = MutableStateFlow<FloatArray?>(null)
    val fftWindow: StateFlow<FloatArray?> = _fftWindow.asStateFlow()
    private val _waveformSamples = MutableStateFlow<FloatArray?>(null)
    val waveformSamples: StateFlow<FloatArray?> = _waveformSamples.asStateFlow()

    // Sample rate measurement
    private var lastEventUs = 0L
    var measuredSampleRateHz = 150f
        private set

    var motionUnstable = false

    // DC/AC separation — slow EMA tracks the static field, residual = AC component
    private var dcEma = 0f
    private var acSumSq = 0f
    private var acSampleCount = 0
    private val _dcField  = MutableStateFlow(0f)
    val dcField: StateFlow<Float> = _dcField.asStateFlow()
    private val _acRms = MutableStateFlow(0f)
    val acRms: StateFlow<Float> = _acRms.asStateFlow()

    private var lastMagnitude = 0f
    private var highRate = false
    val isAvailable: Boolean get() = magnetometer != null

    fun start() {
        val delay = if (highRate) SensorManager.SENSOR_DELAY_FASTEST else 100_000
        sensorManager.registerListener(this, magnetometer, delay)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun setHighRate(enabled: Boolean) {
        highRate = enabled
        stop()
        start()
        if (!enabled) {
            ringHead = 0
            ringCount = 0
            waveformCounter = 0
            _fftWindow.value = null
            _waveformSamples.value = null
            measuredSampleRateHz = 150f
            lastEventUs = 0L
        }
    }

    private var baseline: SensorReading? = null

    fun clearHistory() {
        _history.value = emptyList()
        lastMagnitude = 0f
        dcEma = 0f
        acSumSq = 0f
        acSampleCount = 0
        _dcField.value = 0f
        _acRms.value = 0f
    }

    fun captureBaseline() { baseline = _reading.value }
    fun clearBaseline() { baseline = null }
    val baselineValue: SensorReading? get() = baseline

    override fun onSensorChanged(event: SensorEvent) {
        val reading = SensorReading(event.values[0], event.values[1], event.values[2])

        // Measure sample rate via EWMA
        val nowUs = event.timestamp / 1_000
        if (lastEventUs != 0L) {
            val dtUs = nowUs - lastEventUs
            if (dtUs > 0) {
                val instantRate = 1_000_000f / dtUs
                measuredSampleRateHz = 0.95f * measuredSampleRateHz + 0.05f * instantRate
            }
        }
        lastEventUs = nowUs

        // DC/AC separation — alpha tuned to ~5s time constant at current sample rate
        val dcAlpha = (1f / (5f * measuredSampleRateHz.coerceAtLeast(8f))).coerceIn(0.0005f, 0.05f)
        if (dcEma == 0f) dcEma = reading.magnitude  // seed on first sample
        dcEma = dcAlpha * reading.magnitude + (1f - dcAlpha) * dcEma
        val residual = reading.magnitude - dcEma
        acSumSq += residual * residual
        acSampleCount++
        if (acSampleCount >= 32) {
            _dcField.value  = dcEma
            _acRms.value    = sqrt(acSumSq / acSampleCount)
            acSumSq         = 0f
            acSampleCount   = 0
        }

        // Ring buffer — feed raw samples, skip if motion unstable
        if (!motionUnstable) {
            synchronized(ringBuffer) {
                ringBuffer[ringHead] = reading.magnitude
                ringHead = (ringHead + 1) % 256
                if (ringCount < 256) ringCount++
                waveformCounter++
                if (ringCount == 256) {
                    val snapshot = FloatArray(256) { i -> ringBuffer[(ringHead + i) % 256] }
                    _fftWindow.value = snapshot
                    // Waveform: emit last 128 samples every 16 new samples
                    if (waveformCounter >= 16) {
                        waveformCounter = 0
                        _waveformSamples.value = snapshot.copyOfRange(128, 256)
                    }
                }
            }
        }

        // Debounced reading for display/history
        if (abs(reading.magnitude - lastMagnitude) >= 0.5f) {
            lastMagnitude = reading.magnitude
            _reading.value = reading
            _history.value = (_history.value + reading.magnitude).takeLast(600)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
