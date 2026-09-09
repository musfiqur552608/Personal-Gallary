package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Non-destructive-look photo editor: presets + tune sliders, saves a NEW copy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    item: MediaItem,
    onDone: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var preset by remember { mutableStateOf(ImageEditUtils.Preset.NORMAL) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(100f) }
    var saturation by remember { mutableFloatStateOf(100f) }
    var comparing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(item.uri) {
        original = withContext(Dispatchers.IO) {
            ImageEditUtils.decodeSampled(context, item.uri, 1408)
        }
    }
    val tune = remember(brightness, contrast, saturation) {
        ImageEditUtils.Tune(brightness.toInt(), contrast.toInt(), saturation.toInt())
    }
    val preview = remember(original, preset, tune) {
        val src = original ?: return@remember null
        try {
            ImageEditUtils.applyTune(src, preset, tune)
        } catch (_: Exception) {
            null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Edit photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (busy) {
                CircularProgressIndicator(Modifier.padding(end = 12.dp))
            } else {
                TextButton(
                    onClick = {
                        val bmp = preview ?: return@TextButton
                        busy = true
                        scope.launch {
                            val uri = ImageEditUtils.saveBitmapToGallery(
                                context, bmp, "PG_EDIT_${System.currentTimeMillis()}.jpg"
                            )
                            busy = false
                            onDone(uri != null)
                        }
                    },
                    enabled = original != null
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 4.dp))
                    Text("Save copy")
                }
            }
        }
        val base = original
        if (base == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                comparing = true
                                tryAwaitRelease()
                                comparing = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val show = if (comparing) base else preview
                if (show != null) {
                    Image(
                        bitmap = show.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(base.width.toFloat() / base.height.toFloat())
                    )
                }
            }
            Text(
                "Touch & hold preview to compare with original",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ImageEditUtils.Preset.entries, key = { it.name }) { p ->
                FilterChip(
                    selected = preset == p,
                    onClick = { preset = p },
                    label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            TuneRow("Brightness", brightness, -100f..100f) { brightness = it }
            TuneRow("Contrast", contrast, 40f..200f) { contrast = it }
            TuneRow("Saturation", saturation, 0f..200f) { saturation = it }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun TuneRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(0.28f))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text("${value.toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(0.14f))
    }
}
