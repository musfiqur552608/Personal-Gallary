package com.freedu.personalgallary.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import com.freedu.personalgallary.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareUtils {
    fun share(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        val uris = ArrayList(items.map { it.uri })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = items.first().mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }
}

object WallpaperHelper {
    suspend fun setAsWallpaper(context: Context, item: MediaItem): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val wm = WallpaperManager.getInstance(context)
                context.contentResolver.openInputStream(item.uri)?.use { ins ->
                    val bmp = BitmapFactory.decodeStream(ins) ?: return@withContext false
                    wm.setBitmap(bmp)
                }
                true
            } catch (_: Exception) {
                false
            }
        }
}
