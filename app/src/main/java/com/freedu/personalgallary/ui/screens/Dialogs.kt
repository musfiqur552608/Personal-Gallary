package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem

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
