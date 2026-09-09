package com.freedu.personalgallary.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.GradientScrim
import com.freedu.personalgallary.ui.components.PopLikeButton
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.delay

/**
 * Full-screen vertical swipe feed for short videos (<= 60s). Auto-play, loop, preload-next.
 */
@OptIn(UnstableApi::class)
@Composable
fun ReelsScreen(
    reels: List<MediaItem>,
    isFavorite: (Long) -> Boolean,
    showInfo: Boolean,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onWallpaper: ((MediaItem) -> Unit)? = null
) {
    if (reels.isEmpty()) {
        EmptyState("No reels yet", "Videos of 60 seconds or less will appear here as a full-screen swipeable feed.")
        return
    }
    val pagerState = rememberPagerState(pageCount = { reels.size })
    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val item = reels[page]
        ReelPage(
            item = item,
            active = pagerState.currentPage == page,
            liked = isFavorite(item.id),
            showInfo = showInfo,
            onToggleFavorite = { onToggleFavorite(item) },
            onShare = { onShare(item) },
            onDelete = { onDelete(item) },
            onAddToAlbum = { onAddToAlbum(item) },
            onDetails = { onDetails(item) },
            onWallpaper = onWallpaper?.let { { it(item) } }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelPage(
    item: MediaItem,
    active: Boolean,
    liked: Boolean,
    showInfo: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onAddToAlbum: () -> Unit,
    onDetails: () -> Unit,
    onWallpaper: (() -> Unit)?
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var muted by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val heartScale = remember { Animatable(0f) }

    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 1f
            prepare()
        }
    }
    DisposableEffect(item.uri) {
        onDispose { player.release() }
    }
    LaunchedEffect(active, paused) {
        if (active && !paused) player.play() else player.pause()
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    // live playback progress for the progress bar
    LaunchedEffect(active) {
        while (active) {
            val d = player.duration
            progress = if (d > 0) (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(150)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { paused = !paused },
                    onDoubleTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!liked) onToggleFavorite()
                    }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // heart pop on double-tap like
        LaunchedEffect(liked) {
            if (liked) {
                heartScale.snapTo(0.4f)
                heartScale.animateTo(1.2f, tween(180))
                heartScale.animateTo(1f, tween(120))
            }
        }
        if (heartScale.value > 0.01f) {
            Icon(
                Icons.Default.Favorite, null, tint = Color(0xFFFF4D6D),
                modifier = Modifier.align(Alignment.Center).size(96.dp)
                    .graphicsLayer(scaleX = heartScale.value, scaleY = heartScale.value, alpha = 1f - heartScale.value * 0.2f)
            )
        }
        if (paused) {
            Icon(
                Icons.Default.PlayArrow, null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(64.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(12.dp)
            )
        }

        // right action rail
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PopLikeButton(
                liked = liked,
                onToggle = onToggleFavorite,
                size = 32.dp,
                activeColor = Color(0xFFFF4D6D),
                inactiveColor = Color.White
            )
            IconButton(onClick = { muted = !muted }) {
                Icon(
                    if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    "Sound", tint = Color.White, modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, "More", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Add to album") }, onClick = { menu = false; onAddToAlbum() })
                DropdownMenuItem(text = { Text("Details") }, onClick = { menu = false; onDetails() })
                if (onWallpaper != null) {
                    DropdownMenuItem(text = { Text("Set as wallpaper") }, onClick = { menu = false; onWallpaper() })
                }
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }

        // bottom info
        if (showInfo) {
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                GradientScrim(Modifier.fillMaxWidth().height(140.dp))
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 80.dp, bottom = 104.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(item.albumName.ifBlank { "Reels" }, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${FormatUtils.formatDate(item.dateTaken)} · ${FormatUtils.formatDuration(item.durationMs)}" +
                        if (item.width > 0) " · ${item.width}×${item.height}" else "",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    item.name, color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1
                )
            }
        }
        // details affordance
        Row(Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 8.dp)) {
            IconButton(onClick = onDetails) {
                Icon(Icons.Default.Info, "Details", tint = Color.White.copy(alpha = 0.9f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.width(4.dp))
        }
        // playback progress
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(brandHorizontal)
            )
        }
    }
}
