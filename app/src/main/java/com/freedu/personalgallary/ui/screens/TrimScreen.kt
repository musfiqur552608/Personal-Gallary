package com.freedu.personalgallary.ui.screens

import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Trim + mute + cover-frame for videos (Media3 Transformer, fully offline). */
@Composable
fun TrimScreen(
    item: MediaItem,
    onTrimDone: () -> Unit,
    onCoverSaved: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val durMs = item.durationMs.coerceAtLeast(1000L)
    var startMs by remember { mutableStateOf(0f) }
    var endMs by remember { mutableStateOf(durMs.toFloat()) }
    var mute by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingOut by remember { mutableStateOf<File?>(null) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val transformer = remember {
        Transformer.Builder(context).addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                mainHandler.post {
                    busy = false
                    status = null
                    pendingOut?.let { f ->
                        MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), null, null)
                    }
                    pendingOut = null
                    onTrimDone()
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
        }).build()
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                transformer.cancel()
            } catch (_: Exception) {
            }
        }
    }

    val trimmed = (endMs - startMs) < durMs - 500f
    val canExport = !busy && (trimmed || mute)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Trim video", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(
            "${FormatUtils.formatDuration(durMs)} · ${FormatUtils.formatBytes(item.sizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Text("Start: ${FormatUtils.formatDuration(startMs.toLong())}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = startMs,
            onValueChange = { startMs = it.coerceAtMost(endMs - 1000f).coerceAtLeast(0f) },
            valueRange = 0f..durMs.toFloat(),
            enabled = !busy
        )
        Spacer(Modifier.height(8.dp))
        Text("End: ${FormatUtils.formatDuration(endMs.toLong())}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = endMs,
            onValueChange = { endMs = it.coerceAtLeast(startMs + 1000f).coerceAtMost(durMs.toFloat()) },
            valueRange = 0f..durMs.toFloat(),
            enabled = !busy
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Kept: ${FormatUtils.formatDuration((endMs - startMs).toLong())}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Remove audio", fontWeight = FontWeight.SemiBold)
                Text(
                    "Export muted version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = mute, onCheckedChange = { mute = it }, enabled = !busy)
        }
        Spacer(Modifier.height(16.dp))
        Row {
            TextButton(
                onClick = {
                    busy = true
                    status = "Saving cover frame…"
                    scope.launch(Dispatchers.IO) {
                        var ok = false
                        try {
                            val r = MediaMetadataRetriever()
                            r.setDataSource(context, item.uri)
                            val frame = r.getFrameAtTime(
                                startMs.toLong() * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                            r.release()
                            if (frame != null) {
                                val uri = ImageEditUtils.saveBitmapToGallery(
                                    context, frame,
                                    "PG_COVER_${System.currentTimeMillis()}.jpg"
                                )
                                frame.recycle()
                                ok = uri != null
                            }
                        } catch (_: Exception) {
                            ok = false
                        }
                        val done = ok
                        withContext(Dispatchers.Main) {
                            busy = false
                            status = null
                            onCoverSaved(done)
                        }
                    }
                },
                enabled = !busy
            ) {
                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.padding(end = 6.dp))
                Text("Save cover frame")
            }
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = {
                busy = true
                status = "Exporting…"
                try {
                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(item.uri)
                        .setClippingConfiguration(
                            ExoMediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startMs.toLong())
                                .setEndPositionMs(endMs.toLong())
                                .build()
                        )
                        .build()
                    val edited = EditedMediaItem.Builder(mediaItem)
                        .setRemoveAudio(mute)
                        .build()
                    val outFile = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "PG_TRIM_${System.currentTimeMillis()}.mp4"
                    )
                    pendingOut = outFile
                    transformer.start(edited, outFile.absolutePath)
                } catch (e: Exception) {
                    busy = false
                    status = "Couldn't start export: ${e.message}"
                }
            },
            enabled = canExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.ContentCut, null, modifier = Modifier.padding(end = 8.dp))
            }
            Text(if (mute && !trimmed) "Export muted copy" else "Export trimmed copy")
        }
        if (status != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                status!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Exports are saved as new files — your original is never touched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
