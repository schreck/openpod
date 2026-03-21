package com.openpod.data.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.EpisodeWithPodcast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Fraction 0..1 for active downloads, keyed by episode guid.
// Episodes not in this map but with downloadId != -1 are queued (waiting to start).
typealias ProgressMap = Map<String, Float>

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: EpisodeDao,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _progress = MutableStateFlow<ProgressMap>(emptyMap())
    val progress = _progress.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()

    fun enqueue(episode: Episode) {
        if (jobs.containsKey(episode.guid)) return
        val job = scope.launch {
            episodeDao.updateDownloadId(episode.guid, 1L) // mark as queued
            try {
                download(episode)
            } catch (e: Exception) {
                episodeDao.updateDownloadId(episode.guid, -1L) // reset on failure
            } finally {
                _progress.update { it - episode.guid }
                jobs.remove(episode.guid)
            }
        }
        jobs[episode.guid] = job
    }

    private suspend fun download(episode: Episode) {
        val response = okHttpClient.newCall(
            Request.Builder().url(episode.audioUrl).build()
        ).execute()

        val body = response.body ?: error("Empty response body")
        val totalBytes = body.contentLength()
        val filename = Uri.parse(episode.audioUrl).lastPathSegment
            ?: "${episode.guid}.audio"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
            ?: context.filesDir
        val file = File(dir, filename)

        var downloadedBytes = 0L
        body.byteStream().use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8_192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        _progress.update {
                            it + (episode.guid to (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }

        episodeDao.completeDownload(episode.guid, file.absolutePath)
    }

    fun cancel(episode: Episode) {
        jobs[episode.guid]?.cancel()
        jobs.remove(episode.guid)
        scope.launch {
            episodeDao.updateDownloadId(episode.guid, -1L)
            _progress.update { it - episode.guid }
        }
    }

    fun getQueueWithProgress(): Flow<List<Pair<EpisodeWithPodcast, Float>>> =
        combine(episodeDao.getQueued(), _progress) { episodes, progressMap ->
            episodes.map { ewp -> ewp to (progressMap[ewp.episode.guid] ?: 0f) }
        }

    fun delete(episode: Episode) {
        scope.launch {
            episode.localFilePath?.let { File(it).delete() }
            episodeDao.clearDownload(episode.guid)
        }
    }

    fun getAllDownloads(): Flow<List<EpisodeWithPodcast>> = episodeDao.getAllDownloads()
}
