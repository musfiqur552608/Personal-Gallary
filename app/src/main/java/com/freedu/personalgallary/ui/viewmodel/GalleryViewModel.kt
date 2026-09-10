package com.freedu.personalgallary.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.freedu.personalgallary.data.local.AlbumItemCrossRef
import com.freedu.personalgallary.data.local.AppDatabase
import com.freedu.personalgallary.data.local.AttemptEntity
import com.freedu.personalgallary.data.local.CaptionEntity
import com.freedu.personalgallary.data.local.CapsuleEntity
import com.freedu.personalgallary.data.local.CapsuleItemCrossRef
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.local.DateOverrideEntity
import com.freedu.personalgallary.data.local.EditLinkEntity
import com.freedu.personalgallary.data.local.FaceIndexEntity
import com.freedu.personalgallary.data.local.FavoriteEntity
import com.freedu.personalgallary.data.local.JournalEntity
import com.freedu.personalgallary.data.local.LockedEntity
import com.freedu.personalgallary.data.local.OcrEntity
import com.freedu.personalgallary.data.local.RuleEntity
import com.freedu.personalgallary.data.local.TrashEntity
import com.freedu.personalgallary.data.local.VoiceNoteEntity
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.FeedKind
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import com.freedu.personalgallary.data.model.RuleTypes
import com.freedu.personalgallary.data.model.SmartKeys
import com.freedu.personalgallary.data.model.StorageInsights
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.AudioNote
import com.freedu.personalgallary.util.ExifUtils
import com.freedu.personalgallary.util.FaceScan
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.HashUtils
import com.freedu.personalgallary.util.ImageEditUtils
import com.freedu.personalgallary.util.OcrScan
import com.freedu.personalgallary.util.PlaceCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

data class FaceStat(val count: Int, val smiling: Boolean)

data class GalleryUiState(
    val isLoading: Boolean = true,
    val scannedCount: Int = 0,
    val allItems: List<MediaItem> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val captions: Map<Long, String> = emptyMap(),
    val trashedIds: Set<Long> = emptySet(),
    val trashedAt: Map<Long, Long> = emptyMap(),
    val lockedIds: Set<Long> = emptySet(),
    val overrides: Map<Long, Long> = emptyMap(),
    val ocrTexts: Map<Long, String> = emptyMap(),
    val faceStats: Map<Long, FaceStat> = emptyMap(),
    val capsuleHidden: Set<Long> = emptySet(),
    val voiceNotes: Map<Long, VoiceNoteEntity> = emptyMap(),
    val editLinks: Map<Long, Long> = emptyMap(), // editedId -> origId
    val rules: Map<String, RuleEntity> = emptyMap(),
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

    val capsules = db.capsules().observeCapsules()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val capsuleRefs = db.capsules().observeItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val journalNotes: StateFlow<Map<String, String>> =
        db.journal().observeAll().map { list -> list.associate { it.dayKey to it.text } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _rulesToast = MutableStateFlow(0)
    val rulesToast: StateFlow<Int> = _rulesToast.asStateFlow()

    var recordingId by mutableStateOf<Long?>(null)
        private set
    var playingId by mutableStateOf<Long?>(null)
        private set
    private var recorder: MediaRecorder? = null
    private var recFile: File? = null
    private var player: MediaPlayer? = null

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
        // metadata side-tables (separate collectors to avoid giant combines)
        viewModelScope.launch {
            db.dateOverrides().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    overrides = list.associate { it.mediaStoreId to it.dateTaken }
                )
                resortWithDates()
            }
        }
        viewModelScope.launch {
            db.ocr().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    ocrTexts = list.associate { it.mediaStoreId to it.text }
                )
            }
        }
        viewModelScope.launch {
            db.faces().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    faceStats = list.associate { it.mediaStoreId to FaceStat(it.faceCount, it.smiling) }
                )
            }
        }
        viewModelScope.launch {
            db.voice().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    voiceNotes = list.associateBy { it.mediaStoreId }
                )
            }
        }
        viewModelScope.launch {
            db.editLinks().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    editLinks = list.associate { it.editedId to it.origId }
                )
            }
        }
        viewModelScope.launch {
            db.rules().observeAll().collect { list ->
                _state.value = _state.value.copy(
                    rules = list.associateBy { it.type }
                )
            }
        }
        viewModelScope.launch {
            combine(capsules, capsuleRefs) { caps, refs -> caps to refs }
                .collect { (caps, refs) ->
                    val now = System.currentTimeMillis()
                    val sealedIds = caps.filter { it.openAt > now }.map { it.capsuleId }.toSet()
                    _state.value = _state.value.copy(
                        capsuleHidden = refs.filter { it.capsuleId in sealedIds }
                            .map { it.mediaStoreId }.toSet()
                    )
                }
        }
    }

    private fun resortWithDates() {
        val items = _state.value.allItems
        if (items.isEmpty()) return
        _state.value = _state.value.copy(allItems = applyDates(items))
    }

    private fun applyDates(items: List<MediaItem>): List<MediaItem> {
        val ov = _state.value.overrides
        if (ov.isEmpty()) return items
        return items.map { if (it.id in ov) it.copy(dateTaken = ov.getValue(it.id)) else it }
            .sortedWith(compareByDescending<MediaItem> { it.dateTaken }.thenByDescending { it.dateAdded })
    }

    /** Effective display/sort date (user override wins over EXIF). */
    fun dateOf(item: MediaItem): Long = _state.value.overrides[item.id] ?: item.dateTaken

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
            _state.value = _state.value.copy(
                isLoading = false,
                allItems = applyDates(result.items)
            )
            purgeExpiredTrash()
            runRules()
        }
    }

    /** Visible library: trashed + vault-locked + sealed-capsule items are hidden. */
    fun visibleItems(items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val t = _state.value.trashedIds
        val l = _state.value.lockedIds
        val c = _state.value.capsuleHidden
        if (t.isEmpty() && l.isEmpty() && c.isEmpty()) return items
        return items.filter { it.id !in t && it.id !in l && it.id !in c }
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

    fun addFavorite(item: MediaItem) {
        viewModelScope.launch {
            if (!isFavorite(item.id)) {
                db.favorites().add(
                    FavoriteEntity(
                        mediaStoreId = item.id,
                        uriString = item.uri.toString(),
                        dateTaken = item.dateTaken
                    )
                )
            }
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

    // --- Date overrides (fix wrong camera dates, EXIF untouched) ---

    fun setDateOverride(id: Long, ts: Long) {
        viewModelScope.launch { db.dateOverrides().upsert(DateOverrideEntity(id, ts)) }
    }

    fun clearDateOverride(id: Long) {
        viewModelScope.launch { db.dateOverrides().remove(id) }
    }

    // --- Journal ---

    fun saveJournal(dayKey: String, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) return@launch
            db.journal().upsert(JournalEntity(dayKey, text.trim()))
        }
    }

    // --- Voice notes ---

    fun voiceFor(id: Long): VoiceNoteEntity? = _state.value.voiceNotes[id]

    fun startVoiceRecord(item: MediaItem): Boolean {
        stopPlayback()
        return try {
            val file = AudioNote.newFile(getApplication())
            val rec = AudioNote.start(getApplication(), file) ?: return false
            recorder = rec
            recFile = file
            recordingId = item.id
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stopVoiceRecord(item: MediaItem): Boolean {
        val rec = recorder
        val file = recFile
        recorder = null
        recFile = null
        recordingId = null
        if (rec == null || file == null) return false
        val ok = AudioNote.stop(rec, file)
        if (!ok) {
            try {
                file.delete()
            } catch (_: Exception) {
            }
            return false
        }
        viewModelScope.launch {
            val dur = try {
                val r = MediaMetadataRetriever()
                r.setDataSource(file.absolutePath)
                val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                r.release()
                ms
            } catch (_: Exception) {
                0L
            }
            db.voice().upsert(VoiceNoteEntity(item.id, file.absolutePath, dur))
        }
        return true
    }

    fun deleteVoice(id: Long) {
        viewModelScope.launch {
            _state.value.voiceNotes[id]?.let {
                try {
                    File(it.filePath).delete()
                } catch (_: Exception) {
                }
            }
            db.voice().remove(id)
        }
    }

    fun toggleVoicePlayback(note: VoiceNoteEntity): Boolean {
        if (playingId == note.mediaStoreId) {
            stopPlayback()
            return false
        }
        stopPlayback()
        val id = note.mediaStoreId
        player = AudioNote.play(note.filePath) {
            playingId = null
        }
        return if (player != null) {
            playingId = id
            true
        } else false
    }

    fun stopPlayback() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        playingId = null
    }

    // --- Edit links (before/after compare) ---

    fun origOf(editedId: Long): Long? = _state.value.editLinks[editedId]

    fun editedOf(origId: Long): Long? =
        _state.value.editLinks.entries.firstOrNull { it.value == origId }?.key

    fun linkEditByName(origId: Long, displayName: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val editedId = findImageIdByName(displayName)
            if (editedId == null || editedId == origId) {
                withContext(Dispatchers.Main) { onDone(false) }
                return@launch
            }
            db.editLinks().link(EditLinkEntity(editedId, origId))
            withContext(Dispatchers.Main) { onDone(true) }
        }
    }

    private fun findImageIdByName(name: String): Long? {
        return try {
            val ctx = getApplication<Application>()
            val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            ctx.contentResolver.query(
                uri, arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
                arrayOf(name),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    // --- OCR index ---

    sealed interface AiScanResult {
        data object Unavailable : AiScanResult
        data class Done(val count: Int) : AiScanResult
    }

    suspend fun scanOcr(
        items: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit
    ): AiScanResult = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val known = _state.value.ocrTexts.keys
        val targets = items.filter { !it.isVideo && it.id !in known }
        var count = 0
        var unavailable = false
        targets.forEachIndexed { i, item ->
            val res = OcrScan.read(ctx, item.uri)
            res.onSuccess { text ->
                if (text.isNotBlank()) {
                    db.ocr().upsert(OcrEntity(item.id, text))
                    count++
                } else {
                    db.ocr().upsert(OcrEntity(item.id, ""))
                }
            }.onFailure {
                unavailable = true
                return@forEachIndexed
            }
            if (unavailable) return@forEachIndexed
            if (i % 5 == 0) {
                val d = i
                withContext(Dispatchers.Main) { onProgress(d, targets.size) }
            }
        }
        withContext(Dispatchers.Main) { onProgress(targets.size, targets.size) }
        if (unavailable && count == 0) AiScanResult.Unavailable else AiScanResult.Done(count)
    }

    // --- Face index ---

    suspend fun scanFaces(
        items: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit
    ): AiScanResult = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val known = _state.value.faceStats.keys
        val targets = items.filter { !it.isVideo && it.id !in known }.take(3000)
        var count = 0
        var unavailable = false
        targets.forEachIndexed { i, item ->
            val res = FaceScan.analyze(ctx, item.uri)
            res.onSuccess { info ->
                db.faces().upsert(FaceIndexEntity(item.id, info.count, info.smiling))
                if (info.count > 0) count++
            }.onFailure {
                unavailable = true
                return@forEachIndexed
            }
            if (unavailable) return@forEachIndexed
            if (i % 5 == 0) {
                val d = i
                withContext(Dispatchers.Main) { onProgress(d, targets.size) }
            }
        }
        withContext(Dispatchers.Main) { onProgress(targets.size, targets.size) }
        if (unavailable && count == 0) AiScanResult.Unavailable else AiScanResult.Done(count)
    }

    fun facePhotos(
        items: List<MediaItem>,
        onlySmiles: Boolean = false,
        minFaces: Int = 1
    ): List<MediaItem> {
        val stats = _state.value.faceStats
        return items.filter {
            val s = stats[it.id] ?: return@filter false
            s.count >= minFaces && (!onlySmiles || s.smiling)
        }
    }

    // --- Time capsules ---

    fun createCapsule(name: String, openAt: Long, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = db.capsules().create(CapsuleEntity(name = name, openAt = openAt))
            onDone(id)
        }
    }

    fun deleteCapsule(id: Long) {
        viewModelScope.launch {
            db.capsules().clearItems(id)
            db.capsules().delete(id)
        }
    }

    fun addToCapsule(capsuleId: Long, item: MediaItem) {
        viewModelScope.launch {
            db.capsules().addItem(
                CapsuleItemCrossRef(capsuleId, item.id, item.uri.toString())
            )
        }
    }

    fun capsuleItems(capsuleId: Long, items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val ids = capsuleRefs.value.filter { it.capsuleId == capsuleId }.map { it.mediaStoreId }.toSet()
        return items.filter { it.id in ids }
    }

    fun isCapsuleOpen(openAt: Long): Boolean = openAt <= System.currentTimeMillis()

    // --- Auto-rules ---

    fun setRule(type: String, enabled: Boolean, days: Int = 90) {
        viewModelScope.launch {
            db.rules().upsert(RuleEntity(type, enabled, days))
        }
    }

    fun consumeRulesToast(): Int {
        val v = _rulesToast.value
        _rulesToast.value = 0
        return v
    }

    private suspend fun runRules() {
        try {
            val rules = _state.value.rules.values.filter { it.enabled }
            if (rules.isEmpty()) return
            var actions = 0
            val now = System.currentTimeMillis()
            val lastRun = try { settings.lastRulesRun.first() } catch (_: Exception) { 0L }
            rules.firstOrNull { it.type == RuleTypes.TRASH_SCREENSHOTS }?.let { rule ->
                val cutoff = now - rule.daysParam * 86_400_000L
                _state.value.allItems
                    .filter {
                        (it.albumName.lowercase().contains("screenshot") ||
                            it.name.lowercase().contains("screenshot")) &&
                            it.dateTaken < cutoff && it.id !in _state.value.trashedIds
                    }
                    .forEach {
                        db.trash().add(
                            TrashEntity(it.id, it.uri.toString(), it.dateTaken)
                        )
                        actions++
                    }
            }
            rules.firstOrNull { it.type == RuleTypes.LOCK_WHATSAPP }?.let {
                _state.value.allItems
                    .filter { m ->
                        m.isVideo && m.albumName.lowercase().contains("whatsapp") &&
                            m.dateAdded > lastRun && m.id !in _state.value.lockedIds
                    }
                    .forEach {
                        db.locked().add(LockedEntity(it.id, it.uri.toString()))
                        actions++
                    }
            }
            settings.setLastRulesRun(now)
            if (actions > 0) _rulesToast.value = actions
        } catch (_: Exception) {
        }
    }

    // --- Duplicates ---

    suspend fun scanDuplicates(
        items: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<List<MediaItem>> = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val images = items.filter { !it.isVideo }
        val groups = HashUtils.findDuplicates(ctx, images, onProgress)
        val byId = items.associateBy { it.id }
        val out = groups.mapNotNull { g ->
            val list = g.ids.mapNotNull { byId[it] }
            if (list.size > 1) list else null
        }.toMutableList()
        // exact video dupes by size+duration
        items.filter { it.isVideo }
            .groupBy { it.sizeBytes to it.durationMs }
            .values.filter { it.size > 1 }.forEach { out += it }
        out.sortedByDescending { it.size }
    }

    // --- Smart auto-albums ---

    fun smartItems(key: String, items: List<MediaItem> = _state.value.allItems): List<MediaItem> {
        val now = System.currentTimeMillis()
        return when (key) {
            SmartKeys.RECENT -> items.filter { it.dateTaken > now - 7L * 86_400_000L }
            SmartKeys.MONTH -> {
                val cal = Calendar.getInstance()
                items.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateTaken }
                    c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                        c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                }
            }
            SmartKeys.SCREENSHOTS -> items.filter {
                it.albumName.lowercase().contains("screenshot") ||
                    it.name.lowercase().contains("screenshot")
            }
            SmartKeys.LONG_VIDEOS -> items.filter { it.isVideo && it.durationMs > 60_000 }
            SmartKeys.REELS -> items.filter { it.isReel }
            else -> emptyList()
        }
    }

    // --- Encrypted backup payload (VM owns DB access) ---

    suspend fun exportJson(): JSONObject = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val favs = JSONArray()
        db.favorites().observeAll().first().forEach {
            favs.put(JSONObject().put("id", it.mediaStoreId).put("uri", it.uriString).put("taken", it.dateTaken))
        }
        root.put("favorites", favs)
        val caps = JSONArray()
        db.captions().observeAll().first().forEach {
            caps.put(JSONObject().put("id", it.mediaStoreId).put("text", it.caption))
        }
        root.put("captions", caps)
        val albums = JSONArray()
        db.albums().observeAlbums().first().forEach { a ->
            val ao = JSONObject().put("name", a.name).put("pinned", a.isPinned).put("locked", a.isLocked)
            val members = JSONArray()
            db.albums().observeItems(a.albumId).first().forEach {
                members.put(JSONObject().put("id", it.mediaStoreId).put("uri", it.uriString))
            }
            ao.put("items", members)
            albums.put(ao)
        }
        root.put("albums", albums)
        val journal = JSONArray()
        db.journal().observeAll().first().forEach {
            journal.put(JSONObject().put("day", it.dayKey).put("text", it.text))
        }
        root.put("journal", journal)
        val links = JSONArray()
        db.editLinks().observeAll().first().forEach {
            links.put(JSONObject().put("edited", it.editedId).put("orig", it.origId))
        }
        root.put("links", links)
        val dates = JSONArray()
        db.dateOverrides().observeAll().first().forEach {
            dates.put(JSONObject().put("id", it.mediaStoreId).put("taken", it.dateTaken))
        }
        root.put("dates", dates)
        val rules = JSONArray()
        db.rules().observeAll().first().forEach {
            rules.put(JSONObject().put("type", it.type).put("enabled", it.enabled).put("days", it.daysParam))
        }
        root.put("rules", rules)
        root
    }

    /** Returns restored record count, or -1 on bad payload. */
    suspend fun importJson(root: JSONObject): Int = withContext(Dispatchers.IO) {
        var n = 0
        try {
            root.optJSONArray("favorites")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.favorites().add(
                        FavoriteEntity(o.getLong("id"), o.optString("uri"), o.optLong("taken"))
                    )
                    n++
                }
            }
            root.optJSONArray("captions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.captions().upsert(CaptionEntity(o.getLong("id"), o.optString("text")))
                    n++
                }
            }
            root.optJSONArray("albums")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = db.albums().upsertAlbum(
                        CustomAlbumEntity(
                            name = o.optString("name"),
                            isPinned = o.optBoolean("pinned"),
                            isLocked = o.optBoolean("locked")
                        )
                    )
                    n++
                    o.optJSONArray("items")?.let { members ->
                        for (j in 0 until members.length()) {
                            val m = members.getJSONObject(j)
                            db.albums().addItem(
                                AlbumItemCrossRef(id, m.getLong("id"), m.optString("uri"))
                            )
                            n++
                        }
                    }
                }
            }
            root.optJSONArray("journal")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.journal().upsert(JournalEntity(o.optString("day"), o.optString("text")))
                    n++
                }
            }
            root.optJSONArray("links")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.editLinks().link(EditLinkEntity(o.getLong("edited"), o.getLong("orig")))
                    n++
                }
            }
            root.optJSONArray("dates")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.dateOverrides().upsert(DateOverrideEntity(o.getLong("id"), o.getLong("taken")))
                    n++
                }
            }
            root.optJSONArray("rules")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.rules().upsert(
                        RuleEntity(o.optString("type"), o.optBoolean("enabled"), o.optInt("days", 90))
                    )
                    n++
                }
            }
        } catch (_: Exception) {
            return@withContext -1
        }
        n
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
                (_state.value.ocrTexts[it.id]?.lowercase()?.contains(q) == true) ||
                FormatUtils.formatShortDate(it.dateTaken).lowercase().contains(q) ||
                (q == "video" && it.isVideo) || (q == "photo" && !it.isVideo) ||
                (q == "reel" && it.isReel) || (q == "favorite" && isFavorite(it.id))
        }
    }
}
