package com.xbjornsen.emftracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xbjornsen.emftracker.data.models.toAlertLevel
import com.xbjornsen.emftracker.ui.components.PoleDetector
import com.xbjornsen.emftracker.ui.components.RadialGauge
import com.xbjornsen.emftracker.ui.components.StatusChip
import com.xbjornsen.emftracker.ui.sheets.SettingsSheet
import com.xbjornsen.emftracker.ui.theme.NormalColor
import com.xbjornsen.emftracker.ui.theme.VeryHighColor
import com.xbjornsen.emftracker.ui.theme.color
import com.xbjornsen.emftracker.viewmodel.MainViewModel

@Composable
fun LiveScreen(viewModel: MainViewModel) {
    val displayReading by viewModel.displayReading.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val baselineEnabled by viewModel.baselineEnabled.collectAsState()
    val motionEnabled by viewModel.motionEnabled.collectAsState()
    val motionState by viewModel.motionState.collectAsState()
    val alertLevel = displayReading.magnitude.toAlertLevel()
    val haptic = LocalHapticFeedback.current
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(alertLevel) {
        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gaugeSize = (maxHeight * 0.42f).coerceIn(200.dp, 340.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Magnetometer",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Row {
                    IconButton(onClick = { viewModel.toggleBaseline() }) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Magnet mode",
                            tint = if (baselineEnabled) NormalColor
                                   else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Analysis settings")
                    }
                }
            }

            // Centre section expands to fill remaining height
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Gauge
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(gaugeSize)) {
                        RadialGauge(
                            magnitude = displayReading.magnitude,
                            alertLevel = alertLevel,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.1f".format(displayReading.magnitude),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = alertLevel.color()
                            )
                            Text(
                                text = if (baselineEnabled) "µT (delta)" else "µT",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = alertLevel.label,
                                color = alertLevel.color(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Axis values
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AxisValue("X", displayReading.x)
                        AxisValue("Y", displayReading.y)
                        AxisValue("Z", displayReading.z)
                    }

                    // Pole detector — visible when baseline is on and a significant delta exists
                    if (baselineEnabled) {
                        PoleDetector(
                            delta = displayReading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Status badges — row always present when any feature is on (avoids layout jump)
                    val showBadgeRow = baselineEnabled || motionEnabled
                    if (showBadgeRow) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (baselineEnabled) StatusChip("Calibrated", NormalColor)
                            // Motion: always show when enabled so the row never pops in/out
                            if (motionEnabled) {
                                if (motionState.isUnstable)
                                    StatusChip("Unstable", VeryHighColor)
                                else
                                    StatusChip("Stable", NormalColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Haptic toggle pinned at bottom
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                Text("Haptic feedback", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = hapticEnabled, onCheckedChange = { viewModel.toggleHaptic() })
            }
        }

        if (showSettings) {
            SettingsSheet(viewModel = viewModel, onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun AxisValue(axis: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(axis, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text("%.1f".format(value), style = MaterialTheme.typography.bodyLarge)
        Text("µT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
