package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Share choice: original files or auto-blurred faces (videos always original). */
@Composable
fun ShareChoiceDialog(
    photoCount: Int,
    videoCount: Int,
    busy: Boolean,
    busyText: String?,
    onShareOriginal: () -> Unit,
    onShareBlurred: () -> Unit,
    onDismiss: () -> Unit
) {
    var blur by remember { mutableStateOf(photoCount > 0) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                if (photoCount + videoCount == 1) "Share" else "Share ${photoCount + videoCount} items",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (photoCount > 0) {
                    BlurOption(
                        selected = !blur,
                        title = "Original photos",
                        desc = "Exactly as stored on this device",
                        enabled = !busy,
                        onSelect = { blur = false }
                    )
                    Spacer(Modifier.height(4.dp))
                    BlurOption(
                        selected = blur,
                        title = "Blur faces first",
                        desc = "Pixelates detected faces; originals untouched",
                        enabled = !busy,
                        onSelect = { blur = true }
                    )
                    if (videoCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$videoCount video${if (videoCount > 1) "s" else ""} always share${if (videoCount > 1) "" else "s"} original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "Videos are shared exactly as stored.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (busy) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        busyText ?: "Working…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (blur && photoCount > 0) onShareBlurred() else onShareOriginal() },
                enabled = !busy
            ) { Text("Share") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}

@Composable
private fun BlurOption(
    selected: Boolean,
    title: String,
    desc: String,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
