package com.openpod.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun getAll(): Flow<List<Podcast>>

    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    suspend fun getAllOnce(): List<Podcast>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: Podcast)

    @Delete
    suspend fun delete(podcast: Podcast)
}
