package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.components.MonthHeader
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.FormatUtils

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
    videoCount: Int = 0
) {
    val groupByMonth = query.isBlank() && selectedAlbum == null &&
        activeFilter == GalleryFilter.ALL && isGrid
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
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = Color.White
    )

    Column(Modifier.fillMaxSize()) {
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                sections.forEach { (label, list) ->
                    if (label != null) {
                        // full-width month header (grid uses 3 fixed columns)
                        item(span = { GridItemSpan(3) }) {
                            MonthHeader(label = label, count = list.size)
                        }
                    }
                    items(list, key = { it.id }) { item ->
                        MediaThumb(
                            item,
                            Modifier
                                .aspectRatio(1f)
                                .animateItem(),
                            onClick = { onOpen(item) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size, key = { items[it].id }) { i ->
                    val item = items[i]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .animateItem()
                    ) {
                        MediaThumb(
                            item,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f),
                            onClick = { onOpen(item) }
                        )
                    }
                }
            }
        }
    }
}
