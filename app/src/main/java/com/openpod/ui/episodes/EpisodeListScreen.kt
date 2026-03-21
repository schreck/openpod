package com.openpod.ui.episodes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openpod.data.db.Episode
import com.openpod.ui.common.DownloadButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.feedUrl) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(episodes, key = { it.guid }) { episode ->
                EpisodeItem(
                    episode = episode,
                    onPlay = { onPlayEpisode(episode) },
                    onDownload = { viewModel.download(episode) },
                    onCancelDownload = { viewModel.cancelDownload(episode) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: Episode,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(formatDate(episode.pubDate), episode.duration)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            DownloadButton(episode = episode, onDownload = onDownload, onCancel = onCancelDownload)
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
        EpisodeProgressBar(episode)
    }
}

@Composable
private fun EpisodeProgressBar(episode: Episode) {
    val progress: Float
    val color: Color
    when {
        episode.isPlayed -> {
            progress = 1f
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }
        episode.playPositionMs > 0 -> {
            val durationMs = parseDurationMs(episode.duration)
            progress = if (durationMs != null && durationMs > 0)
                (episode.playPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            else 0f
            color = MaterialTheme.colorScheme.primary
        }
        else -> {
            progress = 0f
            color = MaterialTheme.colorScheme.primary
        }
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(2.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun formatDate(pubDate: Long): String? {
    if (pubDate == 0L) return null
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(pubDate))
}

