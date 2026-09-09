package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.EmptyState
import com.freedu.personalgallary.ui.components.MediaThumb

/** Hashtag browser built from on-device captions (#tag parsing, no network). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    tags: List<Pair<String, Int>>,
    itemsForTag: (String) -> List<MediaItem>,
    onOpen: (MediaItem) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Tags",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (tags.isEmpty()) {
            EmptyState(
                "No tags yet",
                "Add captions like \"Beach day #vacation\" on any post — tags appear here automatically."
            )
            return
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags, key = { it.first }) { (tag, count) ->
                FilterChip(
                    selected = selected == tag,
                    onClick = { selected = if (selected == tag) null else tag },
                    label = { Text("#$tag ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        val list = selected?.let(itemsForTag) ?: emptyList()
        if (selected == null) {
            EmptyState("Pick a tag", "Tap a hashtag above to browse matching photos.")
            return
        }
        if (list.isEmpty()) {
            EmptyState("Nothing tagged", "No visible photos carry #$selected.")
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(list, key = { it.id }) { item ->
                MediaThumb(item, Modifier.aspectRatio(1f), onClick = { onOpen(item) })
            }
        }
    }
}
