package com.xbjornsen.emftracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private data class Zone(val from: Float, val to: Float, val color: Color)

private val ZONES = listOf(
    Zone(0f,   20f,  Color(0xFF4CAF50)),
    Zone(20f,  50f,  Color(0xFFFFC107)),
    Zone(50f,  100f, Color(0xFFFF9800)),
    Zone(100f, 150f, Color(0xFFF44336)),
)
private val SCALE_MAX = 150f

@Composable
fun LevelMeter(
    magnitude: Float,
    acRms: Float,
    dcField: Float,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        // Zone labels
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            listOf("Normal", "Elevated", "High", "Very High").forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(2.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val barH = size.height * 0.55f
            val barTop = 0f
            val r = CornerRadius(barH / 2)

            // Draw each zone segment
            ZONES.forEach { zone ->
                val xFrom = (zone.from / SCALE_MAX * size.width).coerceIn(0f, size.width)
                val xTo   = (zone.to   / SCALE_MAX * size.width).coerceIn(0f, size.width)
                val filled = magnitude >= zone.to
                val partial = magnitude > zone.from && magnitude < zone.to
                val fillPx = if (partial) ((magnitude - zone.from) / (zone.to - zone.from)) * (xTo - xFrom)
                             else if (filled) xTo - xFrom else 0f

                // Dim background
                drawRoundRect(
                    color = zone.color.copy(alpha = 0.18f),
                    topLeft = Offset(xFrom, barTop),
                    size = Size(xTo - xFrom, barH),
                    cornerRadius = r
                )
                // Lit fill
                if (fillPx > 0f) {
                    drawRoundRect(
                        color = zone.color,
                        topLeft = Offset(xFrom, barTop),
                        size = Size(fillPx, barH),
                        cornerRadius = r
                    )
                }
            }

            // Needle marker at current value
            val needleX = (magnitude / SCALE_MAX * size.width).coerceIn(2f, size.width - 2f)
            drawLine(
                color = Color.White,
                start = Offset(needleX, barTop - 2f),
                end = Offset(needleX, barTop + barH + 6f),
                strokeWidth = 2.5f
            )

            // AC RMS sub-bar (shows how much is changing field vs static)
            if (acRms > 0f) {
                val acX = (acRms / SCALE_MAX * size.width).coerceIn(0f, size.width)
                val acTop = barTop + barH + 6f
                val acH = size.height * 0.25f
                drawRoundRect(
                    color = Color(0xFF64B5F6).copy(alpha = 0.3f),
                    topLeft = Offset(0f, acTop),
                    size = Size(size.width, acH),
                    cornerRadius = CornerRadius(acH / 2)
                )
                drawRoundRect(
                    color = Color(0xFF64B5F6),
                    topLeft = Offset(0f, acTop),
                    size = Size(acX, acH),
                    cornerRadius = CornerRadius(acH / 2)
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(
                "AC variation: %.2f µT".format(acRms),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64B5F6).copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            Text(
                "Static field: %.1f µT".format(dcField),
                style = MaterialTheme.typography.labelSmall,
                color = onSurface.copy(alpha = 0.45f)
            )
        }
    }
}
