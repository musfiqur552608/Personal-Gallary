package com.freedu.personalgallary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.freedu.personalgallary.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.sqrt

enum class CollageBg { WHITE, BLACK, GRADIENT }

/** Renders a square photo collage bitmap fully on-device. */
object CollageRenderer {

    suspend fun render(
        context: Context,
        items: List<MediaItem>,
        bg: CollageBg,
        outSize: Int = 2000
    ): Bitmap? = withContext(Dispatchers.IO) {
        val take = items.take(9)
        if (take.isEmpty()) return@withContext null
        try {
            val n = take.size
            val cols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
            val rows = ceil(n.toDouble() / cols).toInt().coerceAtLeast(1)
            val out = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            // background
            val bgPaint = Paint()
            when (bg) {
                CollageBg.WHITE -> {
                    bgPaint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(0f, 0f, outSize.toFloat(), outSize.toFloat(), bgPaint)
                }
                CollageBg.BLACK -> {
                    bgPaint.color = 0xFF111116.toInt()
                    canvas.drawRect(0f, 0f, outSize.toFloat(), outSize.toFloat(), bgPaint)
                }
                CollageBg.GRADIENT -> {
                    bgPaint.shader = LinearGradient(
                        0f, 0f, outSize.toFloat(), outSize.toFloat(),
                        intArrayOf(0xFF7B5CFF.toInt(), 0xFFFF4D6D.toInt(), 0xFFFF8A3D.toInt()),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, outSize.toFloat(), outSize.toFloat(), bgPaint)
                }
            }
            val gap = (outSize * 0.008f).coerceAtLeast(4f)
            val cellW = (outSize - gap * (cols + 1)) / cols
            val cellH = (outSize - gap * (rows + 1)) / rows
            take.forEachIndexed { index, item ->
                val r = index / cols
                val c = index % cols
                val left = gap + c * (cellW + gap)
                val top = gap + r * (cellH + gap)
                val bmp = ImageEditUtils.decodeSampled(
                    context, item.uri,
                    (maxOf(cellW, cellH) * 1.2f).toInt().coerceAtLeast(256)
                ) ?: return@forEachIndexed
                ImageEditUtils.drawCenterCrop(canvas, bmp, left, top, cellW, cellH)
                if (bmp != out) bmp.recycle()
            }
            out
        } catch (_: Exception) {
            null
        }
    }
}
