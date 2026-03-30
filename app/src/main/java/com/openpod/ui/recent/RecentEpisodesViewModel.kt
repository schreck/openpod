package com.openpod.ui.recent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.download.DownloadRepository
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentEpisodesViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    val episodes: StateFlow<List<EpisodeWithPodcast>> = repository.getRecentEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isRefreshing by mutableStateOf(false)
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            try { repository.refreshAll() } finally { isRefreshing = false }
        }
    }

    fun download(episode: Episode) = downloadRepository.enqueue(episode)
    fun cancelDownload(episode: Episode) = downloadRepository.cancel(episode)
}
