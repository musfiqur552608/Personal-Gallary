package com.freedu.personalgallary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.personalgallary.data.model.ThemeMode
import com.freedu.personalgallary.data.prefs.LockRepository
import com.freedu.personalgallary.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val appLock: Boolean = false,
    val biometric: Boolean = false,
    val autoLockMinutes: Int = 2,
    val hideFromRecents: Boolean = false,
    val blockScreenshots: Boolean = false,
    val excludedAlbums: Set<String> = emptySet(),
    val onboarded: Boolean = false,
    val showReelsInfo: Boolean = true,
    val hasPin: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    val locks = LockRepository(app)

    /** Bumped whenever the encrypted PIN changes so hasPin re-reads. */
    private val pinTick = MutableStateFlow(0)

    val state = combine(repo.all, pinTick) { all, _ ->
        SettingsUiState(
            theme = all.theme,
            appLock = all.appLock,
            biometric = all.biometric,
            autoLockMinutes = all.autoLockMinutes,
            hideFromRecents = all.hideFromRecents,
            blockScreenshots = all.blockScreenshots,
            excludedAlbums = all.excludedAlbums,
            onboarded = all.onboarded,
            showReelsInfo = all.showReelsInfo,
            hasPin = locks.hasPin()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setTheme(m: ThemeMode) = viewModelScope.launch { repo.setTheme(m) }
    fun setAppLock(b: Boolean) = viewModelScope.launch { repo.setAppLock(b) }
    fun setBiometric(b: Boolean) = viewModelScope.launch { repo.setBiometric(b) }
    fun setAutoLock(m: Int) = viewModelScope.launch { repo.setAutoLockMinutes(m) }
    fun setHideRecents(b: Boolean) = viewModelScope.launch { repo.setHideFromRecents(b) }
    fun setBlockScreenshots(b: Boolean) = viewModelScope.launch { repo.setBlockScreenshots(b) }
    fun setExcluded(s: Set<String>) = viewModelScope.launch { repo.setExcludedAlbums(s) }
    fun setOnboarded() = viewModelScope.launch { repo.setOnboarded() }
    fun setShowReelsInfo(b: Boolean) = viewModelScope.launch { repo.setShowReelsInfo(b) }

    fun savePin(pin: String) {
        locks.setPin(pin)
        pinTick.value++
    }
    fun verifyPin(pin: String): Boolean = locks.verifyPin(pin)
    fun clearPin() {
        locks.clearPin()
        pinTick.value++
    }
}
