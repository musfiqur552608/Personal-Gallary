package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.launch

/** Duplicate & burst cleaner: perceptual-hash groups, keep-best flow. */
@Composable
fun DuplicatesScreen(
    scan: suspend ((done: Int, total: Int) -> Unit) -> List<List<MediaItem>>,
    onTrashItems: (List<MediaItem>) -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 1) }
    var groups by remember { mutableStateOf<List<List<MediaItem>>>(emptyList()) }
    var scanned by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Duplicates",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (!scanned) "Find identical & burst shots"
                    else "${groups.size} groups · ${groups.sumOf { it.size - 1 }} reclaimable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        groups = scan { d, t -> progress = d to t }
                        scanned = true
                        busy = false
                    }
                },
                enabled = !busy
            ) { Text(if (scanned) "Re-scan" else "Scan") }
        }
        if (busy) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Hashing photos… ${progress.first}/${progress.second}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
            }
        }
        if (scanned && groups.isEmpty() && !busy) {
            EmptyState("Squeaky clean", "No duplicates or burst groups found in your library.")
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            items(groups, key = { it.first().id }) { group ->
                var dismissed by remember(group) { mutableStateOf(false) }
                if (dismissed) return@items
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "${group.size} similar · ${FormatUtils.formatBytes(group.sumOf { it.sizeBytes })}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(group, key = { it.id }) { item ->
                                MediaThumb(
                                    item, Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)),
                                    onClick = { onOpen(item) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = {
                                onTrashItems(group.drop(1))
                                dismissed = true
                            }) { Text("Keep first, trash rest") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { dismissed = true }) { Text("Keep all") }
                        }
                    }
                }
            }
        }
    }
}
