package com.openpod.data.repository

import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.Podcast
import com.openpod.data.db.PodcastDao
import com.openpod.data.network.RssFetcher
import com.openpod.data.network.RssParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val fetcher: RssFetcher,
    private val parser: RssParser
) {
    val podcasts: Flow<List<Podcast>> = podcastDao.getAll()

    fun getEpisodes(feedUrl: String): Flow<List<Episode>> = episodeDao.getForPodcast(feedUrl)

    suspend fun addPodcast(feedUrl: String) {
        val feed = parser.parse(fetcher.fetch(feedUrl))
        podcastDao.insert(Podcast(
            feedUrl = feedUrl,
            title = feed.title,
            description = feed.description,
            artworkUrl = feed.artworkUrl,
            lastRefreshed = System.currentTimeMillis()
        ))
        episodeDao.insertAll(feed.episodes.map { ep ->
            Episode(
                guid = ep.guid,
                podcastFeedUrl = feedUrl,
                title = ep.title,
                description = ep.description,
                audioUrl = ep.audioUrl,
                duration = ep.duration,
                pubDate = ep.pubDate
            )
        })
    }

    suspend fun deletePodcast(podcast: Podcast) = podcastDao.delete(podcast)
}
