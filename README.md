# Personal Gallary

**Your own private social feed — 100% offline, 100% yours.**

An Android gallery app with the *feel* of Instagram (Reels + Post feed + grid gallery), powered entirely by the media already stored on the device. No internet, no account, no cloud upload, no tracking.

## ✨ Features

### Browse
- **Home Feed** — Instagram-style post cards for photos & long videos, stories tray, captions, double-tap to like, branded header with surprise-me dice
- **Reels** — full-screen vertical swipe feed for videos ≤ 60s (ExoPlayer, loop, mute, tap-pause, heart-burst likes, playback progress, floating search)
- **Gallery** — 3-column grid (2–5 adjustable) with month section headers, timeline fast-scrubber, list/grid toggle, filters, batch multi-select, shake-to-shuffle
- **Liked** — all favorites in one place
- **Detail view** — swipe-through posts, pinch-to-zoom, inline video, overflow actions

### Search (per-tab, independent)
- Names, albums, captions, **#hashtags**, dates, smart queries (`"june 2023"`, `"screenshots"`, `"large"`, `"this year"`)
- **Text-in-photo (OCR)** index and **Faces** wall (Everyone / Smiles / Groups), all on-device via ML Kit

### Create & edit (originals never touched — always a new copy)
- Photo editor (presets + tune), markup & annotate, before/after compare slider
- Photo compress (High/Medium/Small + live size estimate) and resizer (country passport specs, stamp, print, custom mm/px, DPI choice)
- Video trim + mute + cover frame, video compress (1080p/720p/480p H.264)
- Collage maker, favorites PDF book, blur-faces-before-share

### Organize
- Device-folder albums + reference-based custom collections (pin/lock), smart albums (New this week, This month, Screenshots, Long videos, Reels)
- Tags, storage analyzer, Places (EXIF GPS clusters), duplicates & burst cleaner, auto-rules, secure 30-day trash
- EXIF date fixer (local override), photo journal (day notes), voice notes on photos

### Memories
- On This Day / week / month, memory slideshow (Ken Burns), guest kiosk mode, year in review, stats dashboard (streaks, heatmap), daily reminder notification, living wallpaper + photo widget

### Privacy & lock
- App lock: encrypted PIN + biometrics, auto-lock, decoy PIN mode, panic gesture (triple volume-down → decoy)
- Per-item vault, lockable albums, time capsules, break-in attempt log, hide-from-recents, screenshot block, AES-256 password-encrypted backup

### Theming
- Light / Dark / System, brand gradient identity, optional photo-matched dynamic accent

## 🔒 Privacy principles
- **No `INTERNET` permission** — the app is fully offline (ML Kit models ship in the APK, ~105 MB)
- Media is only ever *referenced* via `MediaStore`; metadata (likes, albums, captions…) lives in a local Room DB
- Edited/compressed/blurred outputs are new files; originals are preserved unless explicitly trashed

## 🛠 Tech stack
| Area | Library |
|---|---|
| UI | Jetpack Compose (Material3), Navigation Compose, Coil 2 (video frames), zoomable |
| Video | Media3 ExoPlayer / Transformer / Effect |
| Data | Room (KSP), DataStore Preferences, EncryptedSharedPreferences, WorkManager |
| On-device AI | ML Kit face detection + text recognition, Palette |
| Security | BiometricPrompt, AES-256-GCM + PBKDF2 backup |

- Language: Kotlin · Min SDK 26 · Target/Compile SDK 35 · MVVM + Repository, Coroutines/Flow

## 🔑 Permissions
| Permission | Why |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (or legacy storage) | Index existing photos & videos |
| `USE_BIOMETRIC` | Biometric unlock |
| `SET_WALLPAPER` | Set-as-wallpaper |
| `POST_NOTIFICATIONS` | Daily memory reminder (optional) |
| `RECORD_AUDIO` | Voice notes (optional) |

## 🚀 Build & run
Requirements: Android SDK (Platform 35, Build-tools), JDK 17+.

```bash
# from the project root
export JAVA_HOME=<path-to-jdk-17+>
./gradlew :app:assembleDebug      # APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest  # unit tests
./gradlew :app:installDebug       # install on a connected device
```

Open the project in Android Studio (Ladybug or newer) and press **Run** — first launch asks for media access once, then auto-indexes the library.

## 🗂 Project structure
```
app/src/main/java/com/freedu/personalgallary/
├── MainActivity.kt            # nav graph, gates (onboarding/lock/vault), dialogs, sensors
├── GalleryApp.kt              # Coil setup, notification channel, periodic re-scan
├── data/
│   ├── model/                 # MediaItem, Album, SortOrder, SmartKeys, …
│   ├── local/                 # Room DB + DAOs (favorites, albums, trash, vault, journal, …)
│   ├── media/                 # MediaStoreRepository (scoped-storage indexer)
│   └── prefs/                 # SettingsRepository (DataStore), LockRepository (encrypted PIN)
├── ui/
│   ├── screens/               # Feed, Reels, Gallery, Detail, editors, labs tools, …
│   ├── components/            # thumbs, story rings, like buttons, shimmer, dialogs helpers
│   ├── theme/                 # colors, gradients, Material3 theme
│   ├── navigation/            # Routes
│   └── viewmodel/             # GalleryViewModel, SettingsViewModel
├── util/                      # image edit, collage, PDF, crypto backup, stats, ML helpers, …
├── worker/                    # MediaRescanWorker, ReminderWorker
├── widget/                    # memory home-screen widget
└── wallpaper/                 # living-wallpaper service
```

## 📝 Notes
- Package: `com.freedu.personalgallary` · App name intentionally spelled **"Personal Gallary"**
- Debug APK is large (~105 MB) because ML Kit face/text models are bundled for offline use
- Deleting from the app moves files to a 30-day trash first; "Delete forever" (trash screen) removes them from device storage
