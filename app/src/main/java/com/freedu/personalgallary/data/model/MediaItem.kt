package com.freedu.personalgallary.data.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

enum class FeedKind { REEL, POST }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A single photo/video referenced by MediaStore URI. Media bytes never copied. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val type: MediaType,
    val mimeType: String,
    val dateTaken: Long,
    val dateAdded: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val albumName: String = "",
    val relativePath: String = ""
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO

    /** Spec rule: video <= 60s -> Reel, > 60s -> Post, images -> Post */
    val feedKind: FeedKind
        get() = if (isVideo && durationMs in 1..60_000) FeedKind.REEL else FeedKind.POST

    val isReel: Boolean get() = feedKind == FeedKind.REEL
}

data class Album(
    val name: String,
    val count: Int,
    val cover: MediaItem?,
    val totalBytes: Long,
    val isCustom: Boolean = false,
    val isPinned: Boolean = false,
    val albumId: Long? = null
)

data class StorageInsights(
    val photoCount: Int,
    val videoCount: Int,
    val totalBytes: Long,
    val reelCount: Int,
    val postCount: Int
)
