# OpenPod

A simple, no-frills podcast app for Android.

## Stack

- **UI** — Kotlin + Jetpack Compose
- **Audio** — ExoPlayer via `MediaSessionService`
- **RSS** — OkHttp + `XmlPullParser`
- **Storage** — Room
- **DI** — Hilt
- **Images** — Coil

## Features

- Subscribe to any podcast by RSS feed URL
- Browse and play episodes
- Background playback with notification and lock screen controls
- Resume from last position
- Full-screen player with artwork, scrubber, and skip controls
- Android Auto support

## Architecture

Single-module MVVM app.

```
app/
├── data/
│   ├── db/          # Room entities, DAOs, database
│   ├── network/     # RSS HTTP client + XML parser
│   └── repository/  # PodcastRepository, EpisodeRepository
├── player/          # ExoPlayer service + MediaController wrapper
├── ui/
│   ├── podcasts/    # Podcast list screen
│   ├── episodes/    # Episode list screen
│   └── player/      # Mini-player bar + full-screen player
└── MainActivity.kt
```

## Screens

1. **Podcast List** — subscribed feeds
2. **Episode List** — episodes for a feed, tap to play
3. **Player** — persistent mini-player bar (tap to expand), full-screen player with artwork, scrubber, and skip controls

## Build & Install

Requires Java 11+. If the system JDK is older, prefix with `JAVA_HOME`:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew installDebug
```

Check connected devices first:

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices
```
