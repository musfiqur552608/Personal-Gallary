package com.freedu.personalgallary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.personalgallary.data.local.AlbumItemCrossRef
import com.freedu.personalgallary.data.local.AppDatabase
import com.freedu.personalgallary.data.local.CaptionEntity
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.local.FavoriteEntity
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.FeedKind
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import com.freedu.personalgallary.data.model.StorageInsights
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val scannedCount: Int = 0,
    val allItems: List<MediaItem> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val captions: Map<Long, String> = emptyMap(),
    val hasPermission: Boolean = false
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val mediaRepo = MediaStoreRepository(app)
    val settings = SettingsRepository(app)

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    val customAlbums: StateFlow<List<CustomAlbumEntity>> =
        db.albums().observeAlbums().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val favoriteEntities = db.favorites().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val captionEntities = db.captions().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(favoriteEntities, captionEntities) { favs, caps ->
                favs.map { it.mediaStoreId }.toSet() to caps.associate { it.mediaStoreId to it.caption }
            }.collect { (favIds, caps) ->
                _state.value = _state.value.copy(favorites = favIds, captions = caps)
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
        }
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

    fun stories(items: List<MediaItem> = _state.value.allItems): List<MediaItem> =
        items.take(20)

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

    fun captionFor(id: Long): String? = _state.value.captions[id]
    fun isFavorite(id: Long): Boolean = id in _state.value.favorites

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

    fun deleteMedia(item: MediaItem, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val rows = mediaRepo.delete(item)
            if (rows > 0) {
                db.favorites().remove(item.id)
                _state.value = _state.value.copy(
                    allItems = _state.value.allItems.filterNot { it.id == item.id }
                )
                onDone(true)
            } else onDone(false)
        }
    }

    fun search(query: String, items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
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
