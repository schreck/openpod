package com.openpod.player

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.db.PodcastDao
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var episodeDao: EpisodeDao
    @Inject lateinit var podcastDao: PodcastDao
    @Inject lateinit var podcastRepository: PodcastRepository

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null
    private var currentIsLocal = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                saveJob = scope.launch {
                    while (true) {
                        delay(5_000)
                        val guid = player.currentMediaItem?.mediaId ?: continue
                        episodeDao.updateProgress(guid, player.currentPosition, false, System.currentTimeMillis())
                    }
                }
            } else {
                saveJob?.cancel()
                // Save immediately on pause/stop so the position isn't lost
                // between the last periodic save and when playback stopped.
                val guid = player.currentMediaItem?.mediaId ?: return
                scope.launch {
                    episodeDao.updateProgress(guid, player.currentPosition, false, System.currentTimeMillis())
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
            val guid = mediaItem?.mediaId ?: return
            scope.launch {
                val ep = episodeDao.getByGuid(guid)
                currentIsLocal = ep?.localFilePath != null
                val savedPos = ep?.playPositionMs ?: 0L
                if (savedPos > 0 && player.currentPosition < 1_000L) {
                    player.seekTo(savedPos)
                }
                updateSubtitle(if (currentIsLocal) "Local file" else "Streaming")
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> if (!currentIsLocal) updateSubtitle("Buffering…")
                Player.STATE_READY -> updateSubtitle(if (currentIsLocal) "Local file" else "Streaming")
                Player.STATE_ENDED -> scope.launch {
                    val guid = player.currentMediaItem?.mediaId ?: return@launch
                    episodeDao.updateProgress(guid, 0L, true, System.currentTimeMillis())
                }
            }
        }
    }

    private fun updateSubtitle(subtitle: String) {
        val item = player.currentMediaItem ?: return
        if (item.mediaMetadata.subtitle?.toString() == subtitle) return
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            item.buildUpon()
                .setMediaMetadata(item.mediaMetadata.buildUpon().setSubtitle(subtitle).build())
                .build()
        )
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            scope.launch {
                podcastRepository.refreshAll()
                mediaLibrarySession.notifyChildrenChanged("recent", Int.MAX_VALUE, null)
                mediaLibrarySession.notifyChildrenChanged("downloads", Int.MAX_VALUE, null)
            }
            val root = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle("OpenPod")
                    .build())
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            scope.launch {
                val items = when (parentId) {
                    "root" -> listOf(
                        MediaItem.Builder()
                            .setMediaId("recent")
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle("Recent")
                                .build())
                            .build(),
                        MediaItem.Builder()
                            .setMediaId("downloads")
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle("Downloads")
                                .build())
                            .build()
                    )
                    "recent" -> episodeDao.getAllRecentOnce().map { ewp -> ewp.toMediaItem() }
                    "downloads" -> episodeDao.getCompletedDownloadsOnce().map { ewp -> ewp.toMediaItem() }
                    else -> emptyList()
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaItemsWithStartPosition>()
            scope.launch {
                val resolved = mediaItems.map { item ->
                    val ep = episodeDao.getByGuid(item.mediaId)
                    if (ep != null) {
                        MediaItem.Builder()
                            .setMediaId(ep.guid)
                            .setUri(ep.localFilePath ?: ep.audioUrl)
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setTitle(ep.title)
                                .build())
                            .build()
                    } else item
                }
                future.set(MediaItemsWithStartPosition(resolved, startIndex, startPositionMs))
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            scope.launch {
                val ep = episodeDao.getByGuid(mediaId)
                if (ep != null) {
                    future.set(LibraryResult.ofItem(
                        MediaItem.Builder()
                            .setMediaId(ep.guid)
                            .setUri(ep.audioUrl)
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setTitle(ep.title)
                                .build())
                            .build(),
                        null
                    ))
                } else {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(30_000)
            .setSeekBackIncrementMs(30_000)
            .build()
            .also { it.addListener(playerListener) }
        val seekingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

            override fun isCommandAvailable(command: Int) =
                command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                super.isCommandAvailable(command)

            override fun seekToNextMediaItem() = seekTo(currentPosition + seekForwardIncrement)
            override fun seekToNext() = seekTo(currentPosition + seekForwardIncrement)
            override fun seekToPreviousMediaItem() = seekTo(maxOf(0L, currentPosition - seekBackIncrement))
            override fun seekToPrevious() = seekTo(maxOf(0L, currentPosition - seekBackIncrement))
        }
        mediaLibrarySession = MediaLibrarySession.Builder(this, seekingPlayer, libraryCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    override fun onDestroy() {
        saveJob?.cancel()
        mediaLibrarySession.release()
        player.removeListener(playerListener)
        player.release()
        super.onDestroy()
    }
}

private fun EpisodeWithPodcast.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(episode.guid)
        .setUri(episode.localFilePath ?: episode.audioUrl)
        .setMediaMetadata(MediaMetadata.Builder()
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setTitle(episode.title)
            .setArtist(podcastTitle)
            .build())
        .build()
