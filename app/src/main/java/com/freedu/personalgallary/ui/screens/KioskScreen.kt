package com.freedu.personalgallary.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.delay

/**
 * Guest kiosk: locked full-screen show for handing the phone around.
 * System bars hidden; back press asks for the exit action (PIN gate).
 */
@Composable
fun KioskScreen(
    items: List<MediaItem>,
    title: String,
    onExitRequest: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onExitRequest() }

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val prevBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.systemBarsBehavior = prevBehavior
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Nothing to show", color = Color.White)
        }
        return
    }
    val pager = rememberPagerState(pageCount = { items.size })
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(paused, pager.currentPage) {
        if (paused) return@LaunchedEffect
        delay(5000)
        try {
            pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        } catch (_: Exception) {
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { paused = !paused }
                )
            }
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            val target = if (page == pager.currentPage) 1.14f else 1f
            val zoom by animateFloatAsState(target, tween(5000), label = "kiosk")
            AsyncImage(
                model = item.uri, contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = zoom, scaleY = zoom)
            )
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleSmall)
            Text(
                "${pager.currentPage + 1} / ${items.size} · tap to pause",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        val current = items[pager.currentPage]
        Text(
            FormatUtils.formatShortDate(current.dateTaken),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}
