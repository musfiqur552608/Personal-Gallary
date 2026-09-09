package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb

/** Secure recycle bin: 30-day safety net, restore or delete forever. */
@Composable
fun TrashScreen(
    items: List<MediaItem>,
    trashedAt: Map<Long, Long>,
    onRestore: (Long) -> Unit,
    onDeleteForever: (MediaItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    var confirm by remember { mutableStateOf<MediaItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Trash", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Kept 30 days, then deleted forever",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) { Text("Empty") }
            }
        }
        if (items.isEmpty()) {
            EmptyState("Trash is empty", "Deleted photos rest here for 30 days before disappearing forever.")
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val daysLeft = 30 - ((System.currentTimeMillis() - (trashedAt[item.id] ?: 0L)) / 86_400_000L)
                    .toInt().coerceIn(0, 30)
                Box(Modifier.aspectRatio(1f)) {
                    MediaThumb(item, Modifier.fillMaxSize(), onClick = { onOpen(item) })
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${daysLeft}d left", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Row(Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { onRestore(item.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, "Restore", tint = Color.White)
                        }
                        IconButton(
                            onClick = { confirm = item },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, "Delete forever", tint = Color.White)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    confirm?.let { target ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Delete forever?") },
            text = { Text("${target.name}\nThis cannot be undone.") },
            confirmButton = {
                TextButton({
                    onDeleteForever(target)
                    confirm = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("Keep") } }
        )
    }
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Empty trash?") },
            text = { Text("All ${items.size} items will be permanently deleted.") },
            confirmButton = {
                TextButton({
                    onEmptyTrash()
                    confirmEmpty = false
                }) { Text("Empty", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmEmpty = false }) { Text("Cancel") } }
        )
    }
}
