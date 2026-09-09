package com.freedu.personalgallary.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.freedu.personalgallary.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders an album as a local PDF photo book (no network, framework PdfDocument). */
object PdfExport {

    suspend fun exportAlbum(
        context: Context,
        title: String,
        items: List<MediaItem>,
        maxPhotos: Int = 60
    ): Uri? = withContext(Dispatchers.IO) {
        val take = items.take(maxPhotos)
        if (take.isEmpty()) return@withContext null
        try {
            val doc = PdfDocument()
            val pageW = 595
            val pageH = 842
            val margin = 40
            var photoIndex = 0
            var pageNum = 1
            // cover page
            run {
                val coverBuilder: PdfDocument.PageInfo.Builder =
                    PdfDocument.PageInfo.Builder(pageW, pageH, pageNum)
                pageNum++
                val info: PdfDocument.PageInfo = coverBuilder.create()
                val page = doc.startPage(info)
                val c = page.canvas
                val bg = Paint().apply { color = 0xFF1B1233.toInt() }
                c.drawRect(0f, 0f, pageW.toFloat(), pageH.toFloat(), bg)
                val titlePaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val subPaint = Paint().apply { color = 0xFFB9A8FF.toInt(); textSize = 14f }
                c.drawText("Personal Gallary", margin.toFloat(), 120f, subPaint)
                // simple word-wrap for title
                var y = 170f
                title.chunked(28).take(3).forEach { line ->
                    c.drawText(line, margin.toFloat(), y, titlePaint)
                    y += 38f
                }
                val meta = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 13f }
                c.drawText(
                    "${take.size} photos · ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())}",
                    margin.toFloat(), y + 10f, meta
                )
                doc.finishPage(page)
            }
            // photo pages: 2 per page
            while (photoIndex < take.size) {
                val pageBuilder: PdfDocument.PageInfo.Builder =
                    PdfDocument.PageInfo.Builder(pageW, pageH, pageNum)
                pageNum++
                val info: PdfDocument.PageInfo = pageBuilder.create()
                val page = doc.startPage(info)
                val c = page.canvas
                c.drawColor(0xFFFFFFFF.toInt())
                var y = margin.toFloat()
                repeat(2) {
                    if (photoIndex >= take.size) return@repeat
                    val item = take[photoIndex++]
                    val bmp = ImageEditUtils.decodeSampled(context, item.uri, 900)
                    val boxH = 320f
                    if (bmp != null) {
                        val scale = minOf(
                            (pageW - margin * 2).toFloat() / bmp.width,
                            boxH / bmp.height
                        )
                        val dw = bmp.width * scale
                        val dh = bmp.height * scale
                        val dx = (pageW - dw) / 2f
                        c.drawBitmap(bmp, null, RectF(dx, y, dx + dw, y + dh), Paint(Paint.FILTER_BITMAP_FLAG))
                        bmp.recycle()
                        y += dh + 8f
                    }
                    val cap = Paint().apply { color = 0xFF555555.toInt(); textSize = 11f }
                    val label = "${FormatUtils.formatShortDate(item.dateTaken)} · ${item.albumName}"
                    c.drawText(label.take(70), margin.toFloat(), y, cap)
                    y += 34f
                }
                doc.finishPage(page)
            }
            // write to Documents
            val name = "PG_${title.filter { it.isLetterOrDigit() }.take(24)}_" +
                "${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, name)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(
                        MediaStore.Files.FileColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/PersonalGallary"
                    )
                    put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val uri = resolver.insert(collection, values) ?: run { doc.close(); return@withContext null }
            resolver.openOutputStream(uri)?.use { out ->
                doc.writeTo(out)
            }
            doc.close()
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    fun drawCenteredTextPlaceholder(): Bitmap {
        return Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    }
}
