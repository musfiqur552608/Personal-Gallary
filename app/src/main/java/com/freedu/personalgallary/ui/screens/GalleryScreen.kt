package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb

enum class GalleryFilter { ALL, PHOTOS, VIDEOS, REELS, FAVORITES }

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
    onOpenAlbumDetail: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // search
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search date, album, caption…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            },
            singleLine = true
        )
        // filters + view toggle
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = activeFilter == GalleryFilter.ALL, onClick = { onFilter(GalleryFilter.ALL) }, label = { Text("All") })
            FilterChip(selected = activeFilter == GalleryFilter.PHOTOS, onClick = { onFilter(GalleryFilter.PHOTOS) }, label = { Text("Photos") })
            FilterChip(selected = activeFilter == GalleryFilter.VIDEOS, onClick = { onFilter(GalleryFilter.VIDEOS) }, label = { Text("Videos") })
            FilterChip(selected = activeFilter == GalleryFilter.FAVORITES, onClick = { onFilter(GalleryFilter.FAVORITES) }, label = { Text("Liked") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleView) {
                Icon(if (isGrid) Icons.Default.ViewAgenda else Icons.Default.GridView, "Toggle view")
            }
        }
        // album chips
        if (albums.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedAlbum == null,
                        onClick = { onSelectAlbum(null) },
                        label = { Text("All albums") }
                    )
                }
                items(albums, key = { it.name }) { album ->
                    FilterChip(
                        selected = selectedAlbum == album.name,
                        onClick = {
                            if (selectedAlbum == album.name) onSelectAlbum(null)
                            else onOpenAlbumDetail(album.name)
                        },
                        label = { Text("${album.name} (${album.count})") }
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
                items(items, key = { it.id }) { item ->
                    MediaThumb(item, Modifier.aspectRatio(1f), onClick = { onOpen(item) })
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
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        MediaThumb(item, Modifier.fillMaxWidth().aspectRatio(4f / 3f), onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}
