package com.openpod.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastFeedUrl = :feedUrl ORDER BY pubDate DESC")
    fun getForPodcast(feedUrl: String): Flow<List<Episode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<Episode>)

    @Query("UPDATE episodes SET playPositionMs = :position, isPlayed = :isPlayed WHERE guid = :guid")
    suspend fun updateProgress(guid: String, position: Long, isPlayed: Boolean)
}
