package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb

@Composable
fun FavoritesScreen(items: List<MediaItem>, onOpen: (MediaItem) -> Unit) {
    if (items.isEmpty()) {
        EmptyState(
            "No favorites yet",
            "Double-tap a reel or tap the heart on any post — it stays on this device, just for you."
        )
        return
    }
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
    }
}
