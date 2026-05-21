package com.xbjornsen.emftracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xbjornsen.emftracker.data.models.Session
import com.xbjornsen.emftracker.data.models.toAlertLevel
import com.xbjornsen.emftracker.ui.theme.color
import com.xbjornsen.emftracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsScreen(viewModel: MainViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { if (isRecording) viewModel.stopSession() else viewModel.startSession() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRecording) "Stop Recording" else "Start New Session")
        }

        if (isRecording) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Recording…",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions yet. Start recording to save one.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onDelete = { viewModel.deleteSession(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: Session, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val duration = ((session.endTime - session.startTime) / 1000).let { s -> "${s / 60}m ${s % 60}s" }
    val peakLevel = session.peak.toAlertLevel()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(fmt.format(Date(session.startTime)), style = MaterialTheme.typography.titleSmall)
                Text("Duration: $duration · ${session.readingCount} readings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Peak: %.1f µT".format(session.peak), color = peakLevel.color(), style = MaterialTheme.typography.bodySmall)
                    Text("Avg: %.1f µT".format(session.average), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete session", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
