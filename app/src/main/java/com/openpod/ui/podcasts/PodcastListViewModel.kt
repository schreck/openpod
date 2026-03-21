package com.openpod.ui.podcasts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpod.data.db.Podcast
import com.openpod.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastListViewModel @Inject constructor(
    private val repository: PodcastRepository
) : ViewModel() {

    val podcasts: StateFlow<List<Podcast>> = repository.podcasts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var feedUrlInput by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        isRefreshing = true
        try { repository.refreshAll() } finally { isRefreshing = false }
    }

    fun onFeedUrlChange(url: String) { feedUrlInput = url }

    fun addPodcast() {
        if (feedUrlInput.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repository.addPodcast(feedUrlInput.trim())
                feedUrlInput = ""
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to add podcast"
            } finally {
                isLoading = false
            }
        }
    }

    fun deletePodcast(podcast: Podcast) {
        viewModelScope.launch { repository.deletePodcast(podcast) }
    }

    fun dismissError() { errorMessage = null }
}
