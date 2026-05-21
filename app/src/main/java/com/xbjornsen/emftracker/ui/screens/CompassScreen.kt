package com.xbjornsen.emftracker.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xbjornsen.emftracker.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

private val NorthRed   = Color(0xFFE53935)
private val RoseWhite  = Color.White
private val RoseDim    = Color.White.copy(alpha = 0.35f)
private val RoseBg     = Color(0xFF0D1B2A)

@Composable
fun CompassScreen(viewModel: MainViewModel) {
    val heading by viewModel.heading.collectAsState()
    val isTilted by viewModel.isCompassTilted.collectAsState()

    DisposableEffect(Unit) {
        viewModel.setCompassMode(true)
        onDispose { viewModel.setCompassMode(false) }
    }

    // Smooth heading with shortest-path wrap handling
    var accumulated by remember { mutableFloatStateOf(0f) }
    var lastHeading by remember { mutableFloatStateOf(heading) }
    LaunchedEffect(heading) {
        var delta = heading - lastHeading
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        accumulated += delta
        lastHeading = heading
    }
    val smoothed by animateFloatAsState(
        targetValue = accumulated,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "compassRotation"
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val roseSize = (minWidth * 0.88f).coerceIn(240.dp, 380.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Compass", style = MaterialTheme.typography.titleLarge)

            Box(contentAlignment = Alignment.Center) {
                // Rotating rose
                Canvas(modifier = Modifier.size(roseSize)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    rotate(-smoothed, Offset(cx, cy)) {
                        drawRose(textMeasurer, cx, cy)
                    }

                    // Fixed lubber line — red triangle at 12 o'clock, always upright
                    drawLubberLine(cx, cy)
                }
            }

            // Heading readout
            Text(
                "%.0f°".format((heading + 360f) % 360f),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                headingToCardinal(heading),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            if (isTilted) {
                Text(
                    "Hold phone flatter for best accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB300),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun DrawScope.drawRose(textMeasurer: androidx.compose.ui.text.TextMeasurer, cx: Float, cy: Float) {
    val r = size.minDimension / 2f

    // Background disc
    drawCircle(RoseBg, r * 0.97f, Offset(cx, cy))
    drawCircle(RoseWhite.copy(alpha = 0.12f), r * 0.97f, Offset(cx, cy), style = Stroke(1.5.dp.toPx()))

    // Inner reference circle
    drawCircle(RoseWhite.copy(alpha = 0.06f), r * 0.55f, Offset(cx, cy), style = Stroke(1.dp.toPx()))

    // Tick marks — 0° = North = top (12 o'clock)
    for (deg in 0 until 360 step 5) {
        val isCardinal   = deg % 90 == 0
        val isOrdinal    = deg % 45 == 0 && !isCardinal
        val isTenDeg     = deg % 10 == 0 && !isOrdinal && !isCardinal
        val rad = Math.toRadians(deg.toDouble())
        val sinD = sin(rad).toFloat()
        val cosD = cos(rad).toFloat()

        val innerR = when {
            isCardinal -> r * 0.72f
            isOrdinal  -> r * 0.78f
            isTenDeg   -> r * 0.83f
            else       -> r * 0.87f
        }
        val outerR = r * 0.93f
        val sw = when {
            isCardinal -> 2.5.dp.toPx()
            isOrdinal  -> 1.8.dp.toPx()
            else       -> 1.dp.toPx()
        }
        val color = if (isCardinal || isOrdinal) RoseWhite.copy(alpha = 0.85f) else RoseDim

        drawLine(
            color = color,
            start = Offset(cx + sinD * innerR, cy - cosD * innerR),
            end   = Offset(cx + sinD * outerR, cy - cosD * outerR),
            strokeWidth = sw
        )
    }

    // Cardinal labels: N E S W
    val cardinals = listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
    val labelR = r * 0.60f
    cardinals.forEach { (deg, label) ->
        val rad = Math.toRadians(deg.toDouble())
        val lx = cx + sin(rad).toFloat() * labelR
        val ly = cy - cos(rad).toFloat() * labelR
        val isNorth = deg == 0
        val measured = textMeasurer.measure(
            label,
            style = TextStyle(
                color = if (isNorth) NorthRed else RoseWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        drawText(
            measured,
            topLeft = Offset(lx - measured.size.width / 2f, ly - measured.size.height / 2f)
        )
    }

    // Ordinal labels: NE SE SW NW
    val ordinals = listOf(45 to "NE", 135 to "SE", 225 to "SW", 315 to "NW")
    val ordinalR = r * 0.60f
    ordinals.forEach { (deg, label) ->
        val rad = Math.toRadians(deg.toDouble())
        val lx = cx + sin(rad).toFloat() * ordinalR
        val ly = cy - cos(rad).toFloat() * ordinalR
        val measured = textMeasurer.measure(
            label,
            style = TextStyle(color = RoseWhite.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        )
        drawText(
            measured,
            topLeft = Offset(lx - measured.size.width / 2f, ly - measured.size.height / 2f)
        )
    }

    // North/South needle through the centre
    val needleLen = r * 0.48f
    val needleWidth = 5.dp.toPx()
    // Red North half
    drawLine(NorthRed, Offset(cx, cy), Offset(cx, cy - needleLen), strokeWidth = needleWidth)
    // White South half
    drawLine(RoseWhite.copy(alpha = 0.5f), Offset(cx, cy), Offset(cx, cy + needleLen * 0.6f), strokeWidth = needleWidth)
    // Centre dot
    drawCircle(RoseBg, 8.dp.toPx(), Offset(cx, cy))
    drawCircle(RoseWhite, 8.dp.toPx(), Offset(cx, cy), style = Stroke(2.dp.toPx()))
}

// Fixed triangle pointer at 12 o'clock — shows where YOU are pointing
private fun DrawScope.drawLubberLine(cx: Float, cy: Float) {
    val r = size.minDimension / 2f
    val tipY    = cy - r * 0.96f
    val baseHalf = 8.dp.toPx()
    val baseY   = cy - r * 0.86f

    val path = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx - baseHalf, baseY)
        lineTo(cx + baseHalf, baseY)
        close()
    }
    drawPath(path, Color.White)
    drawPath(path, Color.White.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
}

private fun headingToCardinal(heading: Float): String {
    val h = ((heading % 360f) + 360f) % 360f
    return when {
        h < 11.25f  -> "North"
        h < 33.75f  -> "North-Northeast"
        h < 56.25f  -> "Northeast"
        h < 78.75f  -> "East-Northeast"
        h < 101.25f -> "East"
        h < 123.75f -> "East-Southeast"
        h < 146.25f -> "Southeast"
        h < 168.75f -> "South-Southeast"
        h < 191.25f -> "South"
        h < 213.75f -> "South-Southwest"
        h < 236.25f -> "Southwest"
        h < 258.75f -> "West-Southwest"
        h < 281.25f -> "West"
        h < 303.75f -> "West-Northwest"
        h < 326.25f -> "Northwest"
        h < 348.75f -> "North-Northwest"
        else        -> "North"
    }
}
