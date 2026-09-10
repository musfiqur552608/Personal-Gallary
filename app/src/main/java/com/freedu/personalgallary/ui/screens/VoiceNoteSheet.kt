package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.local.VoiceNoteEntity
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils

/** Voice-note recorder/player sheet for a photo. */
@Composable
fun VoiceNoteSheet(
    item: MediaItem,
    existing: VoiceNoteEntity?,
    recording: Boolean,
    playing: Boolean,
    busy: Boolean,
    onRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!recording) onDismiss() },
        title = { Text("Voice note", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existing == null && !recording) {
                        IconButton(onClick = onRecord, modifier = Modifier.size(56.dp)) {
                            Icon(
                                Icons.Default.Mic, "Record",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text("Tap to record a memory", style = MaterialTheme.typography.bodyMedium)
                    } else if (recording) {
                        if (busy) CircularProgressIndicator(Modifier.size(36.dp))
                        else IconButton(onClick = onStopRecord, modifier = Modifier.size(56.dp)) {
                            Icon(
                                Icons.Default.Stop, "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            "Recording… tap stop to save",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (existing != null) {
                        IconButton(onClick = onPlay, modifier = Modifier.size(56.dp)) {
                            Icon(
                                if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                if (playing) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (playing) "Playing…" else "Saved note",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                FormatUtils.formatDuration(existing.durationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete note", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stored privately on this device, beside the photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (!recording) TextButton(onDismiss) { Text("Done") }
        }
    )
}
