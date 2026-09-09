package com.freedu.personalgallary.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** On-device EXIF GPS reader (images only; fully offline). */
object ExifUtils {
    /** Returns (latitude, longitude) or null if absent/unreadable. */
    fun latLong(context: Context, uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                val exif = ExifInterface(ins)
                val ll = exif.latLong ?: return null
                if (ll[0] == 0.0 && ll[1] == 0.0) null else ll[0] to ll[1]
            }
        } catch (_: Exception) {
            null
        }
    }
}

data class PlaceCluster(
    val key: String,
    val label: String,
    val count: Int,
    val cover: com.freedu.personalgallary.data.model.MediaItem?,
    val newestTs: Long,
    val itemIds: List<Long>
)
