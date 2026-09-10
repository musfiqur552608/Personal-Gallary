package com.freedu.personalgallary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/** Average-hash perceptual hashing for duplicate detection (fully on-device). */
object HashUtils {

    /** 64-bit aHash of a downscaled grayscale version, or null if unreadable. */
    fun ahash(context: Context, uri: Uri): Long? {
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val small = Bitmap.createScaledBitmap(bmp, 8, 8, true)
            if (small != bmp) bmp.recycle()
            val px = IntArray(64)
            small.getPixels(px, 0, 8, 0, 0, 8, 8)
            small.recycle()
            var sum = 0L
            val gray = IntArray(64) { i ->
                val p = px[i]
                val g = ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
                sum += g
                g
            }
            val avg = (sum / 64).toInt()
            var hash = 0L
            gray.forEach { g ->
                hash = (hash shl 1) or (if (g >= avg) 1L else 0L)
            }
            hash
        } catch (_: Exception) {
            null
        }
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    data class DupGroup(val ids: List<Long>, val kind: Kind) {
        enum class Kind { EXACT, SIMILAR }
    }

    /**
     * Groups items by image hash (exact + near via 16-bit bucket prefix + union-find).
     * Calls onProgress periodically. Images only — pass videos separately if needed.
     */
    suspend fun findDuplicates(
        context: Context,
        items: List<com.freedu.personalgallary.data.model.MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit,
        maxHamming: Int = 8
    ): List<DupGroup> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val hashes = ArrayList<Pair<Long, Long>>() // mediaId to hash
        items.forEachIndexed { i, item ->
            ahash(context, item.uri)?.let { hashes += item.id to it }
            if (i % 25 == 0) {
                val d = i
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onProgress(d, items.size)
                }
            }
        }
        // union-find over buckets keyed by top 16 bits
        val parent = HashMap<Long, Long>()
        fun find(x: Long): Long {
            var r = x
            while (parent[r] != r) r = parent[r]!!
            return r
        }
        hashes.forEach { (id, _) -> parent[id] = id }
        val buckets = hashes.groupBy { (_, h) -> h ushr 48 }
        buckets.values.forEach { bucket ->
            for (i in bucket.indices) {
                for (j in i + 1 until bucket.size) {
                    if (hamming(bucket[i].second, bucket[j].second) <= maxHamming) {
                        val ri = find(bucket[i].first)
                        val rj = find(bucket[j].first)
                        if (ri != rj) parent[ri] = rj
                    }
                }
            }
        }
        val groups = hashes.groupBy { find(it.first) }
            .values.filter { it.size > 1 }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            onProgress(items.size, items.size)
        }
        groups.map { g ->
            val exact = g.map { it.second }.toSet().size == 1
            DupGroup(
                g.map { it.first },
                if (exact) DupGroup.Kind.EXACT else DupGroup.Kind.SIMILAR
            )
        }.sortedByDescending { it.ids.size }
    }
}
