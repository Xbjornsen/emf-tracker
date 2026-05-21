package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(data: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val maxVal = data.max().coerceAtLeast(20f)
        val stepX = size.width / (data.size - 1).toFloat()

        val path = Path()
        data.forEachIndexed { i, value ->
            val x = i * stepX
            val y = size.height - (value / maxVal) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = Color(0xFF2196F3), style = Stroke(width = 2.dp.toPx()))

        // Threshold lines
        val thresholds = listOf(20f to Color(0xFF4CAF50), 50f to Color(0xFFFFC107), 100f to Color(0xFFF44336))
        thresholds.forEach { (threshold, color) ->
            if (threshold <= maxVal) {
                val y = size.height - (threshold / maxVal) * size.height
                drawLine(color.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
        }
    }
}
