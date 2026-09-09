package com.freedu.personalgallary.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scoped-storage compliant indexer. Reads MediaStore only, never copies files.
 * Images + videos across Camera, WhatsApp, Downloads, Screenshots, etc.
 */
class MediaStoreRepository(private val context: Context) {

    data class ScanResult(
        val items: List<MediaItem>,
        val truncated: Boolean = false
    )

    suspend fun scanAll(
        excludedAlbums: Set<String> = emptySet(),
        onProgress: ((done: Int) -> Unit)? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        val items = ArrayList<MediaItem>(4096)
        items += queryImages(excludedAlbums, onProgress)
        items += queryVideos(excludedAlbums, onProgress)
        // Newest first — Instagram-feed feel
        items.sortWith(compareByDescending<MediaItem> { it.dateTaken }.thenByDescending { it.dateAdded })
        ScanResult(items)
    }

    private fun queryImages(excluded: Set<String>, onProgress: ((Int) -> Unit)?): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val relCol = if (Build.VERSION.SDK_INT >= 29) {
                c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            } else -1
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val bucket = c.getString(bucketCol).orEmpty()
                if (bucket in excluded) continue
                val addedSec = c.getLong(addedCol)
                var taken = try { c.getLong(takenCol) } catch (_: Exception) { 0L }
                if (taken <= 0) taken = addedSec * 1000
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol).orEmpty(),
                    type = MediaType.IMAGE,
                    mimeType = c.getString(mimeCol).orEmpty(),
                    dateTaken = taken,
                    dateAdded = addedSec * 1000,
                    width = try { c.getInt(wCol) } catch (_: Exception) { 0 },
                    height = try { c.getInt(hCol) } catch (_: Exception) { 0 },
                    sizeBytes = try { c.getLong(sizeCol) } catch (_: Exception) { 0L },
                    albumName = bucket.ifBlank { "Camera" },
                    relativePath = if (relCol >= 0) c.getString(relCol).orEmpty() else ""
                )
                if (out.size % 500 == 0) onProgress?.invoke(out.size)
            }
        }
        return out
    }

    private fun queryVideos(excluded: Set<String>, onProgress: ((Int) -> Unit)?): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val relCol = if (Build.VERSION.SDK_INT >= 29) {
                c.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            } else -1
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val bucket = c.getString(bucketCol).orEmpty()
                if (bucket in excluded) continue
                val addedSec = c.getLong(addedCol)
                var taken = try { c.getLong(takenCol) } catch (_: Exception) { 0L }
                if (taken <= 0) taken = addedSec * 1000
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol).orEmpty(),
                    type = MediaType.VIDEO,
                    mimeType = c.getString(mimeCol).orEmpty(),
                    dateTaken = taken,
                    dateAdded = addedSec * 1000,
                    durationMs = try { c.getLong(durCol) } catch (_: Exception) { 0L },
                    width = try { c.getInt(wCol) } catch (_: Exception) { 0 },
                    height = try { c.getInt(hCol) } catch (_: Exception) { 0 },
                    sizeBytes = try { c.getLong(sizeCol) } catch (_: Exception) { 0L },
                    albumName = bucket.ifBlank { "Camera" },
                    relativePath = if (relCol >= 0) c.getString(relCol).orEmpty() else ""
                )
                if (out.size % 200 == 0) onProgress?.invoke(out.size)
            }
        }
        return out
    }

    /** Delete via MediaStore (user-confirmed batch ops). Returns rows deleted. */
    suspend fun delete(item: MediaItem): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(item.uri, null, null)
        } catch (_: SecurityException) {
            0
        } catch (_: Exception) {
            0
        }
    }
}
