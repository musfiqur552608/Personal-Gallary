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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.freedu.personalgallary.util.PlaceCluster

/** Offline places: photo clusters by EXIF GPS grid cells. No map, no network. */
@Composable
fun PlacesScreen(
    load: suspend ((done: Int, total: Int) -> Unit) -> List<PlaceCluster>,
    itemById: (Long) -> MediaItem?,
    onOpen: (MediaItem) -> Unit
) {
    var clusters by remember { mutableStateOf<List<PlaceCluster>?>(null) }
    var progress by remember { mutableStateOf(0 to 1) }
    var selected by remember { mutableStateOf<PlaceCluster?>(null) }

    LaunchedEffect(Unit) {
        clusters = load { done, total -> progress = done to total }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Places",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        val loaded = clusters
        if (loaded == null) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Reading photo locations… ${progress.first}/${progress.second}")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )
            }
            return
        }
        if (loaded.isEmpty()) {
            EmptyState(
                "No locations found",
                "Photos with GPS tags will be grouped here. Enable location tagging in your camera app."
            )
            return
        }
        if (selected == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                Text(
                    "${loaded.sumOf { it.count }} geotagged photos in ${loaded.size} places",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                loaded.forEach { cluster ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { selected = cluster }
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            cluster.cover?.let {
                                MediaThumb(it, Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)), showBadge = false)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cluster.label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${cluster.count} photos · newest ${FormatUtils.formatShortDate(cluster.newestTs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val sel = selected!!
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.TextButton(onClick = { selected = null }) {
                    Text("‹ All places")
                }
                Text(
                    "${sel.label} · ${sel.count}",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            val gridItems = remember(sel) { sel.itemIds.mapNotNull(itemById) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(gridItems, key = { it.id }) { item ->
                    MediaThumb(item, Modifier.aspectRatio(1f), onClick = { onOpen(item) })
                }
            }
        }
    }
}
