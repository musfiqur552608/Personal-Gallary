package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.freedu.personalgallary.R
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.DoubleTapLikeBox
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.components.PopLikeButton
import com.freedu.personalgallary.ui.components.ShimmerFeed
import com.freedu.personalgallary.ui.components.StoryAvatar
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.ui.theme.storyRing
import com.freedu.personalgallary.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    isLoading: Boolean,
    scannedCount: Int,
    posts: List<MediaItem>,
    memories: List<MediaItem>,
    stories: List<MediaItem>,
    totalCount: Int,
    isFavorite: (Long) -> Boolean,
    captionFor: (Long) -> String?,
    onOpen: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onEditCaption: (MediaItem) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onOpenMemories: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSurprise: () -> Unit = {},
    query: String = "",
    onQuery: (String) -> Unit = {},
    onCompress: (MediaItem) -> Unit = {}
) {
    if (isLoading) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Indexing your memories…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$scannedCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            ShimmerFeed()
        }
        return
    }
    if (posts.isEmpty() && totalCount == 0) {
        EmptyState(
            "No posts yet",
            "Grant media access and your photos + long videos will appear here as a private feed."
        )
        return
    }
    val searching = query.isNotBlank()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FeedHeader(totalCount = totalCount, onRefresh = onRefresh, onSurprise = onSurprise)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                placeholder = { Text("Search posts…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Clear, "Clear search")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (!searching && stories.isNotEmpty()) {
            item {
                StoriesBar(stories, onOpen)
            }
        }
        if (!searching && memories.isNotEmpty()) {
            item {
                MemoriesCard(memories, onOpenMemories, onOpen)
            }
        }
        if (searching && posts.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No posts match \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                onAddToAlbum = { onAddToAlbum(item) },
                modifier = Modifier.animateItem(),
                onCompress = { onCompress(item) }
            )
        }
    }
}

@Composable
private fun FeedHeader(totalCount: Int, onRefresh: () -> Unit, onSurprise: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.personal_gallary_logo),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Personal Gallary",
                style = MaterialTheme.typography.titleLarge.copy(brush = brandHorizontal),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "$totalCount memories · offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onSurprise) {
            Icon(Icons.Default.Casino, "Surprise me")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, "Re-scan library")
        }
    }
}

@Composable
private fun StoriesBar(items: List<MediaItem>, onOpen: (MediaItem) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(brandHorizontal)
            )
            Spacer(Modifier.width(6.dp))
            Text("Stories · recent highlights", style = MaterialTheme.typography.labelLarge)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                StoryAvatar(
                    item = item,
                    label = FormatUtils.formatShortDate(item.dateTaken),
                    onClick = { onOpen(item) }
                )
            }
        }
    }
}

@Composable
private fun MemoriesCard(
    memories: List<MediaItem>,
    onOpenMemories: () -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(storyRing)
            .clickable { onOpenMemories() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cake,
                        null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "On this day",
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${memories.size} memories from previous years",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                    )
                }
                TextButton(onClick = onOpenMemories) {
                    Text("View", color = androidx.compose.ui.graphics.Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories.take(10), key = { it.id }) { m ->
                    MediaThumb(
                        m,
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        onClick = { onOpen(m) }
                    )
                }
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
    onAddToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
    onCompress: () -> Unit = {}
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumb(
                item = item,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                showBadge = false
            )
            Spacer(Modifier.width(10.dp))
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
                DropdownMenuItem(
                    text = { Text("Add to album") },
                    onClick = { menu = false; onAddToAlbum() }
                )
                DropdownMenuItem(
                    text = { Text(if (caption.isNullOrBlank()) "Add caption" else "Edit caption") },
                    onClick = { menu = false; onEditCaption() }
                )
                if (!item.isVideo) {
                    DropdownMenuItem(
                        text = { Text("Compress photo") },
                        onClick = { menu = false; onCompress() }
                    )
                }
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
        // media (tap to open, double-tap to like)
        DoubleTapLikeBox(
            onTap = onOpen,
            onDoubleTap = onToggleFavorite,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (item.isVideo) {
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
        // actions
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopLikeButton(liked = liked, onToggle = onToggleFavorite)
            Text(
                caption?.ifBlank { null } ?: item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
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
