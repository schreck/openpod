package com.openpod.ui.recent

import androidx.lifecycle.ViewModel
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.download.DownloadRepository
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class RecentEpisodesViewModel @Inject constructor(
    repository: PodcastRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    val episodes: StateFlow<List<EpisodeWithPodcast>> = repository.getRecentEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun download(episode: Episode) = downloadRepository.enqueue(episode)
    fun cancelDownload(episode: Episode) = downloadRepository.cancel(episode)
}
