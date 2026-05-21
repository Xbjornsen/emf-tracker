package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val THRESHOLDS = listOf(
    20f  to Color(0xFF4CAF50),  // Normal   — green
    50f  to Color(0xFFFFC107),  // Elevated — amber
    100f to Color(0xFFFF9800),  // High     — orange
    Float.MAX_VALUE to Color(0xFFF44336)  // Very high — red
)

private fun colorFor(value: Float): Color =
    THRESHOLDS.first { value <= it.first }.second

@Composable
fun ThresholdBarChart(data: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.max().coerceAtLeast(20f).coerceAtLeast(100f)
        val n = data.size
        val totalGap = size.width * 0.15f
        val barWidth = (size.width - totalGap) / n
        val gap = totalGap / n

        // Threshold guide lines
        val guideThresholds = listOf(20f to Color(0xFF4CAF50), 50f to Color(0xFFFFC107), 100f to Color(0xFFFF9800))
        guideThresholds.forEach { (t, color) ->
            if (t <= maxVal) {
                val y = size.height - (t / maxVal) * size.height
                drawLine(
                    color.copy(alpha = 0.25f),
                    Offset(0f, y), Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        data.forEachIndexed { i, value ->
            val barH = (value / maxVal) * size.height
            val x = i * (barWidth + gap) + gap / 2f
            drawRect(
                color = colorFor(value),
                topLeft = Offset(x, size.height - barH),
                size = Size(barWidth, barH)
            )
        }
    }
}
