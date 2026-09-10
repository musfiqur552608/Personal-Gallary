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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Photo journal: one note per day beside that day's photos. */
@Composable
fun JournalScreen(
    items: List<MediaItem>,
    noteFor: (String) -> String?,
    onSave: (String, String) -> Unit,
    onOpen: (MediaItem, List<MediaItem>) -> Unit
) {
    var dayOffset by remember { mutableLongStateOf(0L) }
    val dayTs = remember(dayOffset) {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.DAY_OF_YEAR, -dayOffset.toInt())
        }
        c.timeInMillis
    }
    val dayKey = remember(dayTs) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dayTs))
    }
    val dayItems = remember(items, dayTs) {
        items.filter { FormatUtils.isSameDay(it.dateTaken, dayTs) }
    }
    var text by remember(dayKey) { mutableStateOf(noteFor(dayKey).orEmpty()) }
    var savedTick by remember { mutableStateOf(0) }
    LaunchedEffect(savedTick) { }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Journal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dayOffset++ }) { Icon(Icons.Default.ChevronLeft, "Previous day") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (dayOffset == 0L) "Today" else FormatUtils.formatShortDate(dayTs),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${dayItems.size} photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { if (dayOffset > 0) dayOffset-- }) { Icon(Icons.Default.ChevronRight, "Next day") }
            TextButton(onClick = { dayOffset = 0 }) { Text("Today") }
        }
        Spacer(Modifier.height(8.dp))
        if (dayItems.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((((dayItems.size + 3) / 4).coerceAtMost(2) * 96).dp),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                userScrollEnabled = false
            ) {
                items(dayItems, key = { it.id }) { item ->
                    MediaThumb(item, Modifier.aspectRatio(1f), showBadge = false, onClick = { onOpen(item, dayItems) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("What happened this day?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    onSave(dayKey, text)
                    savedTick++
                },
                enabled = text.isNotBlank()
            ) { Text(if (savedTick > 0 && text == noteFor(dayKey)) "Saved ✓" else "Save note") }
        }
        Spacer(Modifier.height(80.dp))
    }
}
