package com.openpod.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.PodcastDao
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

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null

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
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                scope.launch {
                    val guid = player.currentMediaItem?.mediaId ?: return@launch
                    episodeDao.updateProgress(guid, 0L, true, System.currentTimeMillis())
                }
            }
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
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
                val items = when {
                    parentId == "root" -> podcastDao.getAllOnce().map { podcast ->
                        MediaItem.Builder()
                            .setMediaId("podcast/${podcast.feedUrl}")
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(podcast.title)
                                .setArtworkUri(podcast.artworkUrl?.let { Uri.parse(it) })
                                .build())
                            .build()
                    }
                    parentId.startsWith("podcast/") -> {
                        val feedUrl = parentId.removePrefix("podcast/")
                        episodeDao.getForPodcastOnce(feedUrl).map { ep ->
                            MediaItem.Builder()
                                .setMediaId(ep.guid)
                                .setUri(ep.audioUrl)
                                .setMediaMetadata(MediaMetadata.Builder()
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setTitle(ep.title)
                                    .build())
                                .build()
                        }
                    }
                    else -> emptyList()
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
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
        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(30_000)
            .setSeekBackIncrementMs(30_000)
            .build()
            .also { it.addListener(playerListener) }
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback).build()
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
