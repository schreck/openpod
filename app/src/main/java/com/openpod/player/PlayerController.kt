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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val title: String? = null,
    val isPlaying: Boolean = false,
    val hasMedia: Boolean = false
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.update { it.copy(
                title = mediaItem?.mediaMetadata?.title?.toString(),
                hasMedia = mediaItem != null
            )}
        }
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
                    hasMedia = c.mediaItemCount > 0
                )}
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    fun playEpisode(episode: Episode) {
        val item = MediaItem.Builder()
            .setUri(episode.audioUrl)
            .setMediaId(episode.guid)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(episode.title).build())
            .build()
        controller?.run {
            setMediaItem(item)
            prepare()
            play()
        }
        _state.update { it.copy(title = episode.title, isPlaying = true, hasMedia = true) }
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekForward() { controller?.seekForward() }
    fun seekBack() { controller?.seekBack() }
}
