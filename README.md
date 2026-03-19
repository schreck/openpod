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
- Background playback with notification controls
- Resume from last position
- Download episodes for offline listening

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

1. **Podcast List** — subscribed feeds, pull-to-refresh
2. **Episode List** — episodes for a feed, play/download actions
3. **Player** — persistent mini-player, expandable to full player with seek, speed control, and 30s skip

## Build

```bash
./gradlew assembleDebug
```
