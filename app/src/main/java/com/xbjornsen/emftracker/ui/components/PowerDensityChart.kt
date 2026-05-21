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
import kotlin.math.log10

// Building Biology 2015 precautionary guidelines for RF power density.
// These are advisory, not regulatory — ICNIRP limits are ~10,000,000 µW/m² at 2.4 GHz.
private data class PdBand(val floorUwM2: Float, val label: String, val color: Color)
private val PD_BANDS = listOf(
    PdBand(1000f, "Extreme",  Color(0xFFF44336)),
    PdBand(10f,   "Severe",   Color(0xFFFF9800)),
    PdBand(0.1f,  "Slight",   Color(0xFFFFC107)),
    PdBand(0f,    "No concern", Color(0xFF4CAF50)),
)

fun colorForPowerDensity(uwM2: Float): Color =
    PD_BANDS.firstOrNull { uwM2 >= it.floorUwM2 }?.color ?: PD_BANDS.last().color

fun formatPowerDensity(uwM2: Float): String = when {
    uwM2 < 0.01f  -> "< 0.01 µW/m²"
    uwM2 < 10f    -> "%.2f µW/m²".format(uwM2)
    uwM2 < 1000f  -> "%.1f µW/m²".format(uwM2)
    uwM2 < 1e6f   -> "%.1f mW/m²".format(uwM2 / 1000f)
    else          -> "%.2f W/m²".format(uwM2 / 1e6f)
}

fun bandLabelForPowerDensity(uwM2: Float): String =
    PD_BANDS.firstOrNull { uwM2 >= it.floorUwM2 }?.label ?: PD_BANDS.last().label

// Log scale: y axis from 0.001 µW/m² (log = -3) to 100,000 µW/m² (log = 5), range = 8
private const val LOG_MIN = -3f
private const val LOG_MAX = 5f

private fun yOf(uwM2: Float, h: Float): Float {
    val logV = log10(uwM2.coerceAtLeast(0.0005f))
    return ((LOG_MAX - logV) / (LOG_MAX - LOG_MIN) * h).coerceIn(0f, h)
}

@Composable
fun PowerDensityChart(
    data: List<Float>,   // µW/m² values
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val w = size.width
        val h = size.height

        // Reference lines at building biology thresholds
        val refs = listOf(
            0.1f  to "0.1",
            10f   to "10",
            1000f to "1 k",
        )
        refs.forEach { (v, label) ->
            val y = yOf(v, h)
            val color = colorForPowerDensity(v)
            drawLine(
                color = color.copy(alpha = 0.30f),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            )
            drawText(
                textMeasurer, "$label µW/m²",
                topLeft = Offset(4f, y + 2f),
                style = TextStyle(color = color.copy(alpha = 0.65f), fontSize = 8.sp)
            )
        }

        // Fill under the line
        val fillPath = Path()
        data.forEachIndexed { i, v ->
            val x = i.toFloat() / (data.size - 1) * w
            val y = yOf(v, h)
            if (i == 0) { fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
            else fillPath.lineTo(x, y)
        }
        fillPath.lineTo(w, h)
        fillPath.close()
        val fillColor = colorForPowerDensity(data.last())
        drawPath(fillPath, fillColor.copy(alpha = 0.08f))

        // Line — per-segment colour by band
        for (i in 1 until data.size) {
            val x0 = (i - 1).toFloat() / (data.size - 1) * w
            val x1 = i.toFloat() / (data.size - 1) * w
            drawLine(
                color = colorForPowerDensity(data[i]),
                start = Offset(x0, yOf(data[i - 1], h)),
                end   = Offset(x1, yOf(data[i], h)),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Current value label on right edge
        val latest = data.last()
        val labelText = formatPowerDensity(latest)
        drawText(
            textMeasurer, labelText,
            topLeft = Offset(w - 72.dp.toPx(), yOf(latest, h) - 12.dp.toPx()),
            style = TextStyle(color = colorForPowerDensity(latest), fontSize = 9.sp)
        )
    }
}
