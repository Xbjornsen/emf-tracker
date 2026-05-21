package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun FftBarChart(
    bins: List<Float>,
    dominantHz: Float,
    has50Hz: Boolean,
    has60Hz: Boolean,
    sampleRateHz: Float,
    modifier: Modifier = Modifier
) {
    val barColor    = Color(0xFF2196F3)
    val hitColor    = Color(0xFFF44336)
    val zoneColor   = Color(0xFFFFC107).copy(alpha = 0.07f)  // subtle 40–70 Hz highlight
    val refColor    = Color.White.copy(alpha = 0.30f)
    val labelColor  = Color.White.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        if (bins.isEmpty()) return@Canvas

        val labelAreaH = 16.dp.toPx()
        val chartH     = size.height - labelAreaH
        val nyquist    = sampleRateHz / 2f

        val displayBins = bins.take(bins.size.coerceAtMost(128))
        val barWidth = size.width / displayBins.size
        val gap      = (barWidth * 0.15f).coerceAtMost(2.dp.toPx())

        val bin50 = (50f / nyquist * displayBins.size).roundToInt()
        val bin60 = (60f / nyquist * displayBins.size).roundToInt()

        // 40–70 Hz zone highlight
        val x40 = (40f / nyquist * size.width).coerceIn(0f, size.width)
        val x70 = (70f / nyquist * size.width).coerceIn(0f, size.width)
        if (x70 > x40) {
            drawRect(zoneColor, topLeft = Offset(x40, 0f), size = Size(x70 - x40, chartH))
        }

        // Bars
        displayBins.forEachIndexed { i, mag ->
            val isHit = (has50Hz && i in (bin50 - 2)..(bin50 + 2)) ||
                        (has60Hz && i in (bin60 - 2)..(bin60 + 2))
            val barH = mag * chartH
            drawRect(
                color = if (isHit) hitColor else barColor,
                topLeft = Offset(i * barWidth + gap / 2, chartH - barH),
                size = Size(barWidth - gap, barH)
            )
        }

        // Reference lines at 50 and 60 Hz
        drawRefLine(bin50, displayBins.size, chartH, refColor)
        drawRefLine(bin60, displayBins.size, chartH, refColor)

        // X-axis labels every 10 Hz up to Nyquist
        val stepHz = if (nyquist <= 60f) 10f else 20f
        var labelHz = stepHz
        while (labelHz <= nyquist) {
            val x = labelHz / nyquist * size.width
            // Tick mark
            drawLine(labelColor, Offset(x, chartH), Offset(x, chartH + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
            // Label text
            val label = "${labelHz.roundToInt()}"
            val measured = textMeasurer.measure(label, style = TextStyle(color = labelColor, fontSize = 8.sp))
            val lx = (x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
            drawText(measured, topLeft = Offset(lx, chartH + 5.dp.toPx()))
            labelHz += stepHz
        }
    }
}

private fun DrawScope.drawRefLine(bin: Int, totalBins: Int, chartH: Float, color: Color) {
    if (bin < 0 || bin >= totalBins) return
    val x = bin.toFloat() / totalBins * size.width
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end   = Offset(x, chartH),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
    )
}
