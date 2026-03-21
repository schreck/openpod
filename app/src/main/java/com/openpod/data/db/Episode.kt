package com.openpod.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [ForeignKey(
        entity = Podcast::class,
        parentColumns = ["feedUrl"],
        childColumns = ["podcastFeedUrl"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("podcastFeedUrl")]
)
data class Episode(
    @PrimaryKey val guid: String,
    val podcastFeedUrl: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val duration: String?,
    val pubDate: Long,
    val playPositionMs: Long = 0L,
    val isPlayed: Boolean = false,
    val lastPlayedAt: Long = 0L,
    val localFilePath: String? = null
)
