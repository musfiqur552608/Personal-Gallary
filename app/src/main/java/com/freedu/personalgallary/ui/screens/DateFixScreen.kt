package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

/** Fix a wrong camera date: stored as a local override (file EXIF untouched). */
@Composable
fun DateFixScreen(
    item: MediaItem,
    currentDate: Long,
    onSave: (Long?) -> Unit,
    onBack: () -> Unit
) {
    var custom by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun shift(deltaMs: Long, label: String): Pair<String, Long> = label to (currentDate + deltaMs)

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Fix date", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            item.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Currently shown as", style = MaterialTheme.typography.labelLarge)
        Text(
            FormatUtils.formatDate(currentDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        Text("Quick shift", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        val shifts = listOf(
            shift(-3_600_000L, "-1 hour"),
            shift(3_600_000L, "+1 hour"),
            shift(-86_400_000L, "-1 day"),
            shift(86_400_000L, "+1 day"),
            shift(-365L * 86_400_000L, "-1 year"),
            shift(365L * 86_400_000L, "+1 year")
        )
        shifts.chunked(3).forEach { row ->
            Row {
                row.forEach { (label, ts) ->
                    TextButton(onClick = { onSave(ts) }) { Text(label) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Exact date & time", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it; error = null },
            placeholder = { Text("YYYY-MM-DD HH:MM") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                try {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        .parse(custom.trim())?.time
                    if (ts == null || ts <= 0) error = "Use format YYYY-MM-DD HH:MM."
                    else onSave(ts)
                } catch (_: Exception) {
                    error = "Use format YYYY-MM-DD HH:MM."
                }
            }) { Text("Save exact date") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onSave(null) }) { Text("Use original") }
            TextButton(onClick = onBack) { Text("Cancel") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Overrides apply to sorting, Memories and search on this device. The file itself is never modified.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
