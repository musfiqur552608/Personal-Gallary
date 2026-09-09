package com.freedu.personalgallary.ui.navigation

import android.net.Uri

object Routes {
    const val FEED = "feed"
    const val REELS = "reels"
    const val GALLERY = "gallery"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{mediaId}"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album/{albumName}"

    fun detail(mediaId: Long) = "detail/$mediaId"

    /** Album/folder names may contain spaces — encode for safe navigation. */
    fun albumDetail(name: String) = "album/${Uri.encode(name)}"

    fun decodeAlbumName(encoded: String?): String = encoded?.let(Uri::decode).orEmpty()
}
