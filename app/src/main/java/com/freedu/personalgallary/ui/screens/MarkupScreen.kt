package com.freedu.personalgallary.ui.screens

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StrokePts(val points: List<Offset>, val color: Color, val widthFrac: Float)
private data class Stamp(val text: String, var rel: Offset, val sizeFrac: Float, val color: Color)

private val MARK_COLORS = listOf(
    Color.White, Color(0xFFFF4D6D), Color(0xFFFFD23F),
    Color(0xFF2ED573), Color(0xFF4D96FF), Color.Black
)

/** Annotate screenshots/photos: freehand draw + movable text notes. Saves a NEW copy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkupScreen(
    item: MediaItem,
    onDone: (Boolean, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val strokes = remember { mutableStateListOf<StrokePts>() }
    val stamps = remember { mutableStateListOf<Stamp>() }
    var drawMode by remember { mutableStateOf(true) }
    var color by remember { mutableStateOf(Color(0xFFFF4D6D)) }
    var widthDp by remember { mutableFloatStateOf(8f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var noteText by remember { mutableStateOf("") }
    var noteSize by remember { mutableFloatStateOf(0.055f) }
    var busy by remember { mutableStateOf(false) }
    var dragStamp by remember { mutableStateOf<Int?>(null) }
    var dragPoints by remember { mutableStateOf(listOf<Offset>()) }
    val baseImage: ImageBitmap? = remember(base) { base?.asImageBitmap() }

    LaunchedEffect(item.uri) {
        base = withContext(Dispatchers.IO) {
            ImageEditUtils.decodeSampled(context, item.uri, 1280)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Markup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                enabled = strokes.isNotEmpty()
            ) { Icon(Icons.Default.Undo, "Undo stroke") }
            if (busy) {
                CircularProgressIndicator(Modifier.padding(end = 12.dp))
            } else {
                TextButton(
                    onClick = {
                        val bmp = base ?: return@TextButton
                        busy = true
                        scope.launch(Dispatchers.IO) {
                            var savedName: String? = null
                            try {
                                val out = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                val canvas = android.graphics.Canvas(out)
                                strokes.forEach { s ->
                                    val paint = AndroidPaint().apply {
                                        this.color = s.color.toArgb()
                                        style = AndroidPaint.Style.STROKE
                                        strokeWidth = s.widthFrac * out.width
                                        strokeCap = AndroidPaint.Cap.ROUND
                                        strokeJoin = AndroidPaint.Join.ROUND
                                        isAntiAlias = true
                                    }
                                    val path = android.graphics.Path()
                                    s.points.forEachIndexed { i, p ->
                                        val x = p.x * out.width
                                        val y = p.y * out.height
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    canvas.drawPath(path, paint)
                                }
                                stamps.forEach { st ->
                                    val paint = AndroidPaint().apply {
                                        this.color = st.color.toArgb()
                                        textSize = st.sizeFrac * out.width
                                        typeface = android.graphics.Typeface.create(
                                            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
                                        )
                                        textAlign = AndroidPaint.Align.LEFT
                                        isAntiAlias = true
                                        setShadowLayer(6f, 0f, 2f, 0xAA000000.toInt())
                                    }
                                    // LEFT/top anchored like the preview; compensate baseline
                                    canvas.drawText(
                                        st.text,
                                        st.rel.x * out.width,
                                        st.rel.y * out.height - paint.ascent(),
                                        paint
                                    )
                                }
                                val name = "PG_MARKUP_${System.currentTimeMillis()}.jpg"
                                val uri = ImageEditUtils.saveBitmapToGallery(
                                    context, out, name
                                )
                                out.recycle()
                                if (uri != null) savedName = name
                            } catch (_: Exception) {
                            }
                            val done = savedName
                            withContext(Dispatchers.Main) {
                                busy = false
                                onDone(done != null, done)
                            }
                        }
                    },
                    enabled = base != null && (strokes.isNotEmpty() || stamps.isNotEmpty())
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 4.dp))
                    Text("Save")
                }
            }
        }

        val bmp = base
        if (bmp == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
            ) {
                baseImage?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewSize = it }
                        .pointerInput(drawMode, color, widthDp) {
                        detectDragGestures(
                            onDragStart = { start ->
                                if (drawMode || viewSize == IntSize.Zero) return@detectDragGestures
                                val rel = Offset(
                                    start.x / viewSize.width,
                                    start.y / viewSize.height
                                )
                                dragStamp = stamps.indexOfFirst {
                                    (it.rel - rel).getDistance() < 0.09f
                                }.takeIf { it >= 0 }
                            },
                            onDrag = { change, _ ->
                                if (viewSize == IntSize.Zero) return@detectDragGestures
                                val rel = Offset(
                                    (change.position.x / viewSize.width).coerceIn(0f, 1f),
                                    (change.position.y / viewSize.height).coerceIn(0f, 1f)
                                )
                                if (drawMode) {
                                    val cur = dragPoints
                                    dragPoints = cur + rel
                                } else {
                                    dragStamp?.let { idx ->
                                        stamps[idx] = stamps[idx].copy(rel = rel)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (drawMode && dragPoints.size > 1) {
                                    strokes.add(
                                        StrokePts(dragPoints.toList(), color, widthDp / 1000f)
                                    )
                                }
                                dragPoints = emptyList()
                                dragStamp = null
                            },
                            onDragCancel = {
                                dragPoints = emptyList()
                                dragStamp = null
                            }
                        )
                    }
            ) {
                // draw strokes
                strokes.forEach { s ->
                    if (s.points.size > 1) {
                        val path = Path().apply {
                            s.points.forEachIndexed { i, p ->
                                val abs = Offset(p.x * size.width, p.y * size.height)
                                if (i == 0) moveTo(abs.x, abs.y) else lineTo(abs.x, abs.y)
                            }
                        }
                        drawPath(
                            path, s.color,
                            style = Stroke(
                                width = s.widthFrac * size.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
                // live stroke
                if (dragPoints.size > 1) {
                    val path = Path().apply {
                        dragPoints.forEachIndexed { i, p ->
                            val abs = Offset(p.x * size.width, p.y * size.height)
                            if (i == 0) moveTo(abs.x, abs.y) else lineTo(abs.x, abs.y)
                        }
                    }
                    drawPath(
                        path, color,
                        style = Stroke(
                            width = (widthDp / 1000f) * size.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
                // note overlay (top-left anchored — matches the saved render)
                stamps.forEach { st ->
                    Text(
                        st.text,
                        color = st.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = (st.sizeFrac * maxWidth.value).sp,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier.offset(
                            x = (st.rel.x * maxWidth.value).dp,
                            y = (st.rel.y * maxHeight.value).dp
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (drawMode) "Draw with your finger" else "Drag notes to move them",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // controls
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = drawMode,
                onClick = { drawMode = true },
                label = { Text("Draw") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = !drawMode,
                onClick = { drawMode = false },
                label = { Text("Notes") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
            Spacer(Modifier.weight(1f))
            if (stamps.isNotEmpty()) {
                TextButton(onClick = { stamps.clear() }) { Text("Clear notes") }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MARK_COLORS.forEach { c ->
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (color == c) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { color = c }
                )
            }
        }
        if (drawMode) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Size", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = widthDp,
                    onValueChange = { widthDp = it },
                    valueRange = 2f..24f,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Note text…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.padding(4.dp))
                TextButton(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            stamps.add(
                                Stamp(
                                    noteText.trim(),
                                    Offset(0.5f, 0.18f + 0.09f * stamps.size),
                                    noteSize, color
                                )
                            )
                            noteText = ""
                        }
                    },
                    enabled = noteText.isNotBlank()
                ) { Text("Add") }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
