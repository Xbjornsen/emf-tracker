package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Signal quality bands (dBm — applies to both WiFi and cell)
private data class Band(val floor: Float, val label: String, val color: Color)
private val BANDS = listOf(
    Band(-60f,  "Excellent", Color(0xFF4CAF50)),
    Band(-70f,  "Good",      Color(0xFF8BC34A)),
    Band(-80f,  "Fair",      Color(0xFFFFC107)),
    Band(-90f,  "Weak",      Color(0xFFFF9800)),
    Band(-120f, "Poor",      Color(0xFFF44336)),
)

private fun colorForDbm(dbm: Float): Color =
    BANDS.firstOrNull { dbm >= it.floor }?.color ?: BANDS.last().color

@Composable
fun SignalChart(
    data: List<Float>,        // dBm values (negative)
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = Color.White.copy(alpha = 0.10f)
    val labelColor = Color.White.copy(alpha = 0.40f)

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val w = size.width
        val h = size.height

        // Fixed scale: -30 (top) to -110 (bottom)
        val scaleMax = -30f
        val scaleMin = -110f
        val scaleRange = scaleMax - scaleMin   // 80

        fun yOf(dbm: Float) = ((scaleMax - dbm) / scaleRange * h).coerceIn(0f, h)

        // Band threshold lines + labels
        BANDS.dropLast(1).forEach { band ->
            val y = yOf(band.floor)
            drawLine(
                color = band.color.copy(alpha = 0.25f),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            )
            drawText(
                textMeasurer, band.label,
                topLeft = Offset(4f, y + 2f),
                style = TextStyle(color = band.color.copy(alpha = 0.6f), fontSize = 8.sp)
            )
        }

        // Min/max labels on right edge
        val latest = data.last()
        drawText(
            textMeasurer, "${latest.toInt()} dBm",
            topLeft = Offset(w - 52.dp.toPx(), yOf(latest) - 12.dp.toPx()),
            style = TextStyle(color = colorForDbm(latest), fontSize = 9.sp)
        )

        // Fill under the line with a gradient-like tint
        val fillPath = Path()
        data.forEachIndexed { i, v ->
            val x = i.toFloat() / (data.size - 1) * w
            val y = yOf(v)
            if (i == 0) fillPath.moveTo(x, h) else Unit
            if (i == 0) fillPath.lineTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((data.size - 1).toFloat() / (data.size - 1) * w, h)
        fillPath.close()
        drawPath(fillPath, lineColor.copy(alpha = 0.08f))

        // Line — coloured per-segment by signal quality
        for (i in 1 until data.size) {
            val x0 = (i - 1).toFloat() / (data.size - 1) * w
            val x1 = i.toFloat() / (data.size - 1) * w
            val y0 = yOf(data[i - 1])
            val y1 = yOf(data[i])
            drawLine(
                color = colorForDbm(data[i]),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
