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

    // Labs & tools
    const val TOOLS = "tools"
    const val SLIDESHOW = "slideshow"
    const val COLLAGE = "collage"
    const val FILTER = "filter/{mediaId}"
    const val MARKUP = "markup/{mediaId}"
    const val TRIM = "trim/{mediaId}"
    const val STATS = "stats"
    const val STORAGE = "storage"
    const val PLACES = "places"
    const val TAGS = "tags"
    const val TRASH = "trash"
    const val REVIEW = "review"
    const val VAULT = "vault"
    const val SMART = "smart/{key}"
    const val CAPSULES = "capsules"
    const val JOURNAL = "journal"
    const val FACES = "faces"
    const val OCR = "ocr"
    const val DUPLICATES = "duplicates"
    const val RULES = "rules"
    const val KIOSK = "kiosk"
    const val DATEFIX = "datefix/{mediaId}"
    const val COMPARE = "compare/{editedId}"

    fun detail(mediaId: Long) = "detail/$mediaId"
    fun filter(mediaId: Long) = "filter/$mediaId"
    fun markup(mediaId: Long) = "markup/$mediaId"
    fun trim(mediaId: Long) = "trim/$mediaId"
    fun datefix(mediaId: Long) = "datefix/$mediaId"
    fun compare(editedId: Long) = "compare/$editedId"
    fun smart(key: String) = "smart/$key"

    /** Album/folder names may contain spaces — encode for safe navigation. */
    fun albumDetail(name: String) = "album/${Uri.encode(name)}"

    fun decodeAlbumName(encoded: String?): String = encoded?.let(Uri::decode).orEmpty()
}
