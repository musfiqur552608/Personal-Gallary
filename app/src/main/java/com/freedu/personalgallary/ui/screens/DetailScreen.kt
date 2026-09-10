package com.freedu.personalgallary.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.DoubleTapLikeBox
import com.freedu.personalgallary.ui.components.PopLikeButton
import com.freedu.personalgallary.util.FormatUtils
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Full post view: swipe-through gallery, pinch-to-zoom images, inline video playback.
 */
@OptIn(UnstableApi::class)
@Composable
fun DetailScreen(
    items: List<MediaItem>,
    startIndex: Int,
    isFavorite: (Long) -> Boolean,
    captionFor: (Long) -> String?,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onSaveCaption: (MediaItem, String) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onEditPhoto: (MediaItem) -> Unit = {},
    onMarkup: (MediaItem) -> Unit = {},
    onTrimVideo: (MediaItem) -> Unit = {},
    onLockItem: (MediaItem) -> Unit = {},
    onFixDate: (MediaItem) -> Unit = {},
    onVoiceNote: (MediaItem) -> Unit = {},
    onSealCapsule: (MediaItem) -> Unit = {},
    compareTarget: Long? = null,
    onCompare: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    if (items.isEmpty()) {
        Scaffold(topBar = {
            DetailTopBar(
                title = "Post",
                onBack = onBack,
                actions = {}
            )
        }) { Text("Nothing to show", Modifier.padding(it).padding(24.dp)) }
        return
    }
    val safeStart = startIndex.coerceIn(0, items.lastIndex)
    val pager = rememberPagerState(initialPage = safeStart, pageCount = { items.size })
    var showCaptionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    val current = items[pager.currentPage]

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "${pager.currentPage + 1} / ${items.size}",
                onBack = onBack,
                actions = {
                    PopLikeButton(
                        liked = isFavorite(current.id),
                        onToggle = { onToggleFavorite(current) }
                    )
                    IconButton({ onShare(current) }) { Icon(Icons.Default.Share, "Share") }
                    IconButton({ showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete") }
                    IconButton({ overflow = true }) { Icon(Icons.Default.MoreVert, "More actions") }
                    DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                        if (!current.isVideo) {
                            DropdownMenuItem(
                                text = { Text("Edit photo") },
                                onClick = { overflow = false; onEditPhoto(current) }
                            )
                            DropdownMenuItem(
                                text = { Text("Markup & annotate") },
                                onClick = { overflow = false; onMarkup(current) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Trim & mute") },
                                onClick = { overflow = false; onTrimVideo(current) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Move to vault") },
                            onClick = { overflow = false; onLockItem(current) }
                        )
                        DropdownMenuItem(
                            text = { Text("Fix date") },
                            onClick = { overflow = false; onFixDate(current) }
                        )
                        DropdownMenuItem(
                            text = { Text("Voice note") },
                            onClick = { overflow = false; onVoiceNote(current) }
                        )
                        DropdownMenuItem(
                            text = { Text("Seal in time capsule") },
                            onClick = { overflow = false; onSealCapsule(current) }
                        )
                        if (compareTarget != null) {
                            DropdownMenuItem(
                                text = { Text("Compare before / after") },
                                onClick = { overflow = false; onCompare(compareTarget) }
                            )
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(420.dp).background(Color.Black)) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    val item = items[page]
                    if (item.isVideo) {
                        VideoPlayerView(item, active = page == pager.currentPage)
                    } else {
                        val zoom = rememberZoomState()
                        DoubleTapLikeBox(
                            onTap = {},
                            onDoubleTap = { onToggleFavorite(items[pager.currentPage]) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().zoomable(zoom)
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current.albumName.ifBlank { "Gallery" }, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        FormatUtils.formatDate(current.dateTaken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                val caption = captionFor(current.id)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        caption?.ifBlank { null } ?: current.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton({ showCaptionDialog = true }) { Icon(Icons.Default.Edit, "Edit caption") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(if (current.isVideo) "Video" else "Photo")
                        append(" · ${FormatUtils.formatBytes(current.sizeBytes)}")
                        if (current.isVideo) append(" · ${FormatUtils.formatDuration(current.durationMs)}")
                        if (current.width > 0) append(" · ${current.width}×${current.height}")
                        append(" · ${current.mimeType}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedButton({ onAddToAlbum(current) }) { Text("Add to album") }
                    androidx.compose.material3.OutlinedButton({ onDetails(current) }) { Text("Details") }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showCaptionDialog) {
        var text by remember(current.id) { mutableStateOf(captionFor(current.id).orEmpty()) }
        AlertDialog(
            onDismissRequest = { showCaptionDialog = false },
            title = { Text("Caption (stored on-device)") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("Add a memory note…") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
            },
            confirmButton = {
                TextButton({
                    onSaveCaption(current, text.trim())
                    showCaptionDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ showCaptionDialog = false }) { Text("Cancel") } }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to trash?") },
            text = { Text("You can restore it within 30 days.") },
            confirmButton = {
                TextButton({
                    showDeleteDialog = false
                    onDelete(current)
                }) { Text("Move to trash", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ showDeleteDialog = false }) { Text("Keep") } }
        )
    }
}

/** Plain top bar (avoids experimental TopAppBar API). */
@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        actions()
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerView(item: MediaItem, active: Boolean) {
    val context = LocalContext.current
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            if (active) play()
        }
    }
    DisposableEffect(item.uri) { onDispose { player.release() } }
    androidx.compose.runtime.LaunchedEffect(active) {
        if (active) player.play() else player.pause()
    }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}
