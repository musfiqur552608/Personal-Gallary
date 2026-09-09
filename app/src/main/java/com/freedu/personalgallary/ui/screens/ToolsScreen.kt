package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.ui.theme.BrandOrange
import com.freedu.personalgallary.ui.theme.BrandPink
import com.freedu.personalgallary.ui.theme.BrandTeal
import com.freedu.personalgallary.ui.theme.BrandViolet

/** Labs & tools hub: everything beyond the five main tabs. */
@Composable
fun ToolsScreen(
    reminder: Boolean,
    onReminder: (Boolean) -> Unit,
    dynamicAccent: Boolean,
    onDynamicAccent: (Boolean) -> Unit,
    onSlideshow: () -> Unit,
    onReview: () -> Unit,
    onStats: () -> Unit,
    onCollage: () -> Unit,
    onPdfFavorites: () -> Unit,
    onTags: () -> Unit,
    onStorage: () -> Unit,
    onPlaces: () -> Unit,
    trashCount: Int,
    onTrash: () -> Unit,
    onVault: () -> Unit,
    decoySet: Boolean,
    onSetupDecoy: () -> Unit,
    onClearDecoy: () -> Unit,
    attemptsText: String,
    onClearAttempts: () -> Unit,
    decoyMode: Boolean
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Labs & tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Playful extras, all offline",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        ToolSection("Memories") {
            ToolRow(
                Icons.Default.PlayArrow,
                listOf(BrandPink, BrandOrange),
                "Memory slideshow",
                "Auto-playing Ken Burns show of your memories",
                onSlideshow
            )
            ToolRow(
                Icons.Default.CalendarMonth,
                listOf(BrandViolet, BrandPink),
                "Year in review",
                "Your year, months and favorite moments",
                onReview
            )
            ToolRow(
                Icons.Default.Slideshow,
                listOf(BrandTeal, BrandViolet),
                "Stats dashboard",
                "Streaks, heatmap, top albums",
                onStats
            )
            ToggleRow(
                Icons.Default.Notifications,
                listOf(BrandOrange, BrandPink),
                "Daily memory reminder",
                "A gentle offline nudge each morning",
                reminder, onReminder
            )
        }

        ToolSection("Create") {
            ToolRow(
                Icons.Default.Dashboard,
                listOf(BrandViolet, BrandTeal),
                "Collage maker",
                "2–9 photos → one square artwork",
                onCollage
            )
            ToolRow(
                Icons.Default.PictureAsPdf,
                listOf(BrandPink, BrandViolet),
                "Favorites PDF book",
                "Export liked photos as a local PDF",
                onPdfFavorites
            )
        }

        ToolSection("Organize") {
            ToolRow(
                Icons.Default.Tag,
                listOf(BrandTeal, BrandOrange),
                "Tags",
                "Browse #hashtags from your captions",
                onTags
            )
            ToolRow(
                Icons.Default.Storage,
                listOf(BrandOrange, BrandViolet),
                "Storage analyzer",
                "Where your gigabytes live",
                onStorage
            )
            ToolRow(
                Icons.Default.Place,
                listOf(BrandPink, BrandTeal),
                "Places",
                "Photo clusters from GPS tags",
                onPlaces
            )
            if (!decoyMode) {
                ToolRow(
                    Icons.Default.Delete,
                    listOf(BrandPink, BrandOrange),
                    "Trash",
                    if (trashCount == 0) "Empty · 30-day safety net" else "$trashCount items · 30-day safety net",
                    onTrash
                )
                ToolRow(
                    Icons.Default.Lock,
                    listOf(BrandViolet, BrandPink),
                    "Vault",
                    "Per-item locked media",
                    onVault
                )
            }
        }

        ToolSection("Look & security") {
            ToggleRow(
                Icons.Default.Palette,
                listOf(BrandTeal, BrandViolet),
                "Photo-matched accent",
                "Theme tint from your latest photo",
                dynamicAccent, onDynamicAccent
            )
            if (!decoyMode) {
                ToolRow(
                    Icons.Default.VisibilityOff,
                    listOf(BrandOrange, BrandPink),
                    "Decoy PIN",
                    if (decoySet) "Set · tap to change or remove" else "A second PIN that opens a clean library",
                    onSetupDecoy
                )
                if (decoySet) {
                    ToolRow(
                        Icons.Default.VisibilityOff,
                        listOf(BrandViolet, BrandTeal),
                        "Remove decoy PIN",
                        "Back to a single PIN",
                        onClearDecoy
                    )
                }
                ToolRow(
                    Icons.Default.History,
                    listOf(BrandPink, BrandTeal),
                    "Break-in attempts",
                    attemptsText,
                    onClearAttempts
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ToolSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Card(shape = RoundedCornerShape(18.dp)) {
        Column { content() }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    gradient: List<Color>,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    gradient: List<Color>,
    title: String,
    desc: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
