package com.xbjornsen.emftracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xbjornsen.emftracker.data.models.toAlertLevel
import com.xbjornsen.emftracker.ui.components.FftBarChart
import com.xbjornsen.emftracker.ui.components.LineChart
import com.xbjornsen.emftracker.ui.components.StatusChip
import com.xbjornsen.emftracker.ui.components.ThresholdBarChart
import com.xbjornsen.emftracker.ui.components.WaveformChart
import com.xbjornsen.emftracker.ui.theme.VeryHighColor
import com.xbjornsen.emftracker.ui.theme.color
import com.xbjornsen.emftracker.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun ChartScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsState()
    val displayReading by viewModel.displayReading.collectAsState()
    val fftEnabled by viewModel.fftEnabled.collectAsState()
    val fftResult by viewModel.fftResult.collectAsState()
    val waveformSamples by viewModel.waveformSamples.collectAsState()
    val alertLevel = displayReading.magnitude.toAlertLevel()
    var showBars by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // When FFT panel is hidden, give the history chart most of the screen.
        // When FFT is on, shrink it so the extra cards are reachable by scrolling.
        val chartHeight = if (fftEnabled) 180.dp else (maxHeight * 0.48f).coerceIn(180.dp, 400.dp)
        val statsHeight = if (fftEnabled) null else (maxHeight * 0.35f).coerceIn(160.dp, 280.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("60-Second History", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(selected = !showBars, onClick = { showBars = false }, label = { Text("Line") })
                FilterChip(selected = showBars,  onClick = { showBars = true  }, label = { Text("Bars") })
                IconButton(onClick = { viewModel.refreshReadings() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear and refresh readings")
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(12.dp)) {
                if (history.size >= 2) {
                    if (showBars)
                        ThresholdBarChart(data = history, modifier = Modifier.fillMaxWidth().height(chartHeight))
                    else
                        LineChart(data = history, modifier = Modifier.fillMaxWidth().height(chartHeight))
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.Center) {
                        Text("Collecting data…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Statistics", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Card(modifier = if (statsHeight != null) Modifier.fillMaxWidth().height(statsHeight) else Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).then(if (statsHeight != null) Modifier.fillMaxSize() else Modifier),
                verticalArrangement = if (statsHeight != null) Arrangement.SpaceEvenly else Arrangement.spacedBy(8.dp)
            ) {
                StatRow("Current", "%.1f µT".format(displayReading.magnitude), alertLevel.color())
                StatRow("Peak", "%.1f µT".format(history.maxOrNull() ?: 0f))
                StatRow("Average", "%.1f µT".format(if (history.isEmpty()) 0f else history.average().toFloat()))
                StatRow("Readings", "${history.size}")
            }
        }

        if (fftEnabled) {
            Spacer(Modifier.height(16.dp))
            Text("AC Waveform (time domain)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Raw magnetic field over the last 128 samples (~640 ms). A 50/60 Hz AC source produces a visible oscillation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(12.dp)) {
                    val wave = waveformSamples
                    val result = fftResult
                    if (wave != null) {
                        WaveformChart(
                            samples = wave,
                            sampleRateHz = result?.sampleRateHz ?: 150f,
                            has50Hz = result?.has50Hz ?: false,
                            has60Hz = result?.has60Hz ?: false,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Frequency Analysis (FFT)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "50/60 Hz peaks = AC mains radiation (the health-relevant signal). " +
                "Low-frequency peaks (1–5 Hz) = hand movement near a static magnet, not radiation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val result = fftResult
                    if (result != null) {
                        // Sample rate warning — Nyquist means 50/60 Hz is invisible below ~130 Hz
                        val nyquist = result.sampleRateHz / 2f
                        if (result.sampleRateHz < 130f) {
                            Text(
                                "⚠ Sample rate ${result.sampleRateHz.roundToInt()} Hz — " +
                                "Nyquist limit is ${nyquist.roundToInt()} Hz. " +
                                "50/60 Hz mains not detectable on this device at this rate.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Only show chips for confirmed 40–70 Hz signals
                        if (result.has50Hz || result.has60Hz) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                if (result.has50Hz) StatusChip("50 Hz ⚡", VeryHighColor)
                                if (result.has60Hz) StatusChip("60 Hz ⚡", VeryHighColor)
                            }
                        }
                        FftBarChart(
                            bins = result.bins,
                            dominantHz = result.dominantFrequencyHz,
                            has50Hz = result.has50Hz,
                            has60Hz = result.has60Hz,
                            sampleRateHz = result.sampleRateHz,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
    } // BoxWithConstraints
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyLarge)
    }
}
