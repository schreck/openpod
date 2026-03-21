package com.openpod.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class EpisodeWithPodcast(
    @Embedded val episode: Episode,
    @ColumnInfo(name = "podcastTitle") val podcastTitle: String,
    @ColumnInfo(name = "podcastArtworkUrl") val podcastArtworkUrl: String?
)

@Dao
interface EpisodeDao {
    @Query("""
        SELECT episodes.*, podcasts.title as podcastTitle, podcasts.artworkUrl as podcastArtworkUrl
        FROM episodes
        JOIN podcasts ON episodes.podcastFeedUrl = podcasts.feedUrl
        ORDER BY episodes.pubDate DESC
        LIMIT 100
    """)
    fun getAllRecent(): Flow<List<EpisodeWithPodcast>>

    @Query("SELECT * FROM episodes WHERE podcastFeedUrl = :feedUrl ORDER BY pubDate DESC")
    fun getForPodcast(feedUrl: String): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE podcastFeedUrl = :feedUrl ORDER BY pubDate DESC")
    suspend fun getForPodcastOnce(feedUrl: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE guid = :guid LIMIT 1")
    suspend fun getByGuid(guid: String): Episode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<Episode>)

    @Query("SELECT playPositionMs FROM episodes WHERE guid = :guid")
    suspend fun getPlayPosition(guid: String): Long

    @Query("UPDATE episodes SET playPositionMs = :position, isPlayed = :isPlayed, lastPlayedAt = :lastPlayedAt WHERE guid = :guid")
    suspend fun updateProgress(guid: String, position: Long, isPlayed: Boolean, lastPlayedAt: Long)

    @Query("UPDATE episodes SET downloadId = :downloadId WHERE guid = :guid")
    suspend fun updateDownloadId(guid: String, downloadId: Long)

    @Query("UPDATE episodes SET localFilePath = :path, downloadId = -1 WHERE guid = :guid")
    suspend fun completeDownload(guid: String, path: String)

    @Query("""
        SELECT episodes.*, podcasts.title as podcastTitle, podcasts.artworkUrl as podcastArtworkUrl
        FROM episodes
        JOIN podcasts ON episodes.podcastFeedUrl = podcasts.feedUrl
        WHERE episodes.downloadId != -1
        ORDER BY episodes.pubDate DESC
    """)
    fun getQueued(): Flow<List<EpisodeWithPodcast>>

    @Query("""
        SELECT episodes.*, podcasts.title as podcastTitle, podcasts.artworkUrl as podcastArtworkUrl
        FROM episodes
        JOIN podcasts ON episodes.podcastFeedUrl = podcasts.feedUrl
        WHERE episodes.localFilePath IS NOT NULL
        ORDER BY episodes.pubDate DESC
    """)
    fun getDownloaded(): Flow<List<EpisodeWithPodcast>>

    @Query("SELECT * FROM episodes WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getByDownloadId(downloadId: Long): Episode?

    @Query("""
        SELECT episodes.*, podcasts.title as podcastTitle, podcasts.artworkUrl as podcastArtworkUrl
        FROM episodes
        JOIN podcasts ON episodes.podcastFeedUrl = podcasts.feedUrl
        WHERE episodes.lastPlayedAt > 0
        ORDER BY episodes.lastPlayedAt DESC
    """)
    fun getPlayHistory(): Flow<List<EpisodeWithPodcast>>
}
