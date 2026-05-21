package com.xbjornsen.emftracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xbjornsen.emftracker.data.models.SensorReading
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val POLE_THRESHOLD_UT = 3f

enum class MagneticPole { NORTH, SOUTH, NONE }

// Which face of the phone the dominant field is coming from
enum class ApproachAxis(val label: String) {
    SCREEN_FACE("Screen face"),
    BACK("Back"),
    RIGHT_EDGE("Right edge"),
    LEFT_EDGE("Left edge"),
    TOP_EDGE("Top edge"),
    BOTTOM_EDGE("Bottom edge"),
}

data class PoleResult(
    val pole: MagneticPole,
    val approach: ApproachAxis,
    val strengthUt: Float,
    val proximity: String   // Very close / Close / Moderate
)

fun SensorReading.analyzePole(): PoleResult {
    if (magnitude < POLE_THRESHOLD_UT) return PoleResult(MagneticPole.NONE, ApproachAxis.SCREEN_FACE, magnitude, "")

    // Dominant axis determines approach direction and pole
    val axX = abs(x); val axY = abs(y); val axZ = abs(z)
    val (dominant, approach) = when {
        axZ >= axX && axZ >= axY -> z to if (z > 0) ApproachAxis.SCREEN_FACE else ApproachAxis.BACK
        axX >= axY               -> x to if (x > 0) ApproachAxis.RIGHT_EDGE  else ApproachAxis.LEFT_EDGE
        else                     -> y to if (y > 0) ApproachAxis.TOP_EDGE    else ApproachAxis.BOTTOM_EDGE
    }

    val pole = if (dominant > 0) MagneticPole.NORTH else MagneticPole.SOUTH

    // Dipole field falls as 1/r³ — qualitative proximity from field strength
    val proximity = when {
        magnitude > 200f -> "Extremely close"
        magnitude > 80f  -> "Very close"
        magnitude > 30f  -> "Close"
        magnitude > 10f  -> "Moderate"
        else             -> "Distant"
    }

    return PoleResult(pole, approach, magnitude, proximity)
}

@Composable
fun PoleDetector(delta: SensorReading, modifier: Modifier = Modifier) {
    val result = delta.analyzePole()
    val visible = result.pole != MagneticPole.NONE

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        val isNorth   = result.pole == MagneticPole.NORTH
        val poleColor = if (isNorth) Color(0xFFE53935) else Color(0xFF1E88E5)
        val poleName  = if (isNorth) "NORTH POLE" else "SOUTH POLE"
        val fieldNote = if (isNorth) "Field lines exit toward sensor" else "Field lines converge away from sensor"

        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.35f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800, easing = { it }), RepeatMode.Reverse),
            label = "pulseAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(poleColor.copy(alpha = 0.08f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated field-line diagram
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawFieldLines(isNorth, poleColor, pulse)
                    }
                    Text(
                        text = if (isNorth) "N" else "S",
                        color = poleColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        poleName,
                        style = MaterialTheme.typography.titleMedium,
                        color = poleColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        fieldNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(6.dp))
                    // Proximity label + strength
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            result.proximity,
                            style = MaterialTheme.typography.labelMedium,
                            color = poleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("·", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Text(
                            "Δ %.1f µT".format(result.strengthUt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Proximity bar
            val proximityFraction = (result.strengthUt / 300f).coerceIn(0f, 1f)
            Text(
                "Signal strength",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { proximityFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = poleColor,
                trackColor = poleColor.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(10.dp))

            // Approach direction row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Approaching from:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                Text(
                    result.approach.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = poleColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Axis breakdown
            Spacer(Modifier.height(8.dp))
            AxisBar("X", delta.x, poleColor)
            Spacer(Modifier.height(4.dp))
            AxisBar("Y", delta.y, poleColor)
            Spacer(Modifier.height(4.dp))
            AxisBar("Z", delta.z, poleColor)
        }
    }
}

@Composable
private fun AxisBar(label: String, value: Float, accent: Color) {
    val maxRange = 300f
    val fraction = (abs(value) / maxRange).coerceIn(0f, 1f)
    val isPositive = value >= 0f
    val barColor = if (isPositive) accent else accent.copy(alpha = 0.6f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
            Box(modifier = Modifier
                .fillMaxWidth(fraction)
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor))
        }
        Text("%.1f".format(value),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.width(48.dp))
    }
}

private fun DrawScope.drawFieldLines(isNorth: Boolean, color: Color, alpha: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r  = size.minDimension / 2f

    repeat(8) { i ->
        val angle = i * Math.PI / 4
        val cosA = cos(angle).toFloat()
        val sinA = sin(angle).toFloat()
        val inner = r * 0.30f
        val outer = r * 0.88f

        val sx = cx + cosA * (if (isNorth) inner else outer)
        val sy = cy + sinA * (if (isNorth) inner else outer)
        val ex = cx + cosA * (if (isNorth) outer else inner)
        val ey = cy + sinA * (if (isNorth) outer else inner)

        drawLine(color.copy(alpha = alpha * 0.75f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = 1.8f, cap = StrokeCap.Round)

        // Arrowhead
        val dx = (ex - sx) / r
        val dy = (ey - sy) / r
        val hl = r * 0.13f
        val ha = 0.45f
        val tip = Offset(ex, ey)
        fun head(sign: Int) = Offset(
            ex - hl * (dx * cos(ha.toDouble()).toFloat() - sign * dy * sin(ha.toDouble()).toFloat()),
            ey - hl * (dy * cos(ha.toDouble()).toFloat() + sign * dx * sin(ha.toDouble()).toFloat())
        )
        drawLine(color.copy(alpha = alpha), tip, head(-1), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = alpha), tip, head(+1), strokeWidth = 1.5f, cap = StrokeCap.Round)
    }

    drawCircle(color.copy(alpha = alpha * 0.18f), r * 0.28f, Offset(cx, cy))
    drawCircle(color.copy(alpha = alpha * 0.7f),  r * 0.28f, Offset(cx, cy), style = Stroke(2.2f))
}
