package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.freedu.personalgallary.data.model.MediaItem

/** Before/after drag slider for original vs edited copy. */
@Composable
fun CompareScreen(
    before: MediaItem,
    after: MediaItem,
    onBack: () -> Unit
) {
    var frac by remember { mutableFloatStateOf(0.5f) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("Before / After", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("drag the handle", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                    }
                }
        ) {
            val w = maxWidth
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            frac = (frac + dragAmount / size.width).coerceIn(0.05f, 0.95f)
                        }
                    }
            ) {
                AsyncImage(
                    model = after.uri, contentDescription = "Edited",
                    contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(w * frac)
                        .clipToBounds()
                ) {
                    AsyncImage(
                        model = before.uri, contentDescription = "Original",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(w)
                    )
                }
                // divider + labels
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = (w * frac) - 1.5.dp)
                        .background(Color.White)
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = (w * frac) - 22.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↔", fontWeight = FontWeight.Bold)
                }
                Text(
                    "BEFORE", color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    "AFTER", color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
