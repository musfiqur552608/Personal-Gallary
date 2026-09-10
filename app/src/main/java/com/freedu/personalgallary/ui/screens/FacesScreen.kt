package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.viewmodel.FaceStat
import com.freedu.personalgallary.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

/** Faces wall: on-device face detection index (photos with people, smiles, groups). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacesScreen(
    items: List<MediaItem>,
    stats: Map<Long, FaceStat>,
    scan: suspend ((done: Int, total: Int) -> Unit) -> GalleryViewModel.AiScanResult,
    onOpen: (MediaItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 1) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 all faces, 1 smiles, 2 groups

    val wall = remember(items, stats, mode) {
        items.filter {
            val s = stats[it.id] ?: return@filter false
            when (mode) {
                1 -> s.smiling
                2 -> s.count >= 3
                else -> s.count >= 1
            }
        }
    }
    val indexed = stats.size
    val withFaces = remember(stats) { stats.count { it.value.count > 0 } }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Faces",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            "$indexed scanned · $withFaces with people · 100% on-device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (error != null) {
            Text(
                error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        when (val r = scan { d, t -> progress = d to t }) {
                            is GalleryViewModel.AiScanResult.Done ->
                                if (r.count == 0) error = "No new faces found."
                            is GalleryViewModel.AiScanResult.Unavailable ->
                                error = "Face model unavailable — needs Google Play services (one-time download)."
                        }
                        busy = false
                    }
                },
                enabled = !busy
            ) { Text(if (indexed == 0) "Scan library" else "Scan new") }
            listOf("Everyone" to 0, "Smiles" to 1, "Groups" to 2).forEach { (label, m) ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        if (busy) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Detecting faces… ${progress.first}/${progress.second}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
            }
        }
        if (wall.isEmpty() && !busy) {
            EmptyState(
                "No faces here yet",
                "Run a scan — group shots, smiles and portraits will gather on this wall."
            )
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(wall, key = { it.id }) { item ->
                val n = stats[item.id]?.count ?: 0
                androidx.compose.foundation.layout.Box(Modifier.aspectRatio(1f)) {
                    MediaThumb(item, Modifier.fillMaxSize(), showBadge = false, onClick = { onOpen(item) })
                    if (n > 1) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .align(androidx.compose.ui.Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$n", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
