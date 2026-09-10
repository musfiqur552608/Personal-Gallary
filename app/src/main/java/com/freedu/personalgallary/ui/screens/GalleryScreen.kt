package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.SortOrder
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.components.MonthHeader
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class GalleryFilter { ALL, PHOTOS, VIDEOS, REELS, FAVORITES }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    activeFilter: GalleryFilter,
    onFilter: (GalleryFilter) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    isGrid: Boolean,
    onToggleView: () -> Unit,
    selectedAlbum: String?,
    onSelectAlbum: (String?) -> Unit,
    isFavorite: (Long) -> Boolean,
    onOpen: (MediaItem) -> Unit,
    onOpenAlbumDetail: (String) -> Unit,
    photoCount: Int = 0,
    videoCount: Int = 0,
    onShuffle: () -> Unit = {},
    columns: Int = 3,
    onColumnsChange: (Int) -> Unit = {},
    sort: SortOrder = SortOrder.NEWEST,
    onSortChange: (SortOrder) -> Unit = {},
    onBatchTrash: (List<MediaItem>) -> Unit = {},
    onBatchFavorite: (List<MediaItem>) -> Unit = {},
    onBatchShare: (List<MediaItem>) -> Unit = {},
    onBatchAlbum: (List<MediaItem>) -> Unit = {},
    onBatchVault: (List<MediaItem>) -> Unit = {}
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selecting = selectedIds.isNotEmpty()
    var sortMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val groupByMonth = query.isBlank() && selectedAlbum == null &&
        activeFilter == GalleryFilter.ALL && isGrid && sort == SortOrder.NEWEST
    val sections = remember(items, groupByMonth) {
        if (!groupByMonth) {
            listOf(null to items)
        } else {
            items.groupBy { FormatUtils.monthKey(it.dateTaken) }
                .toList()
                .sortedByDescending { it.first }
                .map { (_, v) -> FormatUtils.monthLabel(v.first().dateTaken) to v }
        }
    }
    val flatItems = remember(sections) { sections.flatMap { it.second } }
    // number of month-header rows before each flat position (for scrub mapping)
    val headerBefore: List<Int> = remember(sections) {
        val proper = ArrayList<Int>()
        var h = 0
        sections.forEach { (label, list) ->
            if (label != null) h++
            repeat(list.size) { proper.add(h) }
        }
        proper
    }
    val sectionForFlat: (Int) -> String = { pos ->
        var acc = 0
        var label = ""
        sections.forEach { (l, list) ->
            if (pos in acc until acc + list.size) label = l ?: ""
            acc += list.size
        }
        label
    }
    var trackH by remember { mutableFloatStateOf(1f) }
    var bubble by remember { mutableStateOf<Pair<String, Float>?>(null) }

    fun toggleSelect(item: MediaItem) {
        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
    }
    fun selectedItems(): List<MediaItem> {
        val byId = items.associateBy { it.id }
        return selectedIds.mapNotNull { byId[it] }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = Color.White
    )

    Column(Modifier.fillMaxSize()) {
        if (selecting) {
            // batch selection bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedIds = emptySet() }) {
                    Icon(Icons.Default.Close, "Close selection")
                }
                Text(
                    "${selectedIds.size} selected",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { selectedIds = items.map { it.id }.toSet() }) {
                    Icon(Icons.Default.Checklist, "Select all")
                }
            }
        }
        // gradient library banner
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(brandHorizontal)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                }
                Spacer(Modifier.padding(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Your library",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "$photoCount photos · $videoCount videos",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { onColumnsChange(if (columns >= 5) 2 else columns + 1) }) {
                    Icon(Icons.Default.GridView, "Columns: $columns", tint = Color.White)
                }
                IconButton(onClick = { sortMenu = true }) {
                    Icon(Icons.Default.Sort, "Sort", tint = Color.White)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortOrder.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { sortMenu = false; onSortChange(s) }
                        )
                    }
                }
                IconButton(onClick = onToggleView) {
                    Icon(
                        if (isGrid) Icons.Default.ViewAgenda else Icons.Default.GridView,
                        "Toggle view",
                        tint = Color.White
                    )
                }
            }
        }
        // search
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text("Search date, album, caption…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        // filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = activeFilter == GalleryFilter.ALL,
                    onClick = { onFilter(GalleryFilter.ALL) },
                    label = { Text("All") },
                    colors = chipColors
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == GalleryFilter.PHOTOS,
                    onClick = { onFilter(GalleryFilter.PHOTOS) },
                    label = { Text("Photos") },
                    colors = chipColors
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == GalleryFilter.VIDEOS,
                    onClick = { onFilter(GalleryFilter.VIDEOS) },
                    label = { Text("Videos") },
                    colors = chipColors
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == GalleryFilter.FAVORITES,
                    onClick = { onFilter(GalleryFilter.FAVORITES) },
                    label = { Text("Liked") },
                    colors = chipColors
                )
            }
        }
        // album chips
        if (albums.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedAlbum == null,
                        onClick = { onSelectAlbum(null) },
                        label = { Text("All albums") },
                        colors = chipColors
                    )
                }
                items(albums, key = { it.name }) { album ->
                    FilterChip(
                        selected = selectedAlbum == album.name,
                        onClick = {
                            if (selectedAlbum == album.name) onSelectAlbum(null)
                            else onOpenAlbumDetail(album.name)
                        },
                        label = { Text("${album.name} (${album.count})") },
                        colors = chipColors
                    )
                }
            }
        }
        if (items.isEmpty()) {
            EmptyState("Nothing found", "Try a different search, filter, or album.")
            return
        }
        if (isGrid) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { trackH = it.height.toFloat().coerceAtLeast(1f) }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    sections.forEach { (label, list) ->
                        if (label != null) {
                            item(span = { GridItemSpan(columns) }) {
                                MonthHeader(label = label, count = list.size)
                            }
                        }
                        items(list, key = { it.id }) { item ->
                            val sel = item.id in selectedIds
                            Box(Modifier.aspectRatio(1f).animateItem()) {
                                MediaThumb(
                                    item,
                                    Modifier.fillMaxSize(),
                                    onClick = {
                                        if (selecting) toggleSelect(item) else onOpen(item)
                                    },
                                    onLongClick = { toggleSelect(item) }
                                )
                                if (sel) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "✓",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(140.dp)) }
                }
                // timeline fast scrubber
                if (!selecting && flatItems.size > 60) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(26.dp)
                            .pointerInput(sections) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        val frac = (offset.y / trackH).coerceIn(0f, 1f)
                                        val pos = (frac * flatItems.size).toInt()
                                            .coerceIn(0, flatItems.size - 1)
                                        bubble = sectionForFlat(pos) to frac
                                    },
                                    onVerticalDrag = { change, _ ->
                                        val frac = (change.position.y / trackH).coerceIn(0f, 1f)
                                        val pos = (frac * flatItems.size).toInt()
                                            .coerceIn(0, flatItems.size - 1)
                                        bubble = sectionForFlat(pos) to frac
                                        val slot = pos + (headerBefore.getOrNull(pos) ?: 0)
                                        scope.launch {
                                            try {
                                                gridState.scrollToItem(slot)
                                            } catch (_: Exception) {
                                            }
                                        }
                                    },
                                    onDragEnd = { bubble = null },
                                    onDragCancel = { bubble = null }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .fillMaxHeight(0.5f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                    bubble?.let { (label, frac) ->
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .offset { IntOffset(-96, (frac * trackH).roundToInt() - 60) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.inverseSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label.ifBlank { "•" },
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // batch action bar
                if (selecting) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.inverseSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatchBtn(Icons.Default.Favorite, "Like") {
                            onBatchFavorite(selectedItems()); selectedIds = emptySet()
                        }
                        BatchBtn(Icons.Default.Share, "Share") {
                            onBatchShare(selectedItems())
                        }
                        BatchBtn(Icons.Default.Delete, "Trash") {
                            onBatchTrash(selectedItems()); selectedIds = emptySet()
                        }
                        BatchBtn(Icons.Default.CreateNewFolder, "Album") {
                            onBatchAlbum(selectedItems())
                        }
                        BatchBtn(Icons.Default.Lock, "Vault") {
                            onBatchVault(selectedItems()); selectedIds = emptySet()
                        }
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size, key = { items[it].id }) { i ->
                    val item = items[i]
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        MediaThumb(
                            item,
                            Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                            onClick = { onOpen(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, desc, tint = MaterialTheme.colorScheme.inverseOnSurface)
    }
}
