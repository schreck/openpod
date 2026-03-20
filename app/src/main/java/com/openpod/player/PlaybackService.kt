package com.openpod.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.openpod.data.db.EpisodeDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var episodeDao: EpisodeDao

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                saveJob = scope.launch {
                    while (true) {
                        delay(5_000)
                        val guid = player.currentMediaItem?.mediaId ?: continue
                        episodeDao.updateProgress(guid, player.currentPosition, false)
                    }
                }
            } else {
                saveJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                scope.launch {
                    val guid = player.currentMediaItem?.mediaId ?: return@launch
                    episodeDao.updateProgress(guid, 0L, true)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(30_000)
            .setSeekBackIncrementMs(30_000)
            .build()
            .also { it.addListener(listener) }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        saveJob?.cancel()
        mediaSession.release()
        player.removeListener(listener)
        player.release()
        super.onDestroy()
    }
}
