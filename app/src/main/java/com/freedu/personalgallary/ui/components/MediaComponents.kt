package com.freedu.personalgallary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.ui.theme.storyRing
import com.freedu.personalgallary.util.FormatUtils

@Composable
fun MediaThumb(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showBadge: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val req = ImageRequest.Builder(LocalContext.current)
        .data(item.uri)
        .crossfade(true)
        .build()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        AsyncImage(
            model = req,
            contentDescription = item.name,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo && showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
    }
}

/** Instagram-style story avatar with gradient ring. */
@Composable
fun StoryAvatar(
    item: MediaItem,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(storyRing)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(3.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            MediaThumb(item = item, modifier = Modifier.fillMaxSize(), showBadge = false)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Heart button with springy pop + haptic tick. */
@Composable
fun PopLikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val haptics = LocalHapticFeedback.current
    var popKey by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(popKey) {
        if (popKey > 0) {
            scale.snapTo(0.6f)
            scale.animateTo(1.3f, tween(130))
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    IconButton(
        onClick = {
            popKey++
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        modifier = modifier
    ) {
        Icon(
            if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            "Favorite",
            tint = if (liked) activeColor else inactiveColor,
            modifier = Modifier
                .size(size)
                .scale(scale.value)
        )
    }
}

/**
 * Wraps media so single-tap opens and double-tap likes with a heart burst + haptic.
 * (Burst overlay is drawn centered.)
 */
@Composable
fun DoubleTapLikeBox(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var burstKey by remember { mutableIntStateOf(0) }
    var bursting by remember { mutableIntStateOf(0) }
    val burstScale = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }
    LaunchedEffect(burstKey) {
        if (burstKey == 0) return@LaunchedEffect
        bursting = 1
        burstScale.snapTo(0.3f)
        burstAlpha.snapTo(1f)
        burstScale.animateTo(1.25f, tween(180))
        burstScale.animateTo(1f, tween(100))
        burstAlpha.animateTo(0f, tween(300))
        bursting = 0
    }
    Box(
        modifier = modifier.pointerInput(onTap, onDoubleTap) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    burstKey++
                    onDoubleTap()
                }
            )
        }
    ) {
        content()
        if (bursting == 1) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4D6D).copy(
                        alpha = burstAlpha.value.coerceIn(0f, 1f)
                    ),
                    modifier = Modifier
                        .size((96 * burstScale.value.coerceAtLeast(0.01f)).dp)
                )
            }
        }
    }
}

/** Skeleton shimmer for the feed while the library is indexing. */
@Composable
fun ShimmerFeed() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val alpha by t.animateFloat(
        0.35f, 0.85f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Box(
                    Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(bg)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

/** Skeleton shimmer grid. */
@Composable
fun ShimmerGrid() {
    val t = rememberInfiniteTransition(label = "shimmerGrid")
    val alpha by t.animateFloat(
        0.35f, 0.85f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(24) {
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
            )
        }
    }
}

/** Sticky month section header for the gallery grid. */
@Composable
fun MonthHeader(label: String, count: Int) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)) {
        androidx.compose.foundation.layout.Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Primary call-to-action with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) brandHorizontal
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(enabled = enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.HideImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GradientScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
            )
        )
    )
}
