package com.openpod.di

import android.content.Context
import androidx.room.Room
import com.openpod.data.db.AppDatabase
import com.openpod.data.db.EpisodeDao
import com.openpod.data.db.MIGRATION_1_2
import com.openpod.data.db.MIGRATION_2_3
import com.openpod.data.db.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "openpod.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun providePodcastDao(db: AppDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: AppDatabase): EpisodeDao = db.episodeDao()
}
