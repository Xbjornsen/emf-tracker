package com.xbjornsen.emftracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xbjornsen.emftracker.data.models.AlertLevel
import com.xbjornsen.emftracker.ui.theme.color

@Composable
fun RadialGauge(magnitude: Float, alertLevel: AlertLevel, modifier: Modifier = Modifier) {
    val arcColor = alertLevel.color()
    val animated by animateFloatAsState(
        targetValue = magnitude.coerceIn(0f, 100f),
        animationSpec = tween(100),
        label = "gauge"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(size.minDimension - strokeWidth, size.minDimension - strokeWidth)
        val topLeft = Offset(
            (size.width - arcSize.width) / 2,
            (size.height - arcSize.height) / 2
        )

        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )

        val sweep = (animated / 100f) * 270f
        if (sweep > 0f) {
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        }
    }
}
