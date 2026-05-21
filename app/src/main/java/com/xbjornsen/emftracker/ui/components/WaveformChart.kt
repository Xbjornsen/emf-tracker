package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 3-point moving average — smooths sensor noise without distorting signals above a few Hz. */
private fun FloatArray.smoothed(): FloatArray {
    if (size < 3) return this
    return FloatArray(size) { i ->
        when (i) {
            0    -> (this[0] + this[1]) / 2f
            size - 1 -> (this[size - 2] + this[size - 1]) / 2f
            else -> (this[i - 1] + this[i] + this[i + 1]) / 3f
        }
    }
}

@Composable
fun WaveformChart(
    samples: FloatArray,
    sampleRateHz: Float,
    has50Hz: Boolean,
    has60Hz: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor   = Color(0xFF4FC3F7)
    val gridColor   = Color.White.copy(alpha = 0.12f)
    val labelColor  = Color.White.copy(alpha = 0.45f)
    val period50    = Color(0xFFF44336).copy(alpha = 0.55f)
    val period60    = Color(0xFFFF9800).copy(alpha = 0.55f)
    val textMeasurer = rememberTextMeasurer()

    val hasAcSignal = has50Hz || has60Hz

    Canvas(modifier = modifier) {
        if (samples.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val midY = h / 2f

        if (!hasAcSignal) {
            // No confirmed 40–70 Hz signal — draw a flat quiet line
            drawLine(
                lineColor.copy(alpha = 0.25f),
                Offset(0f, midY), Offset(w, midY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawScaleLabel(textMeasurer, "No 40–70 Hz signal", 4f, midY - 10.dp.toPx(), labelColor)
            return@Canvas
        }

        val display = samples.smoothed()
        val n = display.size

        val min   = display.min()
        val max   = display.max()
        val range = (max - min).coerceAtLeast(0.5f)
        val pad   = range * 0.12f

        fun yOf(v: Float) = h - ((v - min + pad) / (range + pad * 2f)) * h

        // Centre reference line
        drawLine(gridColor, Offset(0f, yOf((min + max) / 2f)), Offset(w, yOf((min + max) / 2f)),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))

        // Y-axis scale labels
        drawScaleLabel(textMeasurer, "%.1f µT".format(max), 4f, 2f, labelColor)
        drawScaleLabel(textMeasurer, "%.1f µT".format(min), 4f, h - 14.dp.toPx(), labelColor)

        // Period markers
        if (sampleRateHz > 110f) {
            if (has50Hz) drawPeriodMarkers(50f, sampleRateHz, n, period50)
            if (has60Hz) drawPeriodMarkers(60f, sampleRateHz, n, period60)
        }

        // Waveform
        val path = Path()
        display.forEachIndexed { i, v ->
            val x = (i.toFloat() / (n - 1)) * w
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawPeriodMarkers(freqHz: Float, sampleRateHz: Float, n: Int, color: Color) {
    val periodSamples = sampleRateHz / freqHz
    var x = periodSamples / 2f
    while (x < n) {
        val px = (x / n) * size.width
        drawLine(color, Offset(px, 0f), Offset(px, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        x += periodSamples
    }
}

private fun DrawScope.drawScaleLabel(
    measurer: TextMeasurer, text: String, x: Float, y: Float, color: Color
) {
    drawText(
        measurer, text,
        topLeft = Offset(x, y),
        style = TextStyle(color = color, fontSize = 9.sp)
    )
}
