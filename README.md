# OpenPod

A simple, open-source podcast app for Android.

## Features

- Subscribe to any podcast by RSS feed URL
- Import and export subscriptions via OPML
- Browse and play episodes
- Background playback with notification and lock screen controls
- Resume from last position
- Full-screen player with artwork, scrubber, and skip controls
- Download episodes for offline playback
- Dark mode with dynamic color (Material You)
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
app/src/main/java/com/openpod/
├── data/
│   ├── db/              # Room entities (Podcast, Episode), DAOs, AppDatabase
│   ├── download/        # DownloadRepository — OkHttp streaming, progress tracking
│   ├── network/         # RssFetcher, RssParser, OpmlParser, OpmlWriter
│   └── repository/      # PodcastRepository — coordinates db + network
├── di/                  # Hilt modules (DatabaseModule, NetworkModule)
├── player/              # PlaybackService (MediaLibraryService), PlayerController
├── ui/
│   ├── common/          # DownloadButton
│   ├── downloads/       # Downloads tab — DownloadsScreen, DownloadsViewModel
│   ├── episodes/        # Episode list — EpisodeListScreen, EpisodeListViewModel, EpisodeProgress
│   ├── history/         # Play history tab — PlayHistoryScreen, PlayHistoryViewModel
│   ├── home/            # HomeScreen (tab host)
│   ├── player/          # MiniPlayerBar, PlayerScreen, PlayerViewModel
│   ├── podcasts/        # Podcast list tab — PodcastListScreen, PodcastListViewModel
│   ├── recent/          # Recent episodes tab — RecentEpisodesScreen, RecentEpisodesViewModel
│   └── theme/           # OpenPodTheme (Material You / dynamic color)
├── MainActivity.kt
└── OpenPodApplication.kt
```

## Screens

- **Recent** — latest 100 episodes across all feeds, pull-to-refresh
- **History** — episodes ordered by last played
- **Downloads** — in-progress downloads with progress bar, completed downloads with play/delete
- **Podcasts** — subscribed feeds, add by RSS URL, pull-to-refresh, OPML import/export
- **Episode List** — episodes for a feed; progress bar shows unplayed / in-progress / finished
- **Player** — persistent mini-player bar (tap to expand), full-screen player with artwork, scrubber, skip controls, and streaming/local indicator

## Downloads

Episodes can be downloaded from any episode list screen. The Downloads tab shows:
- In-progress: progress bar, percentage, cancel button
- Completed: play and delete buttons

Downloaded episodes play from the local file; all others stream. The player shows "Local file" or "Streaming" to indicate which.

## OPML

Import subscriptions from any OPML file via the upload icon on the Podcasts tab. Export via the download icon — shares an `.opml` file through the system share sheet.

## Android Auto

OpenPod uses `MediaLibraryService` (Media3) to integrate with Android Auto. The relevant code is in `player/PlaybackService.kt`.

### How it works

`PlaybackService` extends `MediaLibraryService` and implements `MediaLibrarySession.Callback`. When Android Auto connects, it:

1. Calls `onGetLibraryRoot` to get the browse tree root. OpenPod returns a single root node and triggers a feed refresh in the background.
2. Calls `onGetChildren("root", ...)` to populate the main screen. OpenPod returns the 100 most recent episodes as a flat, playable list.
3. Calls `onSetMediaItems` when the user picks an episode. OpenPod re-resolves the audio URI from the database at this point — Media3 strips URIs during IPC, so this is required for playback to work.

### Audio routing

ExoPlayer is configured with explicit audio attributes (`USAGE_MEDIA`, `AUDIO_CONTENT_TYPE_SPEECH`) and `handleAudioFocus = true`. This is required for the system to route audio through the car's speakers rather than the phone.

### Skip forward 30s

A `CommandButton` using `COMMAND_SEEK_FORWARD` is added to the session's custom layout. ExoPlayer is configured with `setSeekForwardIncrementMs(30_000)`, so the standard seek-forward command skips exactly 30 seconds.

### Desktop Head Unit (local testing)

```bash
# Forward the port
/opt/homebrew/share/android-commandlinetools/platform-tools/adb forward tcp:5277 tcp:5277

# Launch the DHU (keep terminal open — it's interactive)
/opt/homebrew/share/android-commandlinetools/extras/google/auto/desktop-head-unit
```

Enable the head unit server in the Android Auto app on the phone first. Restart the DHU after installing a new build.

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

## License

Apache 2.0 — see [LICENSE](LICENSE).
