package com.openpod.ui.episodes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.openpod.data.db.Episode
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PodcastRepository
) : ViewModel() {

    val feedUrl: String = checkNotNull(savedStateHandle["feedUrl"])

    val episodes: StateFlow<List<Episode>> = repository.getEpisodes(feedUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
