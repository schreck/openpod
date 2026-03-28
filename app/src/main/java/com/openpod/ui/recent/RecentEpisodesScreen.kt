package com.openpod.ui.recent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeWithPodcast
import com.openpod.ui.common.DownloadButton
import com.openpod.ui.common.EpisodePlayButton
import com.openpod.ui.episodes.parseDurationMs
import com.openpod.ui.player.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentEpisodesContent(
    onPlayEpisode: (Episode) -> Unit,
    viewModel: RecentEpisodesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(episodes, key = { it.episode.guid }) { item ->
                RecentEpisodeItem(
                    item = item,
                    isPlaying = playerState.currentGuid == item.episode.guid && playerState.isPlaying,
                    onPlay = { onPlayEpisode(item.episode) },
                    onPause = { playerViewModel.playPause() },
                    onDownload = { viewModel.download(item.episode) },
                    onCancelDownload = { viewModel.cancelDownload(item.episode) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecentEpisodeItem(
    item: EpisodeWithPodcast,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val episode = item.episode
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.podcastArtworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(item.podcastTitle, formatDate(episode.pubDate))
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            DownloadButton(episode = episode, onDownload = onDownload, onCancel = onCancelDownload)
            EpisodePlayButton(
                isPlaying = isPlaying,
                isPlayed = episode.isPlayed,
                playPositionMs = episode.playPositionMs,
                onPlay = onPlay,
                onPause = onPause
            )
        }
        val progress: Float
        val progressColor: Color
        when {
            episode.isPlayed -> {
                progress = 1f
                progressColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
            episode.playPositionMs > 0 -> {
                val durationMs = parseDurationMs(episode.duration)
                progress = if (durationMs != null && durationMs > 0)
                    (episode.playPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                else 0f
                progressColor = MaterialTheme.colorScheme.primary
            }
            else -> {
                progress = 0f
                progressColor = MaterialTheme.colorScheme.primary
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatDate(pubDate: Long): String? {
    if (pubDate == 0L) return null
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(pubDate))
}

