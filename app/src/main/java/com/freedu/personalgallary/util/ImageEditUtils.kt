package com.freedu.personalgallary.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Decode + filter + save helpers. Edited results are always saved as NEW files (originals preserved). */
object ImageEditUtils {

    fun decodeSampled(context: Context, uri: Uri, reqSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val (w, h) = bounds.outWidth to bounds.outHeight
            if (w <= 0 || h <= 0) return null
            while (w / sample > reqSize || h / sample > reqSize) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    enum class Preset { NORMAL, MONO, WARM, VIVID, COOL }

    data class Tune(val brightness: Int = 0, val contrast: Int = 100, val saturation: Int = 100)

    /** brightness -100..100, contrast/saturation in percent. */
    fun applyTune(src: Bitmap, preset: Preset, tune: Tune): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val sat = tune.saturation / 100f
        val con = tune.contrast / 100f
        val bri = tune.brightness * 2.55f
        val cm = ColorMatrix()
        // saturation
        val satM = ColorMatrix().apply { setSaturation(sat) }
        cm.postConcat(satM)
        // contrast around 128 + brightness
        val contrastM = ColorMatrix(
            floatArrayOf(
                con, 0f, 0f, 0f, (1 - con) * 128f + bri,
                0f, con, 0f, 0f, (1 - con) * 128f + bri,
                0f, 0f, con, 0f, (1 - con) * 128f + bri,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastM)
        // preset tint
        val tint = when (preset) {
            Preset.NORMAL -> null
            Preset.MONO -> ColorMatrix().apply { setSaturation(0f) }
            Preset.WARM -> ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, 6f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 0.88f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            Preset.VIVID -> ColorMatrix().apply {
                setSaturation(1.45f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.08f, 0f, 0f, 0f, -4f,
                            0f, 1.08f, 0f, 0f, -4f,
                            0f, 0f, 1.08f, 0f, -4f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            Preset.COOL -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, -4f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.12f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        if (tint != null) cm.postConcat(tint)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        c.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** centerCrop src into dst rect of canvas. */
    fun drawCenterCrop(canvas: Canvas, src: Bitmap, left: Float, top: Float, w: Float, h: Float) {
        val scale = maxOf(w / src.width, h / src.height)
        val dw = src.width * scale
        val dh = src.height * scale
        val dx = left + (w - dw) / 2f
        val dy = top + (h - dh) / 2f
        canvas.drawBitmap(
            src,
            null,
            android.graphics.RectF(dx, dy, dx + dw, dy + dh),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bmp: Bitmap,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val stream = java.io.ByteArrayOutputStream()
        val flat = flattenAlpha(bmp)
        flat.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        if (flat !== bmp) {
            try {
                flat.recycle()
            } catch (_: Exception) {
            }
        }
        saveJpegBytes(context, stream.toByteArray(), displayName)
    }

    /** Renders [uri] bounded by [maxDim] px at JPEG [quality] into memory (for estimates). */
    suspend fun compressToBytes(
        context: Context,
        uri: Uri,
        maxDim: Int,
        quality: Int
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val bmp = decodeSampled(context, uri, maxDim) ?: return@withContext null
            val flat = flattenAlpha(bmp)
            val stream = java.io.ByteArrayOutputStream()
            flat.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 95), stream)
            try {
                bmp.recycle()
                if (flat !== bmp) flat.recycle()
            } catch (_: Exception) {
            }
            stream.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /** JPEG (opaque) needs no alpha — paint transparent sources onto white. */
    private fun flattenAlpha(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(android.graphics.Color.WHITE)
        c.drawBitmap(src, 0f, 0f, null)
        return out
    }

    suspend fun saveJpegBytes(
        context: Context,
        bytes: ByteArray,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val name = displayName.ifBlank {
                "PG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/PersonalGallary"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val uri = resolver.insert(collection, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
            } ?: run {
                resolver.delete(uri, null, null)
                return@withContext null
            }
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    fun dpToPx(v: Float, density: Float): Float = v * density

    fun sampleCount(w: Int, h: Int, req: Int): Int {
        var s = 1
        while (w / s > req || h / s > req) s *= 2
        return s
    }
}
