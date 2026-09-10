package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.freedu.personalgallary.data.local.CapsuleEntity
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Time capsules: sealed photo sets with a countdown, hidden until the date arrives. */
@Composable
fun CapsuleScreen(
    capsules: List<CapsuleEntity>,
    itemsFor: (Long) -> List<MediaItem>,
    isOpen: (Long) -> Boolean,
    onCreate: (String, Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenItem: (MediaItem, List<MediaItem>) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true; error = null }) {
                Icon(Icons.Default.Add, "New time capsule")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            Text("Time capsules", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Sealed photos stay hidden until the date arrives",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (capsules.isEmpty()) {
                EmptyState(
                    "No capsules yet",
                    "Seal birthday photos, letters, or predictions — future-you will thank you."
                )
                return@Column
            }
            capsules.forEach { cap ->
                val open = isOpen(cap.openAt)
                val items = remember(cap, capsules) { itemsFor(cap.capsuleId) }
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (open) Icons.Default.LockOpen else Icons.Default.LockClock,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.padding(4.dp))
                            Column(Modifier.weight(1f).clickable {
                                expanded = if (expanded == cap.capsuleId) null else cap.capsuleId
                            }) {
                                Text(cap.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (open) "Open — ${items.size} photos revealed"
                                    else "Opens ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(cap.openAt))} · ${countdown(cap.openAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(cap.capsuleId) }) {
                                Icon(Icons.Default.Delete, "Delete capsule", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (expanded == cap.capsuleId && items.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((120 * ((items.size + 2) / 3).coerceAtMost(3)).dp),
                                contentPadding = PaddingValues(2.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                userScrollEnabled = false
                            ) {
                                items(items, key = { it.id }) { item ->
                                    MediaThumb(
                                        item, Modifier.aspectRatio(1f),
                                        onClick = { onOpenItem(item, items) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New time capsule") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text("e.g. 2030 predictions") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateText, onValueChange = { dateText = it },
                        placeholder = { Text("Open date: YYYY-MM-DD") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("+1 mo" to 30, "+6 mo" to 182, "+1 yr" to 365).forEach { (label, days) ->
                            TextButton(onClick = {
                                val cal = java.util.Calendar.getInstance().apply {
                                    add(java.util.Calendar.DAY_OF_YEAR, days)
                                }
                                dateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                            }) { Text(label) }
                        }
                    }
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Add photos later from any post → “Seal in capsule”.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val ts = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateText.trim())?.time
                            if (name.trim().length < 2) {
                                error = "Give it a name."
                            } else if (ts == null || ts <= System.currentTimeMillis()) {
                                error = "Pick a future date (YYYY-MM-DD)."
                            } else {
                                onCreate(name.trim(), ts)
                                name = ""
                                dateText = ""
                                showCreate = false
                            }
                        } catch (_: Exception) {
                            error = "Use format YYYY-MM-DD."
                        }
                    }
                ) { Text("Seal it") }
            },
            dismissButton = { TextButton({ showCreate = false }) { Text("Cancel") } }
        )
    }
}

private fun countdown(openAt: Long): String {
    val days = ((openAt - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
    return when {
        days == 0 -> "opens today"
        days == 1 -> "1 day left"
        days < 30 -> "$days days left"
        days < 365 -> "${days / 30} months left"
        else -> "${days / 365}y ${days % 365 / 30}m left"
    }
}
