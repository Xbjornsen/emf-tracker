package com.xbjornsen.emftracker.data.models

data class FftPeak(val frequencyHz: Float, val normalizedMagnitude: Float)

data class FftResult(
    val dominantFrequencyHz: Float,
    val has50Hz: Boolean,
    val has60Hz: Boolean,
    val bins: List<Float>,
    val sampleRateHz: Float,
    val peaks: List<FftPeak> = emptyList()
)

data class MotionState(
    val motionMs2: Float = 0f,
    val isUnstable: Boolean = false
)

// Estimated from received signal power via antenna effective aperture (PD = Pr * 4π / λ²).
// Assumes isotropic receive antenna. Uncertainty ±10–20 dB due to antenna gain pattern.
data class WifiPowerEstimate(
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val powerDensityUwM2: Float   // microwatts per square metre
)

data class RssiState(
    val wifiPower: WifiPowerEstimate? = null
)
