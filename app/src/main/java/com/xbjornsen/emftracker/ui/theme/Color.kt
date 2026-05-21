package com.xbjornsen.emftracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.xbjornsen.emftracker.data.models.AlertLevel

val NormalColor = Color(0xFF4CAF50)
val ElevatedColor = Color(0xFFFFC107)
val HighColor = Color(0xFFFF9800)
val VeryHighColor = Color(0xFFF44336)

val Primary = Color(0xFF1976D2)
val Background = Color(0xFF0D1117)
val Surface = Color(0xFF161B22)
val OnSurface = Color(0xFFE6EDF3)

fun AlertLevel.color(): Color = when (this) {
    AlertLevel.NORMAL -> NormalColor
    AlertLevel.ELEVATED -> ElevatedColor
    AlertLevel.HIGH -> HighColor
    AlertLevel.VERY_HIGH -> VeryHighColor
}
