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
│   └── common/      # DownloadButton
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

`PlaybackService` implements `MediaLibrarySession.Callback` with a two-level browse tree: Podcasts → Episodes. Overrides `onSetMediaItems` to re-resolve the audio URI from the DB (Media3 strips URIs over IPC).

## Tab Order

Home tabs (left to right): Recent → History → Downloads → Podcasts

## Build Phases

1. **Foundation** — Gradle setup, Room schema, Hilt wiring, navigation skeleton ✓
2. **RSS + Podcast list** — add feed by URL, fetch + parse + persist, display list ✓
3. **Episode list** — list episodes per feed, mark played ✓
4. **Playback** — ExoPlayer service, mini-player bar, seek/skip 30s ✓
5. **Resume position** — save and restore play position per episode ✓
6. **Full-screen player** — artwork, scrubber, time labels, skip controls ✓
7. **Android Auto** — MediaLibraryService browse tree ✓
8. **Downloads** — OkHttp streaming, progress tracking, offline playback ✓
9. **Play history** — history tab ordered by last played ✓
10. **Feed refresh** — on app open, pull-to-refresh, Android Auto connect ✓
11. **Remaining** — playback speed control

## Testing

Run unit tests:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew test
```

**When to add tests:** Write unit tests for pure logic that doesn't need Android — parsing, formatting, data transformation. `RssParser` and `parseDurationMs` are the main examples. Don't test Room DAOs, Compose UI, or ExoPlayer wiring; those require instrumented tests and aren't worth the setup cost for this project.

**Avoid Android deps in tests:** Use `XmlPullParserFactory` instead of `android.util.Xml`, and keep XML fixture strings on a single line (or string concatenation) to avoid `trimIndent()` whitespace bugs with kxml2.

## Building & Installing

**Java:** The system JDK is Java 8, but AGP 8.5 requires Java 11+. Use the Homebrew OpenJDK 17:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew installDebug
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

## Conventions

- One `ViewModel` per screen
- DAOs return `Flow<List<T>>` for reactive UI; suspend functions for writes
- `Repository` is the only layer that touches both `db` and `network`
- No `LiveData` — use `StateFlow` / `collectAsStateWithLifecycle`
- Prefer `XmlPullParser` over third-party RSS libs to keep deps minimal
