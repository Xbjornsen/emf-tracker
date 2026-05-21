package com.xbjornsen.emftracker.data.models

import kotlin.math.sqrt

data class SensorReading(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val magnitude: Float = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
}

enum class AlertLevel(val label: String, val description: String) {
    NORMAL("Normal", "< 20 µT — typical background. Earth's field is 25–65 µT total, but most of that is static DC and harmless. AC variation in this range is well below any guideline."),
    ELEVATED("Elevated", "20–50 µT — near household appliances at arm's length (TV, fridge, laptop). ICNIRP general-public limit for 50 Hz AC exposure is 200 µT — you are still far below it."),
    HIGH("High", "50–100 µT — close to a running motor, transformer, or power cable. Still within ICNIRP limits for occasional exposure. Note: static magnets (speakers, phone cases) also read here but are not an EMF concern."),
    VERY_HIGH("Very High", "≥ 100 µT — direct contact with electrical equipment or a strong magnet. Exceeds WHO precautionary residential guideline (0.3–0.4 µT AC average). Use baseline + FFT to confirm this is AC radiation, not a static field.")
}

fun Float.toAlertLevel(): AlertLevel = when {
    this < 20f -> AlertLevel.NORMAL
    this < 50f -> AlertLevel.ELEVATED
    this < 100f -> AlertLevel.HIGH
    else -> AlertLevel.VERY_HIGH
}

data class Session(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val peak: Float,
    val average: Float,
    val readingCount: Int
)
