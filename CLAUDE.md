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

1. **Foundation** — Gradle setup, Room schema, Hilt wiring, navigation skeleton
2. **RSS + Podcast list** — add feed by URL, fetch + parse + persist, display list
3. **Episode list** — list episodes per feed, mark played
4. **Playback** — ExoPlayer service, mini-player bar, seek/skip 30s
5. **Polish** — download to local storage, playback speed, resume position

## Conventions

- One `ViewModel` per screen
- DAOs return `Flow<List<T>>` for reactive UI; suspend functions for writes
- `Repository` is the only layer that touches both `db` and `network`
- No `LiveData` — use `StateFlow` / `collectAsStateWithLifecycle`
- Prefer `XmlPullParser` over third-party RSS libs to keep deps minimal
