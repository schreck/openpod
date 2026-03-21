package com.openpod.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.openpod.data.db.Episode
import com.openpod.ui.downloads.DownloadsContent
import com.openpod.ui.history.PlayHistoryContent
import com.openpod.ui.podcasts.PodcastListContent
import com.openpod.ui.recent.RecentEpisodesContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPodcastClick: (String) -> Unit,
    onPlayEpisode: (Episode) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("OpenPod") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Podcasts") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recent") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Downloads") }
                )
            }
            when (selectedTab) {
                0 -> PodcastListContent(onPodcastClick = onPodcastClick)
                1 -> RecentEpisodesContent(onPlayEpisode = onPlayEpisode)
                2 -> PlayHistoryContent(onPlayEpisode = onPlayEpisode)
                3 -> DownloadsContent(onPlayEpisode = onPlayEpisode)
            }
        }
    }
}
