package com.openpod.ui.recent

import androidx.lifecycle.ViewModel
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class RecentEpisodesViewModel @Inject constructor(
    repository: PodcastRepository
) : ViewModel() {
    val episodes: StateFlow<List<EpisodeWithPodcast>> = repository.getRecentEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
