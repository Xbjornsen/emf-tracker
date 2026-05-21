package com.xbjornsen.emftracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xbjornsen.emftracker.data.AccelerometerManager
import com.xbjornsen.emftracker.data.FftAnalyzer
import com.xbjornsen.emftracker.data.MagnetometerManager
import com.xbjornsen.emftracker.data.RssiManager
import com.xbjornsen.emftracker.data.SessionRepository
import com.xbjornsen.emftracker.data.models.FftResult
import com.xbjornsen.emftracker.data.models.MotionState
import com.xbjornsen.emftracker.data.models.RssiState
import com.xbjornsen.emftracker.data.models.Session
import com.xbjornsen.emftracker.data.models.SensorReading
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val magnetometerManager = MagnetometerManager(application)
    private val accelerometerManager = AccelerometerManager(application)
    private val rssiManager = RssiManager(application)
    private val repository = SessionRepository(application)

    // Raw sensor data
    val reading = magnetometerManager.reading
    val history = magnetometerManager.history
    val isSensorAvailable = magnetometerManager.isAvailable

    // ── Feature toggles ──────────────────────────────────────────────────────
    private val _baselineEnabled = MutableStateFlow(false)
    val baselineEnabled: StateFlow<Boolean> = _baselineEnabled.asStateFlow()

    private val _fftEnabled = MutableStateFlow(false)
    val fftEnabled: StateFlow<Boolean> = _fftEnabled.asStateFlow()

    private val _motionEnabled = MutableStateFlow(false)
    val motionEnabled: StateFlow<Boolean> = _motionEnabled.asStateFlow()

    private val _rssiEnabled = MutableStateFlow(false)
    val rssiEnabled: StateFlow<Boolean> = _rssiEnabled.asStateFlow()

    // ── Baseline ─────────────────────────────────────────────────────────────
    private val _baselineReading = MutableStateFlow<SensorReading?>(null)

    val displayReading: StateFlow<SensorReading> = combine(
        reading, _baselineEnabled, _baselineReading
    ) { raw, enabled, baseline ->
        if (enabled && baseline != null)
            SensorReading(raw.x - baseline.x, raw.y - baseline.y, raw.z - baseline.z)
        else raw
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SensorReading())

    // ── FFT ──────────────────────────────────────────────────────────────────
    private val _fftResult = MutableStateFlow<FftResult?>(null)
    val fftResult: StateFlow<FftResult?> = _fftResult.asStateFlow()

    val waveformSamples: StateFlow<FloatArray?> = magnetometerManager.waveformSamples
    val dcField: StateFlow<Float> = magnetometerManager.dcField
    val acRms: StateFlow<Float>   = magnetometerManager.acRms

    val measuredSampleRateHz: Float get() = magnetometerManager.measuredSampleRateHz

    // ── Motion ───────────────────────────────────────────────────────────────
    private val _motionState = MutableStateFlow(MotionState())
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    // ── RSSI ─────────────────────────────────────────────────────────────────
    val rssiState: StateFlow<RssiState> = rssiManager.rssiState
    val powerHistory: StateFlow<List<Float>> = rssiManager.powerHistory
    private var rssiJob: Job? = null
    private var locationPermissionGranted = false

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    // ── Compass ──────────────────────────────────────────────────────────────
    // Tilt-compensated when accelerometer is running; falls back to flat-phone atan2.
    val heading: StateFlow<Float> = combine(reading, accelerometerManager.gravity) { mag, grav ->
        if (grav != null) {
            val rotMatrix = FloatArray(9)
            val magnetic = floatArrayOf(mag.x, mag.y, mag.z)
            if (SensorManager.getRotationMatrix(rotMatrix, null, grav, magnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotMatrix, orientation)
                (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
            } else flatHeading(mag.x, mag.y)
        } else flatHeading(mag.x, mag.y)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    // True if the phone is tilted so far that compass accuracy degrades
    val isCompassTilted: StateFlow<Boolean> = combine(
        accelerometerManager.gravity, accelerometerManager.motionState
    ) { g, _ ->
        if (g == null) false
        else {
            val mag = sqrt((g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).toDouble()).toFloat()
            mag > 0f && (g[2] / mag) < 0.3f   // tilted > ~72° from horizontal
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var compassModeActive = false

    fun setCompassMode(enabled: Boolean) {
        compassModeActive = enabled
        if (enabled) {
            accelerometerManager.start(SensorManager.SENSOR_DELAY_GAME)
        } else if (!_motionEnabled.value && !_fftEnabled.value) {
            accelerometerManager.stop()
        }
    }

    // ── Sessions ─────────────────────────────────────────────────────────────
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(false)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private var sessionStart = 0L
    private val sessionReadings = mutableListOf<Float>()

    // ── Init ─────────────────────────────────────────────────────────────────
    init {
        magnetometerManager.start()
        viewModelScope.launch { _sessions.value = repository.getSessions() }

        // Session accumulator uses raw reading
        viewModelScope.launch {
            reading.collect { r ->
                if (_isRecording.value) sessionReadings.add(r.magnitude)
            }
        }

        // FFT pipeline — runs on Default dispatcher, never blocks the main thread
        viewModelScope.launch(Dispatchers.Default) {
            magnetometerManager.fftWindow.filterNotNull().collect { window ->
                if (_fftEnabled.value) {
                    try {
                        val rate = magnetometerManager.measuredSampleRateHz
                        val result = FftAnalyzer.analyze(window, rate)
                        _fftResult.value = result
                    } catch (_: Exception) { /* ignore bad windows */ }
                }
            }
        }

        // Motion → magnetometer unstable flag
        viewModelScope.launch {
            accelerometerManager.motionState.collect { state ->
                _motionState.value = state
                magnetometerManager.motionUnstable = state.isUnstable
            }
        }
    }

    // ── Toggle functions ─────────────────────────────────────────────────────

    fun toggleBaseline() {
        if (_baselineEnabled.value) {
            _baselineEnabled.value = false
            _baselineReading.value = null
            magnetometerManager.clearBaseline()
        } else {
            magnetometerManager.captureBaseline()
            _baselineReading.value = reading.value
            _baselineEnabled.value = true
        }
    }

    fun toggleFft() {
        if (_fftEnabled.value) {
            _fftEnabled.value = false
            magnetometerManager.setHighRate(false)
            _fftResult.value = null
            if (!_motionEnabled.value) accelerometerManager.stop()
        } else {
            _fftEnabled.value = true
            magnetometerManager.setHighRate(true)
            if (_motionEnabled.value) accelerometerManager.start(0)
        }
    }

    fun toggleMotion() {
        if (_motionEnabled.value) {
            _motionEnabled.value = false
            if (!_fftEnabled.value) accelerometerManager.stop()
            magnetometerManager.motionUnstable = false
            _motionState.value = MotionState()
        } else {
            _motionEnabled.value = true
            val delay = if (_fftEnabled.value) 0 else android.hardware.SensorManager.SENSOR_DELAY_GAME
            accelerometerManager.start(delay)
        }
    }

    fun toggleRssi() {
        if (_rssiEnabled.value) {
            _rssiEnabled.value = false
            rssiJob?.cancel()
            rssiManager.stopWifiCallback()
        } else {
            _rssiEnabled.value = true
            if (locationPermissionGranted) rssiManager.startWifiCallback()
            rssiJob = viewModelScope.launch {
                while (isActive) {
                    rssiManager.refresh(rssiManager.getWifiRssi())
                    delay(2_000)
                }
            }
        }
    }

    fun notifyPermissions(location: Boolean) {
        locationPermissionGranted = location
        _permissionsGranted.value = location
        if (location && _rssiEnabled.value) rssiManager.startWifiCallback()
    }

    // ── Session control ───────────────────────────────────────────────────────

    fun startSession() {
        sessionReadings.clear()
        sessionStart = System.currentTimeMillis()
        _isRecording.value = true
    }

    fun stopSession() {
        _isRecording.value = false
        if (sessionReadings.isEmpty()) return
        val session = Session(
            id = UUID.randomUUID().toString(),
            startTime = sessionStart,
            endTime = System.currentTimeMillis(),
            peak = sessionReadings.max(),
            average = sessionReadings.average().toFloat(),
            readingCount = sessionReadings.size
        )
        viewModelScope.launch {
            repository.saveSession(session)
            _sessions.value = repository.getSessions()
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
            _sessions.value = repository.getSessions()
        }
    }

    fun refreshReadings() {
        magnetometerManager.clearHistory()
        _fftResult.value = null
        rssiManager.clearHistory()
    }

    fun toggleHaptic() { _hapticEnabled.value = !_hapticEnabled.value }

    override fun onCleared() {
        super.onCleared()
        magnetometerManager.stop()
        accelerometerManager.stop()
        rssiManager.stopWifiCallback()
        rssiJob?.cancel()
    }

    private fun flatHeading(x: Float, y: Float): Float =
        (Math.toDegrees(atan2(-y.toDouble(), x.toDouble())).toFloat() + 360f) % 360f
}
