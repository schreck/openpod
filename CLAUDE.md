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
**Episode** — podcast FK, GUID, title, description, audio URL, duration, pub date, play position (ms), played flag, local file path (nullable)

## Project Structure

```
app/src/main/java/com/openpod/
├── data/
│   ├── db/          # PodcastDao, EpisodeDao, AppDatabase
│   ├── network/     # RssFetcher (OkHttp), RssParser (XmlPullParser)
│   └── repository/  # PodcastRepository, EpisodeRepository
├── player/          # PlaybackService (MediaSessionService), PlayerController
├── ui/
│   ├── podcasts/    # PodcastListScreen, PodcastListViewModel
│   ├── episodes/    # EpisodeListScreen, EpisodeListViewModel
│   └── player/      # MiniPlayer composable, PlayerScreen, PlayerViewModel
└── MainActivity.kt  # Single activity, NavHost
```

## Playback Architecture

`PlaybackService` extends `MediaSessionService` and runs as a foreground service.
`PlayerViewModel` connects via `MediaController` (no direct ExoPlayer reference in UI).
This gives background playback and notification/lock screen controls for free.

## Build Phases

1. **Foundation** — Gradle setup, Room schema, Hilt wiring, navigation skeleton ✓
2. **RSS + Podcast list** — add feed by URL, fetch + parse + persist, display list ✓
3. **Episode list** — list episodes per feed, mark played ✓
4. **Playback** — ExoPlayer service, mini-player bar, seek/skip 30s ✓
5. **Resume position** — save and restore play position per episode ✓
6. **Full-screen player** — artwork, scrubber, time labels, skip controls ✓
7. **Android Auto** — MediaLibraryService browse tree ✓
8. **Remaining** — download to local storage, playback speed control

## Testing

Run unit tests:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew test
```

**When to add tests:** Write unit tests for pure logic that doesn't need Android — parsing, formatting, data transformation. `RssParser` is the main example. Don't test Room DAOs, Compose UI, or ExoPlayer wiring; those require instrumented tests and aren't worth the setup cost for this project.

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

## Conventions

- One `ViewModel` per screen
- DAOs return `Flow<List<T>>` for reactive UI; suspend functions for writes
- `Repository` is the only layer that touches both `db` and `network`
- No `LiveData` — use `StateFlow` / `collectAsStateWithLifecycle`
- Prefer `XmlPullParser` over third-party RSS libs to keep deps minimal
