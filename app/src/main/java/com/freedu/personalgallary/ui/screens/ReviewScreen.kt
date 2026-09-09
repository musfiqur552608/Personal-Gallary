package com.freedu.personalgallary.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.ui.components.MediaThumb
import com.freedu.personalgallary.ui.theme.brandHorizontal
import com.freedu.personalgallary.util.StatsUtils
import java.util.Calendar

/** Year in review: months, highlights, totals — all local. */
@Composable
fun YearInReviewScreen(
    items: List<MediaItem>,
    favItems: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onShare: (String) -> Unit
) {
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearItems = remember(items, year) {
        items.filter { StatsUtils.yearOf(it.dateTaken) == year }
    }
    val months = remember(yearItems, year) { StatsUtils.monthsOfYear(yearItems, year) }
    val maxMonth = remember(months) { months.maxOfOrNull { it.count } ?: 1 }
    val highlights = remember(favItems, year) {
        favItems.filter { StatsUtils.yearOf(it.dateTaken) == year }.take(9)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$year in review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${yearItems.size} memories this year",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                onShare(
                    "My $year in Personal Gallary: ${yearItems.size} memories, " +
                        "${highlights.size} favorites. 100% offline."
                )
            }) { androidx.compose.material3.Icon(Icons.Default.Share, "Share") }
        }
        Spacer(Modifier.height(12.dp))
        // month bars
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEach { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            "${m.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height((118 * (m.count.toFloat() / maxMonth)).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (m.count > 0) brandHorizontal
                                    else androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(m.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Favorite moments", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (highlights.isEmpty()) {
            Text(
                "Like some $year photos and they'll shine here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                userScrollEnabled = false
            ) {
                items(highlights.size, key = { highlights[it].id }) { i ->
                    MediaThumb(highlights[i], Modifier.aspectRatio(1f), onClick = { onOpen(highlights[i]) })
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
