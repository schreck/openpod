package com.openpod.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val description: String,
    val artworkUrl: String?,
    val lastRefreshed: Long = 0L
)
