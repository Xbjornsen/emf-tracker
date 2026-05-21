package com.xbjornsen.emftracker.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xbjornsen.emftracker.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val baselineEnabled by viewModel.baselineEnabled.collectAsState()
    val fftEnabled by viewModel.fftEnabled.collectAsState()
    val motionEnabled by viewModel.motionEnabled.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Analysis Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            SettingRow(
                title = "Baseline Subtraction",
                description = "Calibrate to current field and show delta readings",
                checked = baselineEnabled,
                onToggle = { viewModel.toggleBaseline() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingRow(
                title = "High Rate + FFT",
                description = "~200 Hz sampling to detect 50/60 Hz mains sources",
                checked = fftEnabled,
                onToggle = { viewModel.toggleFft() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingRow(
                title = "Motion Detection",
                description = "Flag readings as unstable when device is moving",
                checked = motionEnabled,
                onToggle = { viewModel.toggleMotion() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
