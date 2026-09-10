package com.freedu.personalgallary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freedu.personalgallary.data.local.CustomAlbumEntity
import com.freedu.personalgallary.data.model.MediaItem
import com.freedu.personalgallary.data.model.MediaType
import com.freedu.personalgallary.ui.navigation.Routes
import com.freedu.personalgallary.ui.screens.AddToAlbumSheet
import com.freedu.personalgallary.ui.screens.AlbumDetailScreen
import com.freedu.personalgallary.ui.screens.AlbumsScreen
import com.freedu.personalgallary.ui.screens.CollageScreen
import com.freedu.personalgallary.ui.screens.DetailScreen
import com.freedu.personalgallary.ui.screens.FavoritesScreen
import com.freedu.personalgallary.ui.screens.FeedScreen
import com.freedu.personalgallary.ui.screens.FilterScreen
import com.freedu.personalgallary.ui.screens.FolderFilterDialog
import com.freedu.personalgallary.ui.screens.GalleryFilter
import com.freedu.personalgallary.ui.screens.GalleryScreen
import com.freedu.personalgallary.ui.screens.LockScreen
import com.freedu.personalgallary.ui.screens.MarkupScreen
import com.freedu.personalgallary.ui.screens.MediaDetailsDialog
import com.freedu.personalgallary.ui.screens.OnboardingScreen
import com.freedu.personalgallary.ui.screens.PlacesScreen
import com.freedu.personalgallary.ui.screens.ProfileScreen
import com.freedu.personalgallary.ui.screens.ReelsScreen
import com.freedu.personalgallary.ui.screens.SlideshowScreen
import com.freedu.personalgallary.ui.screens.StatsScreen
import com.freedu.personalgallary.ui.screens.StorageScreen
import com.freedu.personalgallary.ui.screens.TagsScreen
import com.freedu.personalgallary.ui.screens.ToolsScreen
import com.freedu.personalgallary.ui.screens.TrashScreen
import com.freedu.personalgallary.ui.screens.TrimScreen
import com.freedu.personalgallary.ui.screens.VaultScreen
import com.freedu.personalgallary.ui.screens.YearInReviewScreen
import com.freedu.personalgallary.ui.theme.PersonalGallaryTheme
import com.freedu.personalgallary.ui.viewmodel.GalleryViewModel
import com.freedu.personalgallary.ui.viewmodel.SettingsViewModel
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.PdfExport
import com.freedu.personalgallary.util.ShakeDetector
import com.freedu.personalgallary.util.ShareUtils
import com.freedu.personalgallary.util.WallpaperHelper
import com.freedu.personalgallary.worker.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class MainActivity : FragmentActivity() {

    private var lastActiveMs = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val galleryVm: GalleryViewModel = viewModel()
            val settings by settingsVm.state.collectAsState()
            val accentArgb by galleryVm.accentColor.collectAsState()
            PersonalGallaryTheme(
                themeMode = settings.theme,
                accent = if (settings.dynamicAccent && accentArgb != null) Color(accentArgb!!) else null
            ) {
                LaunchedEffect(settings.hideFromRecents, settings.blockScreenshots) {
                    applySecureFlags(settings.hideFromRecents || settings.blockScreenshots)
                }
                AppRoot(
                    settingsVm = settingsVm,
                    galleryVm = galleryVm,
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

private val IMMERSIVE_ROUTES = setOf(
    Routes.REELS, Routes.SLIDESHOW, Routes.COLLAGE,
    Routes.FILTER, Routes.MARKUP, Routes.TRIM
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
    galleryVm: GalleryViewModel,
    onUserActive: () -> Unit,
    lastActiveMs: () -> Long
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val haptics = LocalHapticFeedback.current
    val state by galleryVm.state.collectAsState()
    val settings by settingsVm.state.collectAsState()
    val customAlbums by galleryVm.customAlbums.collectAsState()
    val attemptList by galleryVm.attempts.collectAsState()

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var unlocked by remember { mutableStateOf(false) }
    var decoyMode by remember { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var setupPinMode by remember { mutableStateOf(false) }
    var decoySetupMode by remember { mutableStateOf(false) }
    var pendingVaultAlbum by remember { mutableStateOf<CustomAlbumEntity?>(null) }
    var vaultUnlockedIds by remember { mutableStateOf(setOf<Long>()) }
    var vaultOpen by remember { mutableStateOf(false) }
    var vaultGate by remember { mutableStateOf(false) }

    // gallery UI state (each tab keeps its own search text)
    var galleryQuery by remember { mutableStateOf("") }
    var feedQuery by remember { mutableStateOf("") }
    var reelsQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(GalleryFilter.ALL) }
    var isGrid by remember { mutableStateOf(true) }
    var deviceAlbumFilter by remember { mutableStateOf<String?>(null) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var detailCtx by remember { mutableStateOf<List<MediaItem>?>(null) }

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
    var pendingReminder by remember { mutableStateOf(false) }

    val needsLock = settings.appLock && settingsVm.locks.hasPin() && !unlocked

    // dynamic accent from latest photo
    LaunchedEffect(settings.dynamicAccent, state.allItems.firstOrNull()?.id) {
        if (settings.dynamicAccent) galleryVm.loadAccent()
    }

    // auto-lock + permission refresh + shake-to-shuffle on gallery
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sensorMgr = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    DisposableEffect(lifecycle, currentRoute) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val idleMin = (System.currentTimeMillis() - lastActiveMs()) / 60000
                if (settings.appLock && settingsVm.locks.hasPin() && unlocked && idleMin >= settings.autoLockMinutes) {
                    unlocked = false
                    decoyMode = false
                    vaultOpen = false
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
        var detector: ShakeDetector? = null
        if (currentRoute == Routes.GALLERY) {
            detector = ShakeDetector {
                shuffleSeed++
                Toast.makeText(context, "Shuffled — shake anytime", Toast.LENGTH_SHORT).show()
            }
            sensorMgr.registerListener(
                detector,
                sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
            )
        }
        onDispose {
            lifecycle.removeObserver(obs)
            detector?.let { sensorMgr.unregisterListener(it) }
        }
    }

    // permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants.values.any { it }
        galleryVm.setHasPermission(ok)
        if (ok) settingsVm.setOnboarded()
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingReminder) {
            scheduleReminder(context)
            settingsVm.setDailyReminder(true)
        } else if (!granted) {
            Toast.makeText(context, "Reminder needs notification permission", Toast.LENGTH_LONG).show()
        }
        pendingReminder = false
    }
    LaunchedEffect(Unit) {
        val granted = requiredPermissions().any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        galleryVm.setHasPermission(granted)
    }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun shareItem(item: MediaItem) {
        onUserActive()
        ShareUtils.share(context, listOf(item))
    }

    fun doExport() {
        onUserActive()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favs = galleryVm.favorites(galleryVm.visibleItems())
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup saved: ${zip.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onReminderToggle(enable: Boolean) {
        onUserActive()
        if (!enable) {
            WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
            settingsVm.setDailyReminder(false)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminder = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        scheduleReminder(context)
        settingsVm.setDailyReminder(true)
    }

    fun authenticateBiometric(onOk: () -> Unit, onFail: (String) -> Unit) {
        val mgr = BiometricManager.from(context)
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
    if (needsLock && !setupPinMode && !decoySetupMode) {
        val bioOk =
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS && settings.biometric
        LockScreen(
            hasPin = true,
            biometricAvailable = bioOk,
            error = lockError,
            onPin = { pin ->
                when {
                    settingsVm.verifyPin(pin) -> {
                        unlocked = true
                        decoyMode = false
                        lockError = null
                        onUserActive()
                    }
                    settingsVm.verifyDecoy(pin) -> {
                        unlocked = true
                        decoyMode = true
                        lockError = null
                        onUserActive()
                    }
                    else -> {
                        lockError = "Wrong PIN — try again"
                        galleryVm.logAttempt()
                    }
                }
            },
            onBiometric = {
                authenticateBiometric(
                    onOk = { unlocked = true; decoyMode = false; lockError = null; onUserActive() },
                    onFail = { lockError = it }
                )
            }
        )
        return
    }
    if (setupPinMode || decoySetupMode || pendingVaultAlbum != null || vaultGate) {
        LockScreen(
            hasPin = settingsVm.locks.hasPin(),
            biometricAvailable = false,
            setupMode = setupPinMode || decoySetupMode,
            error = lockError,
            onPin = { combined ->
                if (setupPinMode || decoySetupMode) {
                    val parts = combined.split("|")
                    if (parts.size == 2 && parts[0] == parts[1] && parts[0].length >= 4) {
                        if (decoySetupMode) {
                            settingsVm.saveDecoy(parts[0])
                            decoySetupMode = false
                            toast("Decoy PIN saved")
                        } else {
                            settingsVm.savePin(parts[0])
                            settingsVm.setAppLock(true)
                            setupPinMode = false
                            unlocked = true
                        }
                        lockError = null
                    } else lockError = "PINs don't match (min 4 digits)"
                } else {
                    // vault unlock
                    if (settingsVm.verifyPin(combined)) {
                        pendingVaultAlbum?.let { vaultUnlockedIds = vaultUnlockedIds + it.albumId }
                        pendingVaultAlbum = null
                        if (vaultGate) {
                            vaultGate = false
                            vaultOpen = true
                            nav.navigate(Routes.VAULT) { launchSingleTop = true }
                        }
                        lockError = null
                        onUserActive()
                    } else {
                        lockError = "Wrong PIN"
                        galleryVm.logAttempt()
                    }
                }
            },
            onBiometric = {},
            onCancel = {
                setupPinMode = false
                decoySetupMode = false
                pendingVaultAlbum = null
                vaultGate = false
                lockError = null
            }
        )
        return
    }

    // ---- derived lists (trash + vault hidden) ----
    val allItems = state.allItems
    val allById = remember(allItems) { allItems.associateBy { it.id } }
    val visible = remember(allItems, state.trashedIds, state.lockedIds) {
        galleryVm.visibleItems(allItems)
    }
    val visibleById = remember(visible) { visible.associateBy { it.id } }
    val reels = remember(visible) { galleryVm.reels(visible) }
    val posts = remember(visible) { galleryVm.posts(visible) }
    val favItems = remember(visible, state.favorites, decoyMode) {
        if (decoyMode) emptyList() else galleryVm.favorites(visible)
    }
    val memories = remember(visible) { galleryVm.memories(visible) }
    val stories = remember(visible) { galleryVm.stories(visible) }
    val deviceAlbums = remember(visible) { galleryVm.deviceAlbums(visible) }
    val insights = remember(visible) { galleryVm.insights(visible) }
    val trashItems = remember(allItems, state.trashedIds) { galleryVm.trashedItems() }
    val vaultItems = remember(allItems, state.lockedIds) { galleryVm.lockedItems() }
    val customShown = remember(customAlbums, decoyMode) {
        if (decoyMode) emptyList() else customAlbums
    }
    val feedPosts = remember(posts, feedQuery, visible) {
        if (feedQuery.isBlank()) posts else galleryVm.search(feedQuery, posts)
    }
    val attemptsText = remember(attemptList) {
        if (attemptList.isEmpty()) "No failed attempts recorded"
        else {
            val last = attemptList.maxOf { it.ts }
            "${attemptList.size} failed · last ${FormatUtils.formatDate(last)}"
        }
    }

    val reelsVisible = remember(visible, reelsQuery) {
        if (reelsQuery.isBlank()) reels else galleryVm.search(reelsQuery, reels)
    }

    val galleryBase: List<MediaItem> = remember(visible, filter, deviceAlbumFilter, galleryQuery, state.favorites, shuffleSeed) {
        var list = when (filter) {
            GalleryFilter.ALL -> visible
            GalleryFilter.PHOTOS -> visible.filter { it.type == MediaType.IMAGE }
            GalleryFilter.VIDEOS -> visible.filter { it.type == MediaType.VIDEO }
            GalleryFilter.REELS -> visible.filter { it.isReel }
            GalleryFilter.FAVORITES -> visible.filter { it.id in state.favorites }
        }
        if (deviceAlbumFilter != null) list = list.filter { it.albumName == deviceAlbumFilter }
        if (galleryQuery.isNotBlank()) list = galleryVm.search(galleryQuery, list)
        if (shuffleSeed > 0) list.shuffled(Random(shuffleSeed)) else list
    }

    fun openDetail(item: MediaItem) {
        onUserActive()
        detailCtx = null
        nav.navigate(Routes.detail(item.id)) { launchSingleTop = true }
    }

    fun openDetailInCtx(item: MediaItem, ctx: List<MediaItem>) {
        onUserActive()
        detailCtx = ctx
        nav.navigate(Routes.detail(item.id)) { launchSingleTop = true }
    }

    fun moveToTrashAndBack(item: MediaItem) {
        galleryVm.moveToTrash(item)
        toast("Moved to trash · 30 days to restore")
        if (currentRoute?.startsWith("detail") == true) nav.popBackStack()
    }

    fun playSlideshow(items: List<MediaItem>, title: String) {
        if (items.isEmpty()) {
            toast("Nothing to play yet")
            return
        }
        onUserActive()
        galleryVm.setSlideshow(items, title)
        nav.navigate(Routes.SLIDESHOW) { launchSingleTop = true }
    }

    // Main scaffold with bottom nav
    Scaffold(
        bottomBar = {
            if (currentRoute !in IMMERSIVE_ROUTES) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                onUserActive()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        posts = feedPosts,
                        memories = memories,
                        stories = stories,
                        totalCount = visible.size,
                        isFavorite = { galleryVm.isFavorite(it) },
                        captionFor = { galleryVm.captionFor(it) },
                        onOpen = { openDetailInCtx(it, feedPosts) },
                        onToggleFavorite = { galleryVm.toggleFavorite(it); onUserActive() },
                        onShare = ::shareItem,
                        onDelete = { pendingDelete = it },
                        onEditCaption = {
                            captionTarget = it
                            captionText = galleryVm.captionFor(it.id).orEmpty()
                        },
                        onAddToAlbum = { albumTarget = it },
                        onOpenMemories = { showMemories = true },
                        onRefresh = { galleryVm.refresh() },
                        onSurprise = {
                            galleryVm.randomItem(visible)?.let { openDetailInCtx(it, visible) }
                                ?: toast("Nothing here yet")
                        },
                        query = feedQuery,
                        onQuery = { feedQuery = it }
                    )
                }
                composable(Routes.REELS) {
                    ReelsScreen(
                        reels = reelsVisible,
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
                                toast(if (ok) "Wallpaper set" else "Couldn't set wallpaper")
                            }
                        },
                        query = reelsQuery,
                        onQuery = { reelsQuery = it }
                    )
                }
                composable(Routes.GALLERY) {
                    GalleryScreen(
                        items = galleryBase,
                        albums = deviceAlbums,
                        activeFilter = filter,
                        onFilter = { filter = it },
                        query = galleryQuery,
                        onQuery = { galleryQuery = it },
                        isGrid = isGrid,
                        onToggleView = { isGrid = !isGrid },
                        selectedAlbum = deviceAlbumFilter,
                        onSelectAlbum = { deviceAlbumFilter = it },
                        isFavorite = { galleryVm.isFavorite(it) },
                        onOpen = { openDetailInCtx(it, galleryBase) },
                        onOpenAlbumDetail = { name -> nav.navigate(Routes.albumDetail(name)) },
                        photoCount = insights.photoCount,
                        videoCount = insights.videoCount,
                        onShuffle = {
                            shuffleSeed++
                            toast("Shuffled — shake anytime")
                        }
                    )
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(favItems) { openDetailInCtx(it, favItems) }
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
                        onRefresh = { galleryVm.refresh() },
                        dynamicAccent = settings.dynamicAccent,
                        onDynamicAccent = { settingsVm.setDynamicAccent(it) },
                        onOpenTools = { nav.navigate(Routes.TOOLS) { launchSingleTop = true } }
                    )
                }
                composable(Routes.DETAIL) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    val fallback = when (currentRoute) {
                        Routes.FAVORITES -> favItems
                        Routes.GALLERY -> galleryBase
                        else -> visible
                    }.ifEmpty { visible }
                    val ctxList = detailCtx?.takeIf { list -> list.any { it.id == id } } ?: fallback
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
                        onEditPhoto = { nav.navigate(Routes.filter(it.id)) },
                        onMarkup = { nav.navigate(Routes.markup(it.id)) },
                        onTrimVideo = { nav.navigate(Routes.trim(it.id)) },
                        onLockItem = {
                            galleryVm.lockItem(it)
                            toast("Moved to vault")
                            if (currentRoute?.startsWith("detail") == true) nav.popBackStack()
                        },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Routes.ALBUM_DETAIL) { entry ->
                    val name = Routes.decodeAlbumName(entry.arguments?.getString("albumName"))
                    val items = remember(visible, name) {
                        visible.filter { it.albumName == name }
                    }
                    AlbumDetailScreen(
                        name, items,
                        { openDetailInCtx(it, items) },
                        { nav.popBackStack() },
                        onPlay = { playSlideshow(items, name) }
                    )
                }
                composable(Routes.TOOLS) {
                    ToolsScreen(
                        reminder = settings.dailyReminder,
                        onReminder = ::onReminderToggle,
                        dynamicAccent = settings.dynamicAccent,
                        onDynamicAccent = { settingsVm.setDynamicAccent(it) },
                        onSlideshow = { playSlideshow(memories.ifEmpty { stories }, "Memories") },
                        onReview = { nav.navigate(Routes.REVIEW) { launchSingleTop = true } },
                        onStats = { nav.navigate(Routes.STATS) { launchSingleTop = true } },
                        onCollage = { nav.navigate(Routes.COLLAGE) { launchSingleTop = true } },
                        onPdfFavorites = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val uri = PdfExport.exportAlbum(context, "Favorites", favItems)
                                withContext(Dispatchers.Main) {
                                    if (uri != null) ShareUtils.shareFile(context, uri, "application/pdf", "Favorites book")
                                    else toast("Nothing to export — like some photos first")
                                }
                            }
                        },
                        onTags = { nav.navigate(Routes.TAGS) { launchSingleTop = true } },
                        onStorage = { nav.navigate(Routes.STORAGE) { launchSingleTop = true } },
                        onPlaces = { nav.navigate(Routes.PLACES) { launchSingleTop = true } },
                        trashCount = trashItems.size,
                        onTrash = { nav.navigate(Routes.TRASH) { launchSingleTop = true } },
                        onVault = {
                            if (settingsVm.locks.hasPin() && !vaultOpen) vaultGate = true
                            else nav.navigate(Routes.VAULT) { launchSingleTop = true }
                        },
                        decoySet = settings.hasDecoy,
                        onSetupDecoy = { decoySetupMode = true; lockError = null },
                        onClearDecoy = { settingsVm.clearDecoy(); toast("Decoy PIN removed") },
                        attemptsText = attemptsText,
                        onClearAttempts = { galleryVm.clearAttempts() },
                        decoyMode = decoyMode
                    )
                }
                composable(Routes.SLIDESHOW) {
                    SlideshowScreen(
                        items = galleryVm.slideshowItems,
                        title = galleryVm.slideshowTitle,
                        onClose = { nav.popBackStack() }
                    )
                }
                composable(Routes.COLLAGE) {
                    CollageScreen(
                        items = visible,
                        onDone = { ok ->
                            toast(if (ok) "Collage saved to gallery" else "Couldn't save collage")
                            if (ok) galleryVm.refresh()
                            nav.popBackStack()
                        },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Routes.FILTER) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    val m = allById[id]
                    if (m == null || m.isVideo) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FilterScreen(
                            item = m,
                            onDone = { ok ->
                                toast(if (ok) "Edited copy saved" else "Couldn't save")
                                if (ok) galleryVm.refresh()
                                nav.popBackStack()
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable(Routes.MARKUP) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    val m = allById[id]
                    if (m == null || m.isVideo) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        MarkupScreen(
                            item = m,
                            onDone = { ok ->
                                toast(if (ok) "Annotated copy saved" else "Couldn't save")
                                if (ok) galleryVm.refresh()
                                nav.popBackStack()
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable(Routes.TRIM) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    val m = allById[id]
                    if (m == null || !m.isVideo) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        TrimScreen(
                            item = m,
                            onTrimDone = {
                                toast("Video exported to gallery")
                                galleryVm.refresh()
                                nav.popBackStack()
                            },
                            onCoverSaved = { ok ->
                                toast(if (ok) "Cover saved to gallery" else "Couldn't save cover")
                                if (ok) galleryVm.refresh()
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable(Routes.STATS) {
                    StatsScreen(
                        items = visible,
                        albums = deviceAlbums,
                        favCount = favItems.size,
                        onShare = { ShareUtils.shareText(context, it, "My gallery stats") }
                    )
                }
                composable(Routes.STORAGE) {
                    StorageScreen(
                        items = visible,
                        albums = deviceAlbums,
                        onOpen = { openDetail(it) },
                        onReviewScreenshots = {
                            galleryQuery = "screenshots"
                            filter = GalleryFilter.ALL
                            deviceAlbumFilter = null
                            nav.navigate(Routes.GALLERY) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Routes.PLACES) {
                    PlacesScreen(
                        load = { cb -> galleryVm.loadPlaces(cb) },
                        itemById = { visibleById[it] },
                        onOpen = { openDetail(it) }
                    )
                }
                composable(Routes.TAGS) {
                    TagsScreen(
                        tags = galleryVm.allTags(),
                        itemsForTag = { tag -> galleryVm.itemsWithTag(tag, visible) },
                        onOpen = { openDetail(it) }
                    )
                }
                composable(Routes.TRASH) {
                    TrashScreen(
                        items = trashItems,
                        trashedAt = state.trashedAt,
                        onRestore = {
                            galleryVm.restoreFromTrash(it)
                            toast("Restored")
                        },
                        onDeleteForever = { target ->
                            galleryVm.deleteForever(target) { ok ->
                                toast(if (ok) "Permanently deleted" else "Couldn't delete")
                            }
                        },
                        onEmptyTrash = {
                            galleryVm.emptyTrash { n -> toast("$n items permanently deleted") }
                        },
                        onOpen = { openDetailInCtx(it, trashItems) }
                    )
                }
                composable(Routes.REVIEW) {
                    YearInReviewScreen(
                        items = visible,
                        favItems = favItems,
                        onOpen = { openDetail(it) },
                        onShare = { ShareUtils.shareText(context, it, "My year in review") }
                    )
                }
                composable(Routes.VAULT) {
                    VaultScreen(
                        items = vaultItems,
                        onOpen = { openDetailInCtx(it, vaultItems) },
                        onUnlock = {
                            galleryVm.unlockItem(it)
                            toast("Removed from vault")
                        }
                    )
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
            customAlbums = customShown,
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
            title = { Text("Move to trash?") },
            text = { Text("${target.name}\nYou can restore it within 30 days.") },
            confirmButton = {
                TextButton({
                    pendingDelete = null
                    moveToTrashAndBack(target)
                }) { Text("Move to trash", color = MaterialTheme.colorScheme.error) }
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
            customAlbums = customShown,
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
        val items = remember(refs, visible) {
            val byId = visible.associateBy { it.id }
            refs.mapNotNull { byId[it.mediaStoreId] }
        }
        AlbumDetailScreen(
            if (album.isLocked) "Vault · ${album.name}" else album.name,
            items,
            { openDetailInCtx(it, items) },
            { customAlbumDetail = null },
            onPlay = { playSlideshow(items, album.name) }
        )
    }
    if (showMemories) {
        val week = remember(visible) { galleryVm.memoriesWeek(visible) }
        val month = remember(visible) { galleryVm.memoriesMonth(visible) }
        AlertDialog(
            onDismissRequest = { showMemories = false },
            title = { Text("On this day & around it") },
            text = {
                Text(
                    when {
                        memories.isEmpty() && week.isEmpty() ->
                            "No memories on this date in previous years — yet."
                        else -> buildString {
                            if (memories.isNotEmpty()) appendLine("• ${memories.size} from this exact day")
                            if (week.isNotEmpty()) appendLine("• ${week.size} from this week, past years")
                            if (month.isNotEmpty()) appendLine("• ${month.size} from this month, past years")
                        }
                    }
                )
            },
            confirmButton = {
                TextButton({
                    showMemories = false
                    val play = memories.ifEmpty { week }.ifEmpty { month }
                    playSlideshow(play.ifEmpty { stories }, "Memories")
                }) { Text(if (memories.isEmpty() && week.isEmpty()) "Close" else "Play slideshow") }
            }
        )
    }
}

private fun scheduleReminder(context: Context) {
    val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        ReminderWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        req
    )
}
