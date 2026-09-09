package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.model.Album
import com.freedu.personalgallary.data.model.StorageInsights
import com.freedu.personalgallary.data.model.ThemeMode
import com.freedu.personalgallary.ui.viewmodel.SettingsUiState
import com.freedu.personalgallary.util.FormatUtils

@Composable
fun ProfileScreen(
    settings: SettingsUiState,
    insights: StorageInsights,
    albums: List<Album>,
    onTheme: (ThemeMode) -> Unit,
    onAppLock: (Boolean) -> Unit,
    onBiometric: (Boolean) -> Unit,
    onSetupPin: () -> Unit,
    onClearPin: () -> Unit,
    onAutoLock: (Int) -> Unit,
    onHideRecents: (Boolean) -> Unit,
    onBlockScreenshots: (Boolean) -> Unit,
    onShowReelsInfo: (Boolean) -> Unit,
    onManageFolders: () -> Unit,
    onManageAlbums: () -> Unit,
    onExport: () -> Unit,
    onRefresh: () -> Unit
) {
    var showTheme by remember { mutableStateOf(false) }
    var showAutoLock by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Text("Profile & Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Private · offline · yours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        // storage insight card
        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Storage insights", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("${insights.photoCount}", "Photos")
                    Stat("${insights.videoCount}", "Videos")
                    Stat("${insights.reelCount}", "Reels")
                    Stat(FormatUtils.formatBytes(insights.totalBytes), "Used")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Re-scan library")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Section("Appearance") {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(settings.theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingContent = {
                    Icon(
                        when (settings.theme) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        }, null
                    )
                },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { showTheme = true }
            )
            ListItem(
                headlineContent = { Text("Show info overlay on Reels") },
                trailingContent = { Switch(settings.showReelsInfo, onShowReelsInfo) }
            )
        }

        Section("Privacy & lock") {
            ListItem(
                headlineContent = { Text("App lock") },
                supportingContent = { Text(if (settings.hasPin) "PIN set" else "No PIN yet") },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                trailingContent = { Switch(settings.appLock, onAppLock) }
            )
            ListItem(
                headlineContent = { Text(if (settings.hasPin) "Change PIN" else "Set PIN") },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { onSetupPin() }
            )
            if (settings.hasPin) {
                ListItem(
                    headlineContent = { Text("Remove PIN") },
                    modifier = Modifier.clickable { onClearPin() }
                )
            }
            ListItem(
                headlineContent = { Text("Biometric unlock") },
                trailingContent = { Switch(settings.biometric, onBiometric) }
            )
            ListItem(
                headlineContent = { Text("Auto-lock after") },
                supportingContent = { Text("${settings.autoLockMinutes} min") },
                modifier = Modifier.clickable { showAutoLock = true }
            )
            ListItem(
                headlineContent = { Text("Hide from recent apps") },
                supportingContent = { Text("Privacy screen when backgrounded") },
                trailingContent = { Switch(settings.hideFromRecents, onHideRecents) }
            )
            ListItem(
                headlineContent = { Text("Block screenshots in locked views") },
                trailingContent = { Switch(settings.blockScreenshots, onBlockScreenshots) }
            )
        }

        Section("Library") {
            ListItem(
                headlineContent = { Text("Include / exclude folders") },
                supportingContent = { Text("${albums.size} albums · ${settings.excludedAlbums.size} excluded") },
                leadingContent = { Icon(Icons.Default.Folder, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { onManageFolders() }
            )
            ListItem(
                headlineContent = { Text("Custom albums & vault") },
                leadingContent = { Icon(Icons.Default.Photo, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { onManageAlbums() }
            )
            ListItem(
                headlineContent = { Text("Export / backup (local zip)") },
                supportingContent = { Text("Favorites + settings metadata, stays on device") },
                modifier = Modifier.clickable { onExport() }
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Personal Gallary v1.0 · no account · no cloud · no tracking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(80.dp))
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onTheme(mode); showTheme = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(mode == settings.theme, onClick = { onTheme(mode); showTheme = false })
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showTheme = false }) { Text("Close") } }
        )
    }
    if (showAutoLock) {
        AlertDialog(
            onDismissRequest = { showAutoLock = false },
            title = { Text("Auto-lock after") },
            text = {
                Column {
                    listOf(1, 2, 5, 10, 30).forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onAutoLock(m); showAutoLock = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(m == settings.autoLockMinutes, onClick = { onAutoLock(m); showAutoLock = false })
                            Text("$m min")
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showAutoLock = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
    Card(shape = RoundedCornerShape(16.dp)) { Column { content() } }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
