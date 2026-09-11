package com.freedu.personalgallary.ui.screens

import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Compress video: downscale + H.264 re-encode (Media3 Transformer, offline). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCompressDialog(
    item: MediaItem,
    onDone: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var srcW by remember { mutableStateOf(item.width) }
    var srcH by remember { mutableStateOf(item.height) }
    var targetH by remember { mutableStateOf(720) }
    var mute by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    // probe real dimensions (MediaStore values can be 0)
    LaunchedEffect(item.uri) {
        if (srcW <= 0 || srcH <= 0) {
            val (w, h) = withContext(Dispatchers.IO) {
                try {
                    val r = MediaMetadataRetriever()
                    r.setDataSource(context, item.uri)
                    val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    r.release()
                    w to h
                } catch (_: Exception) {
                    0 to 0
                }
            }
            if (w > 0 && h > 0) {
                srcW = w
                srcH = h
            }
        }
    }

    val aspect = if (srcW > 0 && srcH > 0) srcW.toFloat() / srcH else 16f / 9f
    val options = remember(srcH) {
        // only offer sizes smaller than the source (plus re-encode fallback)
        listOf(1080, 720, 480).filter { it < srcH || srcH <= 0 }
    }
    LaunchedEffect(options) {
        if (targetH !in options && options.isNotEmpty()) targetH = options.first()
    }
    fun outW(h: Int): Int = ((h * aspect).toInt() / 2 * 2).coerceAtLeast(2)

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var pendingOut by remember { mutableStateOf<File?>(null) }
    val transformer = remember {
        Transformer.Builder(context)
            .setTransformationRequest(
                TransformationRequest.Builder()
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    mainHandler.post {
                        busy = false
                        status = null
                        pendingOut?.let { f ->
                            MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), null, null)
                        }
                        pendingOut = null
                        onDone(true)
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    mainHandler.post {
                        busy = false
                        status = "Export failed: ${exportException.message}"
                    }
                }
            })
            .build()
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                transformer.cancel()
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Compress video", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "${item.name}\n${FormatUtils.formatBytes(item.sizeBytes)} · ${FormatUtils.formatDuration(item.durationMs)}" +
                        if (srcW > 0) " · ${srcW}×${srcH}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("Quality", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                if (options.isEmpty()) {
                    Text(
                        "Already small — will re-encode to H.264 only.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { h ->
                            FilterChip(
                                selected = targetH == h,
                                onClick = { targetH = h },
                                label = { Text("${h}p") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Output ≈ ${outW(targetH)} × $targetH · typically 40–70% smaller",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remove audio", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Even smaller, muted copy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = mute, onCheckedChange = { mute = it }, enabled = !busy)
                }
                if (busy) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.height(4.dp))
                    Text(status ?: "Compressing…", style = MaterialTheme.typography.bodySmall)
                } else if (status != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(status!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    busy = true
                    status = "Compressing…"
                    try {
                        val effects = if (options.isEmpty()) Effects.EMPTY
                        else Effects(emptyList(), listOf(Presentation.createForHeight(targetH)))
                        val edited = EditedMediaItem.Builder(ExoMediaItem.fromUri(item.uri))
                            .setEffects(effects)
                            .setRemoveAudio(mute)
                            .build()
                        val outFile = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                            "PG_COMPVID_${System.currentTimeMillis()}.mp4"
                        )
                        pendingOut = outFile
                        transformer.start(edited, outFile.absolutePath)
                    } catch (e: Exception) {
                        busy = false
                        status = "Couldn't start: ${e.message}"
                    }
                },
                enabled = !busy
            ) {
                if (busy) CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                else Text("Compress")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("Cancel") }
        }
    )
}
