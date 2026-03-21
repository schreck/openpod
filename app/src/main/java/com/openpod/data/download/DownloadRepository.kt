package com.openpod.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.EpisodeWithPodcast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(val status: Int, val fraction: Float)

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeDao: EpisodeDao,
    private val okHttpClient: OkHttpClient
) {
    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(episode: Episode) {
        scope.launch {
            val resolvedUrl = resolveUrl(episode.audioUrl)
            val filename = Uri.parse(resolvedUrl).lastPathSegment ?: "${episode.guid}.audio"
            val request = DownloadManager.Request(Uri.parse(resolvedUrl))
                .setTitle(episode.title)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_PODCASTS, filename)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val downloadId = dm.enqueue(request)
            episodeDao.updateDownloadId(episode.guid, downloadId)
        }
    }

    private suspend fun resolveUrl(url: String): String = withContext(Dispatchers.IO) {
        try {
            val response = okHttpClient.newCall(Request.Builder().url(url).head().build()).execute()
            response.use { it.request.url.toString() }
        } catch (e: Exception) {
            Log.w("DownloadRepo", "Failed to resolve redirects for $url", e)
            url
        }
    }

    fun cancel(episode: Episode) {
        dm.remove(episode.downloadId)
        scope.launch { episodeDao.updateDownloadId(episode.guid, -1L) }
    }

    fun getDownloadProgress(downloadId: Long): DownloadProgress {
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        return cursor.use {
            if (!it.moveToFirst()) return@use DownloadProgress(DownloadManager.STATUS_PENDING, 0f)
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val fraction = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
            DownloadProgress(status, fraction)
        }
    }

    private fun ticker() = flow { while (true) { emit(Unit); delay(1_000) } }

    fun getQueueWithProgress(): Flow<List<Pair<EpisodeWithPodcast, DownloadProgress>>> =
        combine(episodeDao.getQueued(), ticker()) { episodes, _ ->
            episodes.map { ewp -> ewp to getDownloadProgress(ewp.episode.downloadId) }
        }

    fun getDownloaded(): Flow<List<EpisodeWithPodcast>> = episodeDao.getDownloaded()
}
