package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.SmartKeys
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    deviceAlbums: List<Album>,
    customAlbums: List<CustomAlbumEntity>,
    onOpenDeviceAlbum: (String) -> Unit,
    onOpenCustomAlbum: (CustomAlbumEntity) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onDeleteAlbum: (CustomAlbumEntity) -> Unit,
    onPinAlbum: (CustomAlbumEntity) -> Unit,
    onLockAlbum: (CustomAlbumEntity) -> Unit,
    onBack: () -> Unit,
    smartCounts: Map<String, Int> = emptyMap(),
    onOpenSmart: (String) -> Unit = {}
) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Albums") }, navigationIcon = {
                IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        },
        floatingActionButton = {
            FloatingActionButton({ showCreate = true }) { Icon(Icons.Default.Add, "New album") }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Smart albums", fontWeight = FontWeight.Bold)
                }
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val smarts = listOf(
                        Triple(SmartKeys.RECENT, "New this week", Icons.Default.FiberNew),
                        Triple(SmartKeys.MONTH, "This month", Icons.Default.CalendarMonth),
                        Triple(SmartKeys.SCREENSHOTS, "Screenshots", Icons.Default.PhoneAndroid),
                        Triple(SmartKeys.LONG_VIDEOS, "Long videos", Icons.Default.Videocam),
                        Triple(SmartKeys.REELS, "All reels", Icons.Default.Movie)
                    )
                    items(smarts) { (key, label, icon) ->
                        Card(
                            Modifier
                                .width(128.dp)
                                .clickable { onOpenSmart(key) }
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${smartCounts[key] ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (customAlbums.isNotEmpty()) {
                item { Text("My collections", fontWeight = FontWeight.Bold) }
                items(customAlbums, key = { it.albumId }) { album ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenCustomAlbum(album) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (album.isLocked) Icons.Default.Lock else Icons.Default.Folder, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(album.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    (if (album.isPinned) "Pinned · " else "") + (if (album.isLocked) "Locked vault" else "Collection"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton({ onPinAlbum(album) }) {
                                Icon(
                                    Icons.Default.PushPin, "Pin",
                                    tint = if (album.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton({ onLockAlbum(album) }) {
                                Icon(
                                    Icons.Default.Lock, "Lock",
                                    tint = if (album.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton({ onDeleteAlbum(album) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item { Text("On this device", fontWeight = FontWeight.Bold) }
            if (deviceAlbums.isEmpty()) {
                item { Text("No folders found yet.", style = MaterialTheme.typography.bodySmall) }
            }
            items(deviceAlbums, key = { it.name }) { album ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenDeviceAlbum(album.name) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        album.cover?.let { MediaThumb(it, Modifier.width(64.dp).height(64.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(album.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${album.count} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New collection") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("e.g. Family trip") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onCreateAlbum(name.trim()); name = ""; showCreate = false },
                    enabled = name.trim().length >= 2
                ) { Text("Create") }
            },
            dismissButton = { TextButton({ showCreate = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    title: String,
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onPlay: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (onPlay != null && items.isNotEmpty()) {
                        IconButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, "Play slideshow")
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (items.isEmpty()) {
            EmptyState("Album is empty", "Add photos or videos to this album from any post.", Modifier.padding(pad))
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MediaThumb(item, Modifier.aspectRatio(1f), onClick = { onOpen(item) })
            }
        }
    }
}
