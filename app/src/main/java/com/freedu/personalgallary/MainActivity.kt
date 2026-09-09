package com.freedu.personalgallary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import com.freedu.personalgallary.ui.navigation.Routes
import com.freedu.personalgallary.ui.screens.AddToAlbumSheet
import com.freedu.personalgallary.ui.screens.AlbumDetailScreen
import com.freedu.personalgallary.ui.screens.AlbumsScreen
import com.freedu.personalgallary.ui.screens.DetailScreen
import com.freedu.personalgallary.ui.screens.FavoritesScreen
import com.freedu.personalgallary.ui.screens.FeedScreen
import com.freedu.personalgallary.ui.screens.FolderFilterDialog
import com.freedu.personalgallary.ui.screens.GalleryFilter
import com.freedu.personalgallary.ui.screens.GalleryScreen
import com.freedu.personalgallary.ui.screens.LockScreen
import com.freedu.personalgallary.ui.screens.MediaDetailsDialog
import com.freedu.personalgallary.ui.screens.OnboardingScreen
import com.freedu.personalgallary.ui.screens.ProfileScreen
import com.freedu.personalgallary.ui.screens.ReelsScreen
import com.freedu.personalgallary.ui.theme.PersonalGallaryTheme
import com.freedu.personalgallary.ui.viewmodel.GalleryViewModel
import com.freedu.personalgallary.ui.viewmodel.SettingsViewModel
import com.freedu.personalgallary.util.ShareUtils
import com.freedu.personalgallary.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : FragmentActivity() {

    private var lastActiveMs = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settings by settingsVm.state.collectAsState()
            PersonalGallaryTheme(themeMode = settings.theme) {
                // privacy screen flags
                LaunchedEffect(settings.hideFromRecents, settings.blockScreenshots) {
                    applySecureFlags(settings.hideFromRecents || settings.blockScreenshots)
                }
                AppRoot(
                    settingsVm = settingsVm,
                    onUserActive = { lastActiveMs = System.currentTimeMillis() },
                    lastActiveMs = { lastActiveMs }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastActiveMs = System.currentTimeMillis()
    }

    private fun applySecureFlags(secure: Boolean) {
        if (secure) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.FEED, "Home", Icons.Default.Home),
    Tab(Routes.REELS, "Reels", Icons.Default.Movie),
    Tab(Routes.GALLERY, "Gallery", Icons.Default.PhotoLibrary),
    Tab(Routes.FAVORITES, "Liked", Icons.Default.Favorite),
    Tab(Routes.PROFILE, "Settings", Icons.Default.Settings)
)

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
private fun AppRoot(
    settingsVm: SettingsViewModel,
    onUserActive: () -> Unit,
    lastActiveMs: () -> Long
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val galleryVm: GalleryViewModel = viewModel()
    val state by galleryVm.state.collectAsState()
    val settings by settingsVm.state.collectAsState()
    val customAlbums by galleryVm.customAlbums.collectAsState()

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var unlocked by remember { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var setupPinMode by remember { mutableStateOf(false) }
    var pendingVaultAlbum by remember { mutableStateOf<CustomAlbumEntity?>(null) }
    var vaultUnlockedIds by remember { mutableStateOf(setOf<Long>()) }

    // gallery UI state
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(GalleryFilter.ALL) }
    var isGrid by remember { mutableStateOf(true) }
    var deviceAlbumFilter by remember { mutableStateOf<String?>(null) }

    // dialogs / sheets
    var captionTarget by remember { mutableStateOf<MediaItem?>(null) }
    var captionText by remember { mutableStateOf("") }
    var albumTarget by remember { mutableStateOf<MediaItem?>(null) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var pendingDelete by remember { mutableStateOf<MediaItem?>(null) }
    var showFolders by remember { mutableStateOf(false) }
    var showAlbums by remember { mutableStateOf(false) }
    var customAlbumDetail by remember { mutableStateOf<CustomAlbumEntity?>(null) }
    var showMemories by remember { mutableStateOf(false) }

    val needsLock = settings.appLock && settingsVm.locks.hasPin() && !unlocked

    // Biometric availability (FragmentActivity host, so BiometricPrompt is safe)

    // auto-lock on resume after inactivity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val idleMin = (System.currentTimeMillis() - lastActiveMs()) / 60000
                if (settings.appLock && settingsVm.locks.hasPin() && unlocked && idleMin >= settings.autoLockMinutes) {
                    unlocked = false
                }
                val granted = requiredPermissions().any {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                galleryVm.setHasPermission(granted)
                if (granted) galleryVm.refresh()
                onUserActive()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                onUserActive()
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants.values.any { it }
        galleryVm.setHasPermission(ok)
        if (ok) settingsVm.setOnboarded()
    }
    LaunchedEffect(Unit) {
        val granted = requiredPermissions().any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        galleryVm.setHasPermission(granted)
    }

    fun shareItem(item: MediaItem) {
        onUserActive()
        ShareUtils.share(context, listOf(item))
    }

    fun doExport() {
        onUserActive()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favs = galleryVm.favorites()
                val dir = File(context.cacheDir, "export").apply { mkdirs() }
                val meta = JSONObject().apply {
                    put("exportedAt", System.currentTimeMillis())
                    put("favorites", JSONArray(favs.map { it.uri.toString() }))
                }
                File(dir, "metadata.json").writeText(meta.toString(2))
                val zip = File(context.getExternalFilesDir(null), "personal-gallery-backup.zip")
                ZipOutputStream(FileOutputStream(zip)).use { z ->
                    dir.listFiles()?.forEach { f ->
                        z.putNextEntry(ZipEntry(f.name))
                        f.inputStream().use { it.copyTo(z) }
                        z.closeEntry()
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Backup saved: ${zip.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun authenticateBiometric(onOk: () -> Unit, onFail: (String) -> Unit) {
        val mgr = androidx.biometric.BiometricManager.from(context)
        val can = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            onFail("Biometrics not available")
            return
        }
        val exec = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            exec,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onOk()
                }

                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    onFail(msg.toString())
                }

                override fun onAuthenticationFailed() {
                    onFail("Not recognized, try again")
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Personal Gallary")
                .setSubtitle("Private gallery — verify it's you")
                .setNegativeButtonText("Use PIN")
                .build()
        )
    }

    // ---- gates ----
    if (!settings.onboarded && !state.hasPermission) {
        OnboardingScreen(onGrant = { launcher.launch(requiredPermissions()) })
        return
    }
    if (needsLock && !setupPinMode) {
        val bioOk =
            androidx.biometric.BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS && settings.biometric
        LockScreen(
            hasPin = true,
            biometricAvailable = bioOk,
            error = lockError,
            onPin = { pin ->
                if (settingsVm.verifyPin(pin)) {
                    unlocked = true
                    lockError = null
                    onUserActive()
                } else lockError = "Wrong PIN — try again"
            },
            onBiometric = {
                authenticateBiometric(
                    onOk = { unlocked = true; lockError = null; onUserActive() },
                    onFail = { lockError = it }
                )
            }
        )
        return
    }
    if (setupPinMode || pendingVaultAlbum != null) {
        LockScreen(
            hasPin = settingsVm.locks.hasPin(),
            biometricAvailable = false,
            setupMode = setupPinMode,
            error = lockError,
            onPin = { combined ->
                if (setupPinMode) {
                    val parts = combined.split("|")
                    if (parts.size == 2 && parts[0] == parts[1] && parts[0].length >= 4) {
                        settingsVm.savePin(parts[0])
                        settingsVm.setAppLock(true)
                        setupPinMode = false
                        unlocked = true
                        lockError = null
                    } else lockError = "PINs don't match (min 4 digits)"
                } else {
                    // vault unlock
                    if (settingsVm.verifyPin(combined)) {
                        pendingVaultAlbum?.let { vaultUnlockedIds = vaultUnlockedIds + it.albumId }
                        pendingVaultAlbum = null
                        lockError = null
                        onUserActive()
                    } else lockError = "Wrong PIN"
                }
            },
            onBiometric = {},
            onCancel = {
                setupPinMode = false
                pendingVaultAlbum = null
                lockError = null
            }
        )
        return
    }

    // ---- derived lists ----
    val allItems = state.allItems
    val reels = remember(allItems) { galleryVm.reels(allItems) }
    val posts = remember(allItems) { galleryVm.posts(allItems) }
    val favItems = remember(allItems, state.favorites) { galleryVm.favorites(allItems) }
    val memories = remember(allItems) { galleryVm.memories(allItems) }
    val stories = remember(allItems) { galleryVm.stories(allItems) }
    val deviceAlbums = remember(allItems) { galleryVm.deviceAlbums(allItems) }
    val insights = remember(allItems) { galleryVm.insights(allItems) }

    val galleryBase: List<MediaItem> = remember(allItems, filter, deviceAlbumFilter, query, state.favorites) {
        var list = when (filter) {
            GalleryFilter.ALL -> allItems
            GalleryFilter.PHOTOS -> allItems.filter { it.type == MediaType.IMAGE }
            GalleryFilter.VIDEOS -> allItems.filter { it.type == MediaType.VIDEO }
            GalleryFilter.REELS -> allItems.filter { it.isReel }
            GalleryFilter.FAVORITES -> allItems.filter { it.id in state.favorites }
        }
        if (deviceAlbumFilter != null) list = list.filter { it.albumName == deviceAlbumFilter }
        if (query.isNotBlank()) list = galleryVm.search(query, list)
        list
    }

    fun openDetail(item: MediaItem) {
        onUserActive()
        nav.navigate(Routes.detail(item.id)) {
            launchSingleTop = true
        }
    }

    // Main scaffold with bottom nav
    Scaffold(
        bottomBar = {
            // hide bottom bar on fullscreen detail? keep visible except reels for immersion
            if (currentRoute != Routes.REELS) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                onUserActive()
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = Routes.FEED) {
                composable(Routes.FEED) {
                    FeedScreen(
                        isLoading = state.isLoading,
                        scannedCount = state.scannedCount,
                        posts = if (query.isBlank()) posts else galleryVm.search(query, posts),
                        memories = memories,
                        stories = stories,
                        isFavorite = { galleryVm.isFavorite(it) },
                        captionFor = { galleryVm.captionFor(it) },
                        onOpen = ::openDetail,
                        onToggleFavorite = { galleryVm.toggleFavorite(it); onUserActive() },
                        onShare = ::shareItem,
                        onDelete = { pendingDelete = it },
                        onEditCaption = {
                            captionTarget = it
                            captionText = galleryVm.captionFor(it.id).orEmpty()
                        },
                        onAddToAlbum = { albumTarget = it },
                        onOpenMemories = { showMemories = true }
                    )
                }
                composable(Routes.REELS) {
                    ReelsScreen(
                        reels = reels,
                        isFavorite = { galleryVm.isFavorite(it) },
                        showInfo = settings.showReelsInfo,
                        onToggleFavorite = { galleryVm.toggleFavorite(it); onUserActive() },
                        onShare = ::shareItem,
                        onDelete = { pendingDelete = it },
                        onAddToAlbum = { albumTarget = it },
                        onDetails = { detailsTarget = it },
                        onWallpaper = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val ok = WallpaperHelper.setAsWallpaper(context, it)
                                Toast.makeText(
                                    context,
                                    if (ok) "Wallpaper set" else "Couldn't set wallpaper",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
                composable(Routes.GALLERY) {
                    GalleryScreen(
                        items = galleryBase,
                        albums = deviceAlbums,
                        activeFilter = filter,
                        onFilter = { filter = it },
                        query = query,
                        onQuery = { query = it },
                        isGrid = isGrid,
                        onToggleView = { isGrid = !isGrid },
                        selectedAlbum = deviceAlbumFilter,
                        onSelectAlbum = { deviceAlbumFilter = it },
                        isFavorite = { galleryVm.isFavorite(it) },
                        onOpen = ::openDetail,
                        onOpenAlbumDetail = { name -> nav.navigate(Routes.albumDetail(name)) }
                    )
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(favItems, ::openDetail)
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        settings = settings,
                        insights = insights,
                        albums = deviceAlbums,
                        onTheme = { settingsVm.setTheme(it) },
                        onAppLock = {
                            if (it && !settingsVm.locks.hasPin()) setupPinMode = true
                            else settingsVm.setAppLock(it)
                        },
                        onBiometric = { settingsVm.setBiometric(it) },
                        onSetupPin = { setupPinMode = true; lockError = null },
                        onClearPin = {
                            settingsVm.clearPin()
                            settingsVm.setAppLock(false)
                            unlocked = false
                        },
                        onAutoLock = { settingsVm.setAutoLock(it) },
                        onHideRecents = { settingsVm.setHideRecents(it) },
                        onBlockScreenshots = { settingsVm.setBlockScreenshots(it) },
                        onShowReelsInfo = { settingsVm.setShowReelsInfo(it) },
                        onManageFolders = { showFolders = true },
                        onManageAlbums = { showAlbums = true },
                        onExport = ::doExport,
                        onRefresh = { galleryVm.refresh() }
                    )
                }
                composable(Routes.DETAIL) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    // context list: prefer current tab's visible list
                    val ctxList = when (currentRoute) {
                        Routes.FAVORITES -> favItems
                        Routes.GALLERY -> galleryBase
                        else -> allItems
                    }.ifEmpty { allItems }
                    val safeCtxIdx = if (ctxList.isEmpty()) 0 else
                        ctxList.indexOfFirst { it.id == id }.let { if (it < 0) 0 else it }
                    DetailScreen(
                        items = ctxList,
                        startIndex = if (ctxList.isEmpty()) 0 else safeCtxIdx.coerceIn(0, ctxList.lastIndex),
                        isFavorite = { galleryVm.isFavorite(it) },
                        captionFor = { galleryVm.captionFor(it) },
                        onToggleFavorite = { galleryVm.toggleFavorite(it) },
                        onShare = ::shareItem,
                        onDelete = { pendingDelete = it },
                        onSaveCaption = { item, cap -> galleryVm.setCaption(item, cap) },
                        onAddToAlbum = { albumTarget = it },
                        onDetails = { detailsTarget = it },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Routes.ALBUM_DETAIL) { entry ->
                    val name = Routes.decodeAlbumName(entry.arguments?.getString("albumName"))
                    val items = remember(allItems, name) {
                        allItems.filter { it.albumName == name }
                    }
                    AlbumDetailScreen(name, items, ::openDetail) { nav.popBackStack() }
                }
            }
        }
    }

    // ---- overlays ----
    captionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { captionTarget = null },
            title = { Text("Caption (on-device only)") },
            text = {
                OutlinedTextField(
                    value = captionText, onValueChange = { captionText = it },
                    placeholder = { Text("Add a memory note…") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton({
                    galleryVm.setCaption(target, captionText.trim())
                    captionTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ captionTarget = null }) { Text("Cancel") } }
        )
    }
    albumTarget?.let {
        AddToAlbumSheet(
            customAlbums = customAlbums,
            onCreate = { name -> galleryVm.createAlbum(name) },
            onAdd = { albumId -> albumTarget?.let { m -> galleryVm.addToAlbum(albumId, m) } },
            onDismiss = { albumTarget = null }
        )
    }
    detailsTarget?.let {
        MediaDetailsDialog(it, galleryVm.captionFor(it.id)) { detailsTarget = null }
    }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this file?") },
            text = { Text("${target.name}\nThis removes it from device storage.") },
            confirmButton = {
                TextButton({
                    galleryVm.deleteMedia(target) { ok ->
                        Toast.makeText(
                            context,
                            if (ok) "Deleted" else "Couldn't delete",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("Keep") } }
        )
    }
    if (showFolders) {
        FolderFilterDialog(
            albums = deviceAlbums,
            excluded = settings.excludedAlbums,
            onToggle = { name ->
                val next = settings.excludedAlbums.toMutableSet().apply {
                    if (contains(name)) remove(name) else add(name)
                }
                settingsVm.setExcluded(next)
                galleryVm.refresh()
            },
            onDismiss = { showFolders = false }
        )
    }
    if (showAlbums) {
        AlbumsScreen(
            deviceAlbums = deviceAlbums,
            customAlbums = customAlbums,
            onOpenDeviceAlbum = { name ->
                showAlbums = false
                nav.navigate(Routes.albumDetail(name))
            },
            onOpenCustomAlbum = { album ->
                if (album.isLocked && album.albumId !in vaultUnlockedIds) {
                    pendingVaultAlbum = album
                } else {
                    customAlbumDetail = album
                }
            },
            onCreateAlbum = { galleryVm.createAlbum(it) },
            onDeleteAlbum = { galleryVm.deleteAlbum(it.albumId) },
            onPinAlbum = { galleryVm.setAlbumPinned(it.albumId, !it.isPinned) },
            onLockAlbum = { galleryVm.setAlbumLocked(it.albumId, !it.isLocked) },
            onBack = { showAlbums = false }
        )
    }
    customAlbumDetail?.let { album ->
        val refs by galleryVm.albumItems(album.albumId).collectAsState(initial = emptyList())
        val items = remember(refs, allItems) {
            val byId = allItems.associateBy { it.id }
            refs.mapNotNull { byId[it.mediaStoreId] }
        }
        AlbumDetailScreen(if (album.isLocked) "Vault · ${album.name}" else album.name, items, ::openDetail) {
            customAlbumDetail = null
        }
    }
    if (showMemories) {
        AlertDialog(
            onDismissRequest = { showMemories = false },
            title = { Text("On this day") },
            text = {
                Text(
                    if (memories.isEmpty()) "No memories on this date in previous years — yet."
                    else memories.take(15).joinToString("\n") {
                        "• ${it.name} (${it.dateTaken.let { t ->
                            java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date(t))
                        }})"
                    }
                )
            },
            confirmButton = {
                TextButton({
                    showMemories = false
                    memories.firstOrNull()?.let(::openDetail)
                }) { Text(if (memories.isEmpty()) "Close" else "View first") }
            }
        )
    }
}
