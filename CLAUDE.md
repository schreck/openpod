# OpenPod — Claude Context

Simple Android podcast app. Keep everything as lean as possible — no unnecessary abstractions, no extra dependencies.

## Tech Stack

| Layer | Tool |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Navigation |
| Audio | ExoPlayer (`media3`) via `MediaSessionService` |
| RSS | OkHttp + `XmlPullParser` (stdlib, no extra parsing lib) |
| Database | Room with `Flow`-based DAOs |
| DI | Hilt |
| Images | Coil |
| State | `ViewModel` + `StateFlow` |

## Data Model

**Podcast** — feed URL, title, description, artwork URL, last refreshed timestamp
**Episode** — podcast FK, GUID, title, description, audio URL, duration, pub date, play position (ms), played flag, last played timestamp, download ID, local file path (nullable)

## Project Structure

```
app/src/main/java/com/openpod/
├── data/
│   ├── db/          # PodcastDao, EpisodeDao, AppDatabase (v3)
│   ├── download/    # DownloadRepository (OkHttp streaming + StateFlow progress)
│   ├── network/     # RssFetcher (OkHttp), RssParser (XmlPullParser)
│   └── repository/  # PodcastRepository (includes refreshAll)
├── player/          # PlaybackService (MediaSessionService), PlayerController
├── ui/
│   ├── home/        # HomeScreen (TabRow: Recent, History, Downloads, Podcasts)
│   ├── recent/      # RecentEpisodesScreen, RecentEpisodesViewModel
│   ├── history/     # PlayHistoryScreen, PlayHistoryViewModel
│   ├── downloads/   # DownloadsScreen, DownloadsViewModel
│   ├── podcasts/    # PodcastListScreen, PodcastListViewModel
│   ├── episodes/    # EpisodeListScreen, EpisodeListViewModel
│   ├── player/      # MiniPlayerBar, PlayerScreen, PlayerViewModel
│   └── common/      # DownloadButton, EpisodePlayButton (shared play/pause/played/in-progress icon logic)
└── MainActivity.kt  # Single activity, NavHost
```

## Playback Architecture

`PlaybackService` extends `MediaLibraryService` and runs as a foreground service.
`PlayerViewModel` connects via `MediaController` (no direct ExoPlayer reference in UI).
This gives background playback and notification/lock screen controls for free.

`PlayerController.playEpisode()` prefers `episode.localFilePath` over `episode.audioUrl` — plays local file if downloaded, otherwise streams. `PlayerState.isLocal` reflects which is active and is shown in the player UI.

## Download Architecture

`DownloadRepository` uses OkHttp to stream directly to `getExternalFilesDir(DIRECTORY_PODCASTS)`. Progress is tracked via `MutableStateFlow<Map<String, Float>>` updated on each 8KB buffer read — no polling needed. Cancellation uses `MutableMap<String, Job>`.

Episodes in the DB:
- `downloadId == -1`, `localFilePath == null` → not downloaded
- `downloadId == 1`, `localFilePath == null` → downloading/queued
- `downloadId == -1`, `localFilePath != null` → downloaded

## Feed Refresh

`PodcastRepository.refreshAll()` re-fetches all feeds and upserts episodes (Room `REPLACE` strategy). Called:
- On app open via `PodcastListViewModel.init {}`
- On pull-to-refresh on the Podcasts and Recent tabs
- When Android Auto connects via `PlaybackService.onGetLibraryRoot()`

## Android Auto

`PlaybackService` implements `MediaLibrarySession.Callback`. Main screen shows recent episodes as a flat playable list (`getAllRecentOnce()`). Overrides `onSetMediaItems` to re-resolve the audio URI from the DB (Media3 strips URIs over IPC). Custom layout sets a skip forward 30s `CommandButton` on the now playing screen.

## Tab Order

Home tabs (left to right): Recent → History → Downloads → Podcasts

## Theme

`OpenPodTheme` in `ui/theme/Theme.kt` — uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+ (Material You), falls back to standard Material3 dark/light schemes. `enableEdgeToEdge()` is called in `MainActivity`. Screens using `Scaffold` get insets automatically; `PlayerScreen` uses `safeDrawingPadding()`; the root `Column` in `MainActivity` uses `navigationBarsPadding()`.

## OPML

`OpmlParser` and `OpmlWriter` in `data/network/` use `XmlPullParser`. `PodcastRepository.importOpml()` fetches each feed; `exportOpml()` generates XML. UI in `PodcastListScreen` uses `rememberLauncherForActivityResult` for import and `FileProvider` + share intent for export.

## Build Phases

1. **Foundation** — Gradle setup, Room schema, Hilt wiring, navigation skeleton ✓
2. **RSS + Podcast list** — add feed by URL, fetch + parse + persist, display list ✓
3. **Episode list** — list episodes per feed, mark played ✓
4. **Playback** — ExoPlayer service, mini-player bar, seek/skip 30s ✓
5. **Resume position** — save and restore play position per episode ✓
6. **Full-screen player** — artwork, scrubber, time labels, skip controls ✓
7. **Android Auto** — MediaLibraryService browse tree, recent feed, skip forward ✓
8. **Downloads** — OkHttp streaming, progress tracking, offline playback ✓
9. **Play history** — history tab ordered by last played ✓
10. **Feed refresh** — on app open, pull-to-refresh, Android Auto connect ✓
11. **OPML import/export** — XmlPullParser-based, share sheet export ✓
12. **Dark mode** — dynamic color (Material You), edge-to-edge, proper insets ✓
13. **Consistent episode UI** — shared EpisodePlayButton with play/pause/played/in-progress states across all screens ✓
14. **Remaining** — playback speed control

## Testing

Run unit tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew test
```

**When to add tests:** Write unit tests for pure logic that doesn't need Android — parsing, formatting, data transformation. `RssParser`, `parseDurationMs`, and `episodePlayState` are the main examples. Don't test Room DAOs, Compose UI, or ExoPlayer wiring; those require instrumented tests and aren't worth the setup cost for this project.

**Avoid Android deps in tests:** Use `XmlPullParserFactory` instead of `android.util.Xml`, and keep XML fixture strings on a single line (or string concatenation) to avoid `trimIndent()` whitespace bugs with kxml2.

## Building & Installing

**Java:** Use OpenJDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64`:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew installDebug
```

**ADB:** Not on PATH — use the full path:

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices
```

Check for connected devices first. Phones show up when connected via USB with USB debugging enabled.

## Android Auto (Desktop Head Unit)

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb forward tcp:5277 tcp:5277
/opt/homebrew/share/android-commandlinetools/extras/google/auto/desktop-head-unit
```

Enable the head unit server in the Android Auto app on the phone first. DHU must stay in foreground (it's interactive).

## Release Build

R8 minification and resource shrinking are enabled for release. ProGuard rules are in `app/proguard-rules.pro`. Network security config allows cleartext HTTP for podcast feeds (`res/xml/network_security_config.xml`).

Signing credentials are read from `local.properties` (gitignored). Keystore is at `/root/openpod-upload.jks`.

**Every time you build an AAB, increment `versionCode` by 1 in `app/build.gradle.kts` before building.** Also update `versionName` if the user specifies one. Current version: versionCode 5, versionName 1.4.

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew bundleRelease && cp app/build/outputs/bundle/release/app-release.aab /mnt/c/temp/
```

## Before Merging or Making a PR

Always review `README.md` and `CLAUDE.md` and update them to reflect any new features, architecture changes, screen additions, or build/config changes introduced in the branch. Docs should be current before a branch lands on main.

## Conventions

- One `ViewModel` per screen
- DAOs return `Flow<List<T>>` for reactive UI; suspend functions for writes
- `Repository` is the only layer that touches both `db` and `network`
- No `LiveData` — use `StateFlow` / `collectAsStateWithLifecycle`
- Prefer `XmlPullParser` over third-party RSS libs to keep deps minimal
