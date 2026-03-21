package com.openpod.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.PodcastDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val title: String? = null,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasMedia: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.update { it.copy(
                title = mediaItem?.mediaMetadata?.title?.toString(),
                hasMedia = mediaItem != null
            )}
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }
                Player.STATE_READY -> _state.update { it.copy(
                    isBuffering = false,
                    durationMs = c.duration.coerceAtLeast(0),
                    positionMs = c.currentPosition.coerceAtLeast(0)
                )}
                else -> _state.update { it.copy(isBuffering = false) }
            }
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val c = controller
                if (c != null) {
                    _state.update { it.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration.coerceAtLeast(0).takeIf { d -> d > 0 } ?: it.durationMs
                    )}
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture!!.addListener({
            controller = controllerFuture!!.get().also { c ->
                c.addListener(listener)
                _state.update { PlayerState(
                    title = c.mediaMetadata.title?.toString(),
                    isPlaying = c.isPlaying,
                    hasMedia = c.mediaItemCount > 0,
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = c.duration.coerceAtLeast(0)
                )}
                if (c.isPlaying) startPositionUpdates()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        stopPositionUpdates()
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    fun playEpisode(episode: Episode) {
        _state.update { it.copy(title = episode.title, isPlaying = false, isBuffering = true, hasMedia = true) }
        scope.launch {
            val (savedPosition, artworkUrl) = withContext(Dispatchers.IO) {
                val pos = episodeDao.getPlayPosition(episode.guid)
                val artwork = podcastDao.getByFeedUrl(episode.podcastFeedUrl)?.artworkUrl
                pos to artwork
            }
            _state.update { it.copy(artworkUrl = artworkUrl) }
            val item = MediaItem.Builder()
                .setUri(episode.audioUrl)
                .setMediaId(episode.guid)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
                    .build())
                .build()
            controller?.run {
                setMediaItem(item)
                prepare()
                if (savedPosition > 0) seekTo(savedPosition)
                play()
            }
        }
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun seekForward() { controller?.seekForward() }
    fun seekBack() { controller?.seekBack() }
}
