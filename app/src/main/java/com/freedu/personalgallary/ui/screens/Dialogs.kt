package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.launch

@Composable
fun AddToAlbumSheet(
    customAlbums: List<CustomAlbumEntity>,
    onCreate: (String) -> Unit,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to album") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text("New album name…") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    TextButton(
                        onClick = { onCreate(name.trim()); name = "" },
                        enabled = name.trim().length >= 2
                    ) { Text("Create") }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(customAlbums, key = { it.albumId }) { album ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .clickable { onAdd(album.albumId) }
                        ) {
                            Text(album.name, Modifier.padding(12.dp))
                        }
                    }
                }
                if (customAlbums.isEmpty()) {
                    Text(
                        "No collections yet — create one above. References only, files never move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Done") } }
    )
}

@Composable
fun FolderFilterDialog(
    albums: List<Album>,
    excluded: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Include / exclude folders") },
        text = {
            LazyColumn {
                items(albums, key = { it.name }) { album ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(album.name) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(album.name !in excluded, onCheckedChange = { onToggle(album.name) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(album.name)
                            Text(
                                "${album.count} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Done") } }
    )
}

@Composable
fun MediaDetailsDialog(item: MediaItem, caption: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Details") },
        text = {
            Column {
                DetailRow("Name", item.name)
                DetailRow("Type", "${if (item.isVideo) "Video" else "Photo"} · ${item.mimeType}")
                DetailRow("Date", com.freedu.personalgallary.util.FormatUtils.formatDate(item.dateTaken))
                DetailRow("Album", item.albumName.ifBlank { "—" })
                DetailRow("Size", com.freedu.personalgallary.util.FormatUtils.formatBytes(item.sizeBytes))
                if (item.isVideo) DetailRow("Duration", com.freedu.personalgallary.util.FormatUtils.formatDuration(item.durationMs))
                if (item.width > 0) DetailRow("Resolution", "${item.width} × ${item.height}")
                if (!caption.isNullOrBlank()) DetailRow("Caption", caption)
                DetailRow("Path", item.relativePath.ifBlank { "—" })
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private data class CompressPreset(val label: String, val desc: String, val maxDim: Int, val quality: Int)

/** Compress photo: live size estimate, saves a smaller copy (original untouched). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressDialog(
    item: MediaItem,
    onDone: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val presets = remember {
        listOf(
            CompressPreset("High", "2560px · q88", 2560, 88),
            CompressPreset("Medium", "1920px · q80", 1920, 80),
            CompressPreset("Small", "1280px · q70", 1280, 70)
        )
    }
    var selected by remember { mutableStateOf(1) }
    var estimate by remember { mutableStateOf<Long?>(null) }
    var computing by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(item.id, selected) {
        computing = true
        estimate = null
        val p = presets[selected]
        estimate = ImageEditUtils.compressToBytes(context, item.uri, p.maxDim, p.quality)?.size?.toLong()
        computing = false
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Compress photo", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEachIndexed { i, p ->
                        FilterChip(
                            selected = selected == i,
                            onClick = { selected = i },
                            label = { Text(p.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Text(
                    presets[selected].desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                if (computing) {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Estimating…", style = MaterialTheme.typography.bodySmall)
                } else {
                    val est = estimate
                    if (est == null) {
                        Text(
                            "Couldn't read this photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        val saved = (1 - est.toDouble() / item.sizeBytes.coerceAtLeast(1))
                            .coerceIn(0.0, 0.99)
                        Text(
                            "${FormatUtils.formatBytes(item.sizeBytes)} → ${FormatUtils.formatBytes(est)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${(saved * 100).toInt()}% smaller · saved as a new copy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val p = presets[selected]
                        val data = ImageEditUtils.compressToBytes(context, item.uri, p.maxDim, p.quality)
                        val ok = if (data != null) {
                            ImageEditUtils.saveJpegBytes(
                                context, data,
                                "PG_COMP_${System.currentTimeMillis()}.jpg"
                            ) != null
                        } else false
                        saving = false
                        onDone(ok)
                    }
                },
                enabled = !saving && !computing && estimate != null
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save copy")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}
