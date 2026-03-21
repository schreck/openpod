# OpenPod

A simple, open-source podcast app for Android.

## Features

- Subscribe to any podcast by RSS feed URL
- Browse and play episodes
- Background playback with notification and lock screen controls
- Resume from last position
- Full-screen player with artwork, scrubber, and skip controls
- Download episodes for offline playback
- Android Auto support
- Feeds refresh automatically on app open and on Android Auto connect

## Stack

- **UI** — Kotlin + Jetpack Compose
- **Audio** — ExoPlayer via `MediaSessionService`
- **RSS** — OkHttp + `XmlPullParser`
- **Storage** — Room
- **DI** — Hilt
- **Images** — Coil

## Architecture

Single-module MVVM app.

```
app/
├── data/
│   ├── db/          # Room entities, DAOs, database
│   ├── download/    # DownloadRepository (OkHttp streaming, progress tracking)
│   ├── network/     # RSS HTTP client + XML parser
│   └── repository/  # PodcastRepository
├── player/          # ExoPlayer service + MediaController wrapper
├── ui/
│   ├── home/        # HomeScreen (tab host)
│   ├── recent/      # Recent episodes tab
│   ├── history/     # Play history tab
│   ├── downloads/   # Downloads tab (in-progress + completed)
│   ├── podcasts/    # Podcast list tab
│   ├── episodes/    # Episode list screen
│   └── player/      # Mini-player bar + full-screen player
└── MainActivity.kt
```

## Screens

- **Recent** — latest 100 episodes across all feeds, pull-to-refresh
- **History** — episodes ordered by last played
- **Downloads** — in-progress downloads with progress bar, completed downloads with play/delete
- **Podcasts** — subscribed feeds, add by RSS URL, pull-to-refresh
- **Episode List** — episodes for a feed; progress bar shows unplayed / in-progress / finished
- **Player** — persistent mini-player bar (tap to expand), full-screen player with artwork, scrubber, skip controls, and streaming/local indicator

## Downloads

Episodes can be downloaded from any episode list screen. The Downloads tab shows:
- In-progress: progress bar, percentage, cancel button
- Completed: DownloadDone icon, play and delete buttons

Downloaded episodes play from the local file; all others stream. The player shows "Local file" or "Streaming" to indicate which.

## Android Auto

Supported via `MediaLibraryService`. Browse tree: Podcasts → Episodes. Feeds refresh automatically when Android Auto connects.

## Testing

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew test
```

Unit tests live in `app/src/test/`. Covers `RssParser` and `parseDurationMs`.

Write unit tests for pure logic only (parsing, formatting, data transformation). Skip Room DAOs, Compose UI, and ExoPlayer wiring.

## Build & Install

Requires Java 11+. Use Homebrew OpenJDK 17 if the system JDK is older:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew installDebug
```

Check connected devices first:

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices
```

## Android Auto (Desktop Head Unit)

To test Android Auto locally:

```bash
# Forward the port
/opt/homebrew/share/android-commandlinetools/platform-tools/adb forward tcp:5277 tcp:5277

# Launch the DHU (keep terminal open)
/opt/homebrew/share/android-commandlinetools/extras/google/auto/desktop-head-unit
```

Enable the head unit server in the Android Auto app on your phone first.

## License

Apache 2.0 — see [LICENSE](LICENSE).
