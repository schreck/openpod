package com.openpod.ui.recent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openpod.data.db.Episode
import com.openpod.data.db.EpisodeWithPodcast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentEpisodesContent(
    onPlayEpisode: (Episode) -> Unit,
    viewModel: RecentEpisodesViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(episodes, key = { it.episode.guid }) { item ->
            RecentEpisodeItem(item = item, onPlay = { onPlayEpisode(item.episode) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecentEpisodeItem(item: EpisodeWithPodcast, onPlay: () -> Unit) {
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
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
        when {
            episode.isPlayed -> LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            episode.playPositionMs > 0 -> {
                val durationMs = parseDurationMs(episode.duration)
                val progress = if (durationMs != null && durationMs > 0)
                    (episode.playPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

private fun formatDate(pubDate: Long): String? {
    if (pubDate == 0L) return null
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(pubDate))
}

private fun parseDurationMs(duration: String?): Long? {
    if (duration.isNullOrBlank()) return null
    val parts = duration.trim().split(":")
    return when (parts.size) {
        3 -> parts[0].toLongOrNull()?.let { h ->
            parts[1].toLongOrNull()?.let { m ->
                parts[2].toLongOrNull()?.let { s -> (h * 3600 + m * 60 + s) * 1000 }
            }
        }
        2 -> parts[0].toLongOrNull()?.let { m ->
            parts[1].toLongOrNull()?.let { s -> (m * 60 + s) * 1000 }
        }
        1 -> parts[0].toLongOrNull()?.let { it * 1000 }
        else -> null
    }
}
