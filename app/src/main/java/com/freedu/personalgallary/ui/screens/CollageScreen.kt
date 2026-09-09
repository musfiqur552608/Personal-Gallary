package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.util.CollageBg
import com.freedu.personalgallary.util.CollageRenderer
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.launch

/** Pick up to 9 photos → styled square collage → saved to the gallery. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageScreen(
    items: List<MediaItem>,
    onDone: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var bg by remember { mutableStateOf(CollageBg.GRADIENT) }
    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picked = remember(selected, items) {
        val byId = items.associateBy { it.id }
        selected.mapNotNull { byId[it] }
    }

    LaunchedEffect(picked, bg, step) {
        if (step == 1 && picked.size >= 2) {
            preview = CollageRenderer.render(context, picked, bg, 900)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(
                if (step == 0) "Pick photos (up to 9)" else "Style your collage",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (step == 0) {
                Text(
                    "${selected.size}/9",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.padding(4.dp))
                TextButton(
                    onClick = { step = 1 },
                    enabled = selected.size >= 2
                ) { Text("Next") }
            } else {
                TextButton(onClick = { step = 0 }) { Text("Photos") }
            }
        }
        if (step == 0) {
            val photos = remember(items) { items.filter { !it.isVideo } }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(photos, key = { it.id }) { item ->
                    val sel = item.id in selected
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (sel) 3.dp else 0.dp,
                                if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selected = if (sel) selected - item.id
                                else if (selected.size < 9) selected + item.id
                                else selected
                            }
                    ) {
                        MediaThumb(item, Modifier.fillMaxSize(), showBadge = false)
                        if (sel) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(0.dp))
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CollageBg.entries.forEach { option ->
                    FilterChip(
                        selected = bg == option,
                        onClick = { bg = option },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bmp = preview
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Collage preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else {
                    CircularProgressIndicator(Modifier.padding(48.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val full = CollageRenderer.render(context, picked, bg, 2000)
                                val uri = if (full != null) {
                                    ImageEditUtils.saveBitmapToGallery(context, full, "PG_COLLAGE_${System.currentTimeMillis()}.jpg")
                                } else null
                                full?.recycle()
                                busy = false
                                onDone(uri != null)
                            } catch (_: Exception) {
                                busy = false
                                onDone(false)
                            }
                        }
                    },
                    enabled = !busy && picked.size >= 2
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Save collage")
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
