package com.freedu.personalgallary.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.freedu.personalgallary.data.local.AlbumItemCrossRef
import com.freedu.personalgallary.data.local.AppDatabase
import com.freedu.personalgallary.data.local.AttemptEntity
import com.freedu.personalgallary.data.local.CaptionEntity
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.local.FavoriteEntity
import com.freedu.personalgallary.data.local.LockedEntity
import com.freedu.personalgallary.data.local.TrashEntity
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.FeedKind
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import com.freedu.personalgallary.data.model.StorageInsights
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.ExifUtils
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.ImageEditUtils
import com.freedu.personalgallary.util.PlaceCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

data class GalleryUiState(
    val isLoading: Boolean = true,
    val scannedCount: Int = 0,
    val allItems: List<MediaItem> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val captions: Map<Long, String> = emptyMap(),
    val trashedIds: Set<Long> = emptySet(),
    val trashedAt: Map<Long, Long> = emptyMap(),
    val lockedIds: Set<Long> = emptySet(),
    val hasPermission: Boolean = false
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val mediaRepo = MediaStoreRepository(app)
    val settings = SettingsRepository(app)

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private val _accent = MutableStateFlow<Int?>(null)
    val accentColor: StateFlow<Int?> = _accent.asStateFlow()

    /** Transient holder for the slideshow / editor entry points. */
    var slideshowItems: List<MediaItem> = emptyList()
        private set
    var slideshowTitle: String = "Memories"
        private set

    fun setSlideshow(items: List<MediaItem>, title: String = "Memories") {
        slideshowItems = items
        slideshowTitle = title
    }

    val customAlbums: StateFlow<List<CustomAlbumEntity>> =
        db.albums().observeAlbums().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val favoriteEntities = db.favorites().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val captionEntities = db.captions().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val trashEntities = db.trash().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val lockedEntities = db.locked().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val attempts = db.attempts().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(
                favoriteEntities, captionEntities, trashEntities, lockedEntities
            ) { favs, caps, trash, locked ->
                Triple(
                    favs.map { it.mediaStoreId }.toSet() to
                        caps.associate { it.mediaStoreId to it.caption },
                    trash.associate { it.mediaStoreId to it.trashedAt },
                    locked.map { it.mediaStoreId }.toSet()
                )
            }.collect { (favCaps, trashMap, lockedIds) ->
                val (favIds, caps) = favCaps
                _state.value = _state.value.copy(
                    favorites = favIds,
                    captions = caps,
                    trashedIds = trashMap.keys,
                    trashedAt = trashMap,
                    lockedIds = lockedIds
                )
            }
        }
    }

    fun setHasPermission(granted: Boolean) {
        _state.value = _state.value.copy(hasPermission = granted)
        if (granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, scannedCount = 0)
            val excluded = try { settings.excludedAlbums.first() } catch (_: Exception) { emptySet() }
            val result = mediaRepo.scanAll(excluded) { done ->
                _state.value = _state.value.copy(scannedCount = done)
            }
            _state.value = _state.value.copy(isLoading = false, allItems = result.items)
            purgeExpiredTrash()
        }
    }

    /** Visible library: trashed + vault-locked items are hidden everywhere. */
    fun visibleItems(items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val t = _state.value.trashedIds
        val l = _state.value.lockedIds
        if (t.isEmpty() && l.isEmpty()) return items
        return items.filter { it.id !in t && it.id !in l }
    }

    fun trashedItems(): List<MediaItem> {
        val t = _state.value.trashedIds
        return _state.value.allItems.filter { it.id in t }
    }

    fun lockedItems(): List<MediaItem> {
        val l = _state.value.lockedIds
        return _state.value.allItems.filter { it.id in l }
    }

    // --- Derived selectors (pure, testable) ---

    fun reels(items: List<MediaItem> = _state.value.allItems): List<MediaItem> =
        items.filter { it.feedKind == FeedKind.REEL }

    fun posts(items: List<MediaItem> = _state.value.allItems): List<MediaItem> =
        items.filter { it.feedKind == FeedKind.POST }

    fun favorites(items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val fav = _state.value.favorites
        return items.filter { it.id in fav }
    }

    fun memories(items: List<MediaItem> = _state.value.allItems): List<MediaItem> =
        items.filter { FormatUtils.isOnThisDay(it.dateTaken) }.take(30)

    /** Same week (day-of-year ±3), previous years. */
    fun memoriesWeek(items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val now = Calendar.getInstance()
        return items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateTaken }
            c.get(Calendar.YEAR) != now.get(Calendar.YEAR) &&
                dayDiff(c, now) <= 3
        }.take(40)
    }

    /** Same month, previous years. */
    fun memoriesMonth(items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val now = Calendar.getInstance()
        return items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateTaken }
            c.get(Calendar.YEAR) != now.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }.take(60)
    }

    private fun dayDiff(a: Calendar, b: Calendar): Int {
        val ac = (a.clone() as Calendar).apply {
            set(Calendar.YEAR, 2000); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val bc = (b.clone() as Calendar).apply {
            set(Calendar.YEAR, 2000); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var d = ((ac.timeInMillis - bc.timeInMillis) / 86_400_000L).toInt()
        if (d < 0) d = -d
        return minOf(d, 365 - d)
    }

    fun stories(items: List<MediaItem> = _state.value.allItems): List<MediaItem> =
        items.take(20)

    fun randomItem(items: List<MediaItem> = _state.value.allItems): MediaItem? =
        if (items.isEmpty()) null else items[Random.nextInt(items.size)]

    fun deviceAlbums(items: List<MediaItem> = _state.value.allItems): List<Album> =
        items.groupBy { it.albumName.ifBlank { "Other" } }
            .map { (name, list) ->
                Album(
                    name = name,
                    count = list.size,
                    cover = list.firstOrNull(),
                    totalBytes = list.sumOf { it.sizeBytes }
                )
            }.sortedByDescending { it.count }

    fun insights(items: List<MediaItem> = _state.value.allItems): StorageInsights =
        StorageInsights(
            photoCount = items.count { it.type == MediaType.IMAGE },
            videoCount = items.count { it.type == MediaType.VIDEO },
            totalBytes = items.sumOf { it.sizeBytes },
            reelCount = items.count { it.feedKind == FeedKind.REEL },
            postCount = items.count { it.feedKind == FeedKind.POST }
        )

    fun biggestVideos(items: List<MediaItem> = _state.value.allItems, n: Int = 15): List<MediaItem> =
        items.filter { it.isVideo }.sortedByDescending { it.sizeBytes }.take(n)

    fun captionFor(id: Long): String? = _state.value.captions[id]
    fun isFavorite(id: Long): Boolean = id in _state.value.favorites
    fun isTrashed(id: Long): Boolean = id in _state.value.trashedIds
    fun isLocked(id: Long): Boolean = id in _state.value.lockedIds

    /** All #tags found in captions with usage counts. */
    fun allTags(): List<Pair<String, Int>> {
        val regex = Regex("#([\\p{L}\\p{N}_]+)")
        return _state.value.captions.values
            .flatMap { regex.findAll(it).map { m -> m.groupValues[1].lowercase() } }
            .groupingBy { it }.eachCount()
            .toList().sortedByDescending { it.second }
    }

    fun itemsWithTag(tag: String, items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val regex = Regex("#([\\p{L}\\p{N}_]+)")
        return items.filter {
            val cap = captionFor(it.id).orEmpty()
            regex.findAll(cap).any { m -> m.groupValues[1].equals(tag, ignoreCase = true) }
        }
    }

    // --- Mutations (all local metadata) ---

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            if (isFavorite(item.id)) db.favorites().remove(item.id)
            else db.favorites().add(
                FavoriteEntity(
                    mediaStoreId = item.id,
                    uriString = item.uri.toString(),
                    dateTaken = item.dateTaken
                )
            )
        }
    }

    fun setCaption(item: MediaItem, caption: String) {
        viewModelScope.launch {
            db.captions().upsert(CaptionEntity(item.id, caption))
        }
    }

    fun moveToTrash(item: MediaItem) {
        viewModelScope.launch {
            db.trash().add(
                TrashEntity(
                    mediaStoreId = item.id,
                    uriString = item.uri.toString(),
                    dateTaken = item.dateTaken
                )
            )
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch { db.trash().remove(id) }
    }

    fun deleteForever(item: MediaItem, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val rows = mediaRepo.delete(item)
            db.trash().remove(item.id)
            db.favorites().remove(item.id)
            if (rows > 0) {
                _state.value = _state.value.copy(
                    allItems = _state.value.allItems.filterNot { it.id == item.id }
                )
                onDone(true)
            } else onDone(false)
        }
    }

    fun emptyTrash(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val ids = _state.value.trashedIds.toList()
            var count = 0
            val byId = _state.value.allItems.associateBy { it.id }
            ids.forEach { id ->
                val item = byId[id]
                if (item != null) {
                    try {
                        if (mediaRepo.delete(item) > 0) {
                            _state.value = _state.value.copy(
                                allItems = _state.value.allItems.filterNot { it.id == id }
                            )
                        }
                    } catch (_: Exception) {
                    }
                    db.favorites().remove(id)
                    count++
                }
                db.trash().remove(id)
            }
            onDone(count)
        }
    }

    private suspend fun purgeExpiredTrash() {
        try {
            val cutoff = System.currentTimeMillis() - 30L * 86_400_000L
            val expired = db.trash().expired(cutoff)
            if (expired.isEmpty()) return
            val byId = _state.value.allItems.associateBy { it.id }
            expired.forEach { e ->
                byId[e.mediaStoreId]?.let {
                    try {
                        mediaRepo.delete(it)
                    } catch (_: Exception) {
                    }
                }
                db.trash().remove(e.mediaStoreId)
                db.favorites().remove(e.mediaStoreId)
            }
            _state.value = _state.value.copy(
                allItems = _state.value.allItems.filterNot { it.id in expired.map { e -> e.mediaStoreId }.toSet() }
            )
        } catch (_: Exception) {
        }
    }

    fun lockItem(item: MediaItem) {
        viewModelScope.launch {
            db.locked().add(
                LockedEntity(mediaStoreId = item.id, uriString = item.uri.toString())
            )
        }
    }

    fun unlockItem(id: Long) {
        viewModelScope.launch { db.locked().remove(id) }
    }

    fun logAttempt() {
        viewModelScope.launch { db.attempts().log(AttemptEntity()) }
    }

    fun clearAttempts() {
        viewModelScope.launch { db.attempts().clear() }
    }

    fun deleteMedia(item: MediaItem, onDone: (Boolean) -> Unit = {}) {
        // legacy path kept for compatibility: permanent delete
        deleteForever(item, onDone)
    }

    fun createAlbum(name: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = db.albums().upsertAlbum(CustomAlbumEntity(name = name))
            onDone(id)
        }
    }

    fun deleteAlbum(id: Long) {
        viewModelScope.launch {
            db.albums().clearAlbum(id)
            db.albums().deleteAlbum(id)
        }
    }

    fun addToAlbum(albumId: Long, item: MediaItem) {
        viewModelScope.launch {
            db.albums().addItem(
                AlbumItemCrossRef(
                    albumId = albumId,
                    mediaStoreId = item.id,
                    uriString = item.uri.toString()
                )
            )
        }
    }

    fun removeFromAlbum(albumId: Long, mediaId: Long) {
        viewModelScope.launch { db.albums().removeItem(albumId, mediaId) }
    }

    fun setAlbumPinned(id: Long, pinned: Boolean) {
        viewModelScope.launch { db.albums().setPinned(id, pinned) }
    }

    fun setAlbumLocked(id: Long, locked: Boolean) {
        viewModelScope.launch { db.albums().setLocked(id, locked) }
    }

    fun albumItems(albumId: Long) = db.albums().observeItems(albumId)

    // --- Dynamic accent from the latest photo ---

    fun loadAccent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val first = _state.value.allItems.firstOrNull { !it.isVideo } ?: return@launch
                val bmp: Bitmap = ImageEditUtils.decodeSampled(
                    getApplication(), first.uri, 192
                ) ?: return@launch
                val palette = Palette.from(bmp).generate()
                val color = palette.getDominantColor(0)
                bmp.recycle()
                if (color != 0) _accent.value = color
            } catch (_: Exception) {
            }
        }
    }

    // --- Places (EXIF GPS clusters, cached in memory) ---

    private var placesCache: List<PlaceCluster>? = null

    suspend fun loadPlaces(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<PlaceCluster> =
        withContext(Dispatchers.IO) {
            placesCache?.let { return@withContext it }
            val ctx = getApplication<Application>()
            val images = visibleItems(_state.value.allItems).filter { !it.isVideo }
            val coords = ArrayList<Triple<MediaItem, Double, Double>>()
            images.forEachIndexed { i, item ->
                ExifUtils.latLong(ctx, item.uri)?.let { (la, lo) ->
                    coords += Triple(item, la, lo)
                }
                if (i % 50 == 0) {
                    withContext(Dispatchers.Main) { onProgress(i, images.size) }
                }
            }
            withContext(Dispatchers.Main) { onProgress(images.size, images.size) }
            val clusters = coords.groupBy { (_, la, lo) ->
                "${(la * 10).roundToInt()}_${(lo * 10).roundToInt()}"
            }.map { (key, list) ->
                val sorted = list.sortedByDescending { it.first.dateTaken }
                val avgLa = list.map { it.second }.average()
                val avgLo = list.map { it.third }.average()
                PlaceCluster(
                    key = key,
                    label = "%.2f, %.2f".format(avgLa, avgLo),
                    count = list.size,
                    cover = sorted.firstOrNull()?.first,
                    newestTs = sorted.firstOrNull()?.first?.dateTaken ?: 0L,
                    itemIds = list.map { it.first.id }
                )
            }.sortedByDescending { it.count }
            placesCache = clusters
            clusters
        }

    fun clearPlacesCache() {
        placesCache = null
    }

    // --- Smart search ---

    private val monthNames = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )
    private val monthShort = listOf(
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec"
    )

    fun search(query: String, items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        val monthKeyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val yearFmt = SimpleDateFormat("yyyy", Locale.US)
        // month queries: "june" or "june 2023"
        val monthIdx = monthNames.indexOfFirst { q.startsWith(it) }
            .takeIf { it >= 0 }
            ?: monthShort.indexOfFirst { q.startsWith(it) }.takeIf { it >= 0 }
        if (monthIdx != null) {
            val monthName = monthNames[monthIdx]
            val rest = q.removePrefix(monthName).trim()
                .ifBlank { q.removePrefix(monthShort[monthIdx]).trim() }
            val year = rest.toIntOrNull()?.takeIf { it in 1990..2100 }
            return items.filter {
                val key = monthKeyFmt.format(java.util.Date(it.dateTaken))
                val mm = "%02d".format(monthIdx + 1)
                if (year != null) key == "$year-$mm" else key.endsWith("-$mm")
            }
        }
        if (q == "this year") {
            val y = yearFmt.format(java.util.Date(System.currentTimeMillis()))
            return items.filter { yearFmt.format(java.util.Date(it.dateTaken)) == y }
        }
        if (q == "screenshots" || q == "screenshot") {
            return items.filter {
                it.albumName.lowercase().contains("screenshot") ||
                    it.name.lowercase().contains("screenshot")
            }
        }
        if (q == "large" || q == "large videos" || q == "big") {
            return items.filter { it.sizeBytes > 15L * 1024 * 1024 }
        }
        return items.filter {
            it.name.lowercase().contains(q) ||
                it.albumName.lowercase().contains(q) ||
                (captionFor(it.id)?.lowercase()?.contains(q) == true) ||
                FormatUtils.formatShortDate(it.dateTaken).lowercase().contains(q) ||
                (q == "video" && it.isVideo) || (q == "photo" && !it.isVideo) ||
                (q == "reel" && it.isReel) || (q == "favorite" && isFavorite(it.id))
        }
    }
}
