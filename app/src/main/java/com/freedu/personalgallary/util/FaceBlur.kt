package com.freedu.personalgallary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import com.freedu.personalgallary.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Blur-faces-before-share: pixelates detected face regions into throwaway
 * cache copies shared via FileProvider. Originals are never modified.
 */
object FaceBlur {

    sealed interface Prepared {
        data class Blurred(val uri: Uri) : Prepared
        data object NoFaces : Prepared
        data object Failed : Prepared
    }

    suspend fun prepare(
        context: Context,
        item: MediaItem
    ): Prepared = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext Prepared.NoFaces
        try {
            val boxes = FaceScan.boxes(context, item.uri).getOrElse {
                return@withContext Prepared.Failed
            }
            if (boxes.isEmpty()) return@withContext Prepared.NoFaces
            // original dimensions (detection space) + bounded bitmap
            var origW = 0
            var origH = 0
            context.contentResolver.openInputStream(item.uri)?.use { ins ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(ins, null, bounds)
                origW = bounds.outWidth
                origH = bounds.outHeight
            }
            if (origW <= 0 || origH <= 0) return@withContext Prepared.Failed
            val decoded = decodeBounded(context, item.uri, 2048) ?: return@withContext Prepared.Failed
            val out = if (decoded.isMutable) decoded
            else decoded.copy(Bitmap.Config.ARGB_8888, true).also { decoded.recycle() }
            val sx = out.width.toFloat() / origW
            val sy = out.height.toFloat() / origH
            boxes.forEach { box ->
                val mapped = Rect(
                    (box.left * sx).toInt(),
                    (box.top * sy).toInt(),
                    (box.right * sx).toInt(),
                    (box.bottom * sy).toInt()
                )
                pixelate(out, mapped, blocks = 14)
            }
            val uri = saveCache(context, out, item.id)
            try {
                out.recycle()
            } catch (_: Exception) {
            }
            if (uri != null) Prepared.Blurred(uri) else Prepared.Failed
        } catch (_: Exception) {
            Prepared.Failed
        }
    }

    private fun decodeBounded(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pixelate(bmp: Bitmap, box: Rect, blocks: Int) {
        if (box.isEmpty) return
        // grow ~12% for margin, clamp
        val gw = ((box.width() * 0.12f).toInt()).coerceAtLeast(2)
        val gh = ((box.height() * 0.12f).toInt()).coerceAtLeast(2)
        val l = (box.left - gw).coerceIn(0, bmp.width - 1)
        val t = (box.top - gh).coerceIn(0, bmp.height - 1)
        val r = (box.right + gw).coerceIn(l + 1, bmp.width)
        val b = (box.bottom + gh).coerceIn(t + 1, bmp.height)
        val w = r - l
        val h = b - t
        if (w < 4 || h < 4) return
        try {
            val crop = Bitmap.createBitmap(bmp, l, t, w, h)
            val bw = blocks.coerceIn(4, 24)
            val bh = max(4, (bw * h.toFloat() / w).toInt())
            val small = Bitmap.createScaledBitmap(crop, bw, bh, false)
            if (small != crop) crop.recycle()
            val blown = Bitmap.createScaledBitmap(small, w, h, false)
            if (blown != small) small.recycle()
            Canvas(bmp).drawBitmap(blown, l.toFloat(), t.toFloat(), null)
            blown.recycle()
        } catch (_: Exception) {
        }
    }

    private fun saveCache(context: Context, bmp: Bitmap, id: Long): Uri? {
        return try {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            // prune stale copies
            dir.listFiles()?.forEach {
                if (System.currentTimeMillis() - it.lastModified() > 86_400_000L) {
                    try {
                        it.delete()
                    } catch (_: Exception) {
                    }
                }
            }
            val file = File(dir, "blur_${id}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)) return null
            }
            FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
        } catch (_: Exception) {
            null
        }
    }
}
