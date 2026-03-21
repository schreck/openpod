package com.openpod.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.download.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadQueueViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val queue: StateFlow<List<Pair<EpisodeWithPodcast, Float>>> =
        downloadRepository.getQueueWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(ewp: EpisodeWithPodcast) = downloadRepository.cancel(ewp.episode)
}
