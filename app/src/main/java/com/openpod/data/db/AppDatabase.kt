package com.openpod.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Podcast::class, Episode::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
}
