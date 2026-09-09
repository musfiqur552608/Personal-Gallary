package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.StatsUtils

/** Local stats dashboard: streaks, heatmap, totals, top albums. */
@Composable
fun StatsScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    favCount: Int,
    onShare: (String) -> Unit
) {
    val heat = remember(items) { StatsUtils.heatmap(items) }
    val streak = remember(items) { StatsUtils.currentStreak(items) }
    val longest = remember(items) { StatsUtils.longestStreak(items) }
    val photos = remember(items) { items.count { !it.isVideo } }
    val videos = remember(items) { items.count { it.isVideo } }
    val weeks = remember(heat) { heat.chunked(7) }
    val maxDay = remember(heat) { heat.maxOfOrNull { it.count } ?: 0 }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "All computed on-device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                onShare(StatsUtils.shareText(items.size, photos, videos, streak, longest, favCount))
            }) { Icon(Icons.Default.Share, "Share stats") }
        }
        Spacer(Modifier.height(12.dp))
        // streak hero
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(brandHorizontal)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "$streak-day streak",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Best ever: $longest days · ${FormatUtils.formatBytes(items.sumOf { it.sizeBytes })} of memories",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${items.size}", "Memories", Modifier.weight(1f))
            StatCard("$photos", "Photos", Modifier.weight(1f))
            StatCard("$videos", "Videos", Modifier.weight(1f))
            StatCard("$favCount", "Liked", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text("Shooting days · last 20 weeks", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    week.forEach { day ->
                        val frac = if (maxDay == 0) 0f else day.count.toFloat() / maxDay
                        val color = when {
                            day.count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                            frac < 0.34f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            frac < 0.67f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Top albums", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val total = (items.sumOf { it.sizeBytes }).coerceAtLeast(1L)
        albums.take(8).forEach { album ->
            Column(Modifier.padding(vertical = 4.dp)) {
                Row {
                    Text(album.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${album.count} · ${FormatUtils.formatBytes(album.totalBytes)}",
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
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (label == "Liked") {
                Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
