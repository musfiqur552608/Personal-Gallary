package com.freedu.personalgallary.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.freedu.personalgallary.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")

/** App settings: theme, lock, privacy, folder filters. 100% on-device. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MIN = intPreferencesKey("auto_lock_min")
        val HIDE_RECENTS = booleanPreferencesKey("hide_recents")
        val BLOCK_SCREENSHOT = booleanPreferencesKey("block_screenshot")
        val EXCLUDED = stringSetPreferencesKey("excluded_albums")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SHOW_INFO_ON_REELS = booleanPreferencesKey("reels_info")
    }

    data class AllSettings(
        val theme: ThemeMode = ThemeMode.SYSTEM,
        val appLock: Boolean = false,
        val biometric: Boolean = false,
        val autoLockMinutes: Int = 2,
        val hideFromRecents: Boolean = false,
        val blockScreenshots: Boolean = false,
        val excludedAlbums: Set<String> = emptySet(),
        val onboarded: Boolean = false,
        val showReelsInfo: Boolean = true
    )

    /** Single snapshot flow — avoids fragile many-flow combine overloads. */
    val all: Flow<AllSettings> = context.settingsStore.data.map { p ->
        AllSettings(
            theme = when (p[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            appLock = p[Keys.APP_LOCK] == true,
            biometric = p[Keys.BIOMETRIC] == true,
            autoLockMinutes = p[Keys.AUTO_LOCK_MIN] ?: 2,
            hideFromRecents = p[Keys.HIDE_RECENTS] == true,
            blockScreenshots = p[Keys.BLOCK_SCREENSHOT] == true,
            excludedAlbums = p[Keys.EXCLUDED] ?: emptySet(),
            onboarded = p[Keys.ONBOARDED] == true,
            showReelsInfo = p[Keys.SHOW_INFO_ON_REELS] != false
        )
    }

    val theme: Flow<ThemeMode> = all.map { it.theme }
    val appLockEnabled: Flow<Boolean> = all.map { it.appLock }
    val biometricEnabled: Flow<Boolean> = all.map { it.biometric }
    val autoLockMinutes: Flow<Int> = all.map { it.autoLockMinutes }
    val hideFromRecents: Flow<Boolean> = all.map { it.hideFromRecents }
    val blockScreenshots: Flow<Boolean> = all.map { it.blockScreenshots }
    val excludedAlbums: Flow<Set<String>> = all.map { it.excludedAlbums }
    val onboarded: Flow<Boolean> = all.map { it.onboarded }
    val showReelsInfo: Flow<Boolean> = all.map { it.showReelsInfo }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME] = mode.name }
    }
    suspend fun setAppLock(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.APP_LOCK] = enabled }
    }
    suspend fun setBiometric(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.BIOMETRIC] = enabled }
    }
    suspend fun setAutoLockMinutes(min: Int) {
        context.settingsStore.edit { it[Keys.AUTO_LOCK_MIN] = min }
    }
    suspend fun setHideFromRecents(hide: Boolean) {
        context.settingsStore.edit { it[Keys.HIDE_RECENTS] = hide }
    }
    suspend fun setBlockScreenshots(block: Boolean) {
        context.settingsStore.edit { it[Keys.BLOCK_SCREENSHOT] = block }
    }
    suspend fun setExcludedAlbums(set: Set<String>) {
        context.settingsStore.edit { it[Keys.EXCLUDED] = set }
    }
    suspend fun setOnboarded() {
        context.settingsStore.edit { it[Keys.ONBOARDED] = true }
    }
    suspend fun setShowReelsInfo(show: Boolean) {
        context.settingsStore.edit { it[Keys.SHOW_INFO_ON_REELS] = show }
    }
}
