package com.openpod.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.data.download.DownloadRepository
import com.openpod.data.download.ProgressMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    // Each entry: episode + progress fraction (null = fully downloaded, not in progress)
    val downloads: StateFlow<List<Pair<EpisodeWithPodcast, Float?>>> =
        combine(downloadRepository.getAllDownloads(), downloadRepository.progress) { episodes: List<EpisodeWithPodcast>, progressMap: ProgressMap ->
            episodes.map { ewp -> ewp to progressMap[ewp.episode.guid] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(episode: Episode) = downloadRepository.cancel(episode)
    fun delete(episode: Episode) = downloadRepository.delete(episode)
}
