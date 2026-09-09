package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    isLoading: Boolean,
    scannedCount: Int,
    posts: List<MediaItem>,
    memories: List<MediaItem>,
    stories: List<MediaItem>,
    isFavorite: (Long) -> Boolean,
    captionFor: (Long) -> String?,
    onOpen: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onEditCaption: (MediaItem) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onOpenMemories: () -> Unit = {}
) {
    if (isLoading) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Indexing your memories… $scannedCount items", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
        }
        return
    }
    if (posts.isEmpty()) {
        EmptyState("No posts yet", "Grant media access and your photos + long videos will appear here as a private feed.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (stories.isNotEmpty()) {
            item {
                StoriesBar(stories, onOpen)
            }
        }
        if (memories.isNotEmpty()) {
            item {
                MemoriesCard(memories, onOpenMemories, onOpen)
            }
        }
        items(posts, key = { it.id }) { item ->
            PostCard(
                item = item,
                liked = isFavorite(item.id),
                caption = captionFor(item.id),
                onOpen = { onOpen(item) },
                onToggleFavorite = { onToggleFavorite(item) },
                onShare = { onShare(item) },
                onDelete = { onDelete(item) },
                onEditCaption = { onEditCaption(item) },
                onAddToAlbum = { onAddToAlbum(item) }
            )
        }
    }
}

@Composable
private fun StoriesBar(items: List<MediaItem>, onOpen: (MediaItem) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            "Stories · recent highlights",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                    MediaThumb(
                        item = item,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        onClick = { onOpen(item) }
                    )
                    Text(
                        FormatUtils.formatShortDate(item.dateTaken),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoriesCard(memories: List<MediaItem>, onOpenMemories: () -> Unit, onOpen: (MediaItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cake, null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("On this day", fontWeight = FontWeight.Bold)
                Text("${memories.size} memories from previous years", style = MaterialTheme.typography.bodySmall)
            }
            androidx.compose.material3.TextButton(onClick = onOpenMemories) { Text("View") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memories.take(10), key = { it.id }) { m ->
                MediaThumb(m, Modifier.size(72.dp), onClick = { onOpen(m) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostCard(
    item: MediaItem,
    liked: Boolean,
    caption: String?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEditCaption: () -> Unit,
    onAddToAlbum: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.albumName.ifBlank { "Gallery" }, fontWeight = FontWeight.SemiBold)
                Text(
                    FormatUtils.formatDate(item.dateTaken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() })
                DropdownMenuItem(text = { Text("Add to album") }, onClick = { menu = false; onAddToAlbum() })
                DropdownMenuItem(text = { Text(if (caption.isNullOrBlank()) "Add caption" else "Edit caption") }, onClick = { menu = false; onEditCaption() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
        // media
        if (item.isVideo) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).combinedClickable(onClick = onOpen, onLongClick = {})
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    Icons.Default.PlayCircle, null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.Center).size(56.dp)
                )
            }
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).combinedClickable(onClick = onOpen, onLongClick = {})
            )
        }
        // actions
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite",
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                caption?.ifBlank { null } ?: item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            if (item.isVideo) {
                Text(
                    FormatUtils.formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}
