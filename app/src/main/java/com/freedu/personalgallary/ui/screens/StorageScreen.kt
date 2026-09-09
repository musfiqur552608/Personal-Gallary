package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.FormatUtils

/** Storage analyzer: totals, per-album bars, biggest videos, screenshot clutter. */
@Composable
fun StorageScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    onOpen: (MediaItem) -> Unit,
    onReviewScreenshots: () -> Unit
) {
    val total = remember(items) { items.sumOf { it.sizeBytes }.coerceAtLeast(1L) }
    val biggest = remember(items) {
        items.filter { it.isVideo }.sortedByDescending { it.sizeBytes }.take(15)
    }
    val shots = remember(items) {
        items.filter {
            it.albumName.lowercase().contains("screenshot") ||
                it.name.lowercase().contains("screenshot")
        }
    }
    val shotsBytes = shots.sumOf { it.sizeBytes }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Storage", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${FormatUtils.formatBytes(total)} across ${items.size} items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text("By album", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        albums.take(10).forEach { album ->
            Column(Modifier.padding(vertical = 4.dp)) {
                Row {
                    Text(album.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        FormatUtils.formatBytes(album.totalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((album.totalBytes.toFloat() / total).coerceIn(0.02f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brandHorizontal)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Screenshots", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${shots.size} files · ${FormatUtils.formatBytes(shotsBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onReviewScreenshots, enabled = shots.isNotEmpty()) {
                    Text("Review")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Biggest videos", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        biggest.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item) }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumb(item, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${FormatUtils.formatBytes(item.sizeBytes)} · ${FormatUtils.formatDuration(item.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
