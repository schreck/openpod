package com.openpod.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class EpisodePlayState { PLAYING, PLAYED, IN_PROGRESS, UNSTARTED }

fun episodePlayState(isPlaying: Boolean, isPlayed: Boolean, playPositionMs: Long): EpisodePlayState = when {
    isPlaying -> EpisodePlayState.PLAYING
    isPlayed -> EpisodePlayState.PLAYED
    playPositionMs > 0 -> EpisodePlayState.IN_PROGRESS
    else -> EpisodePlayState.UNSTARTED
}

@Composable
fun EpisodePlayButton(
    isPlaying: Boolean,
    isPlayed: Boolean,
    playPositionMs: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit
) {
    val state = episodePlayState(isPlaying, isPlayed, playPositionMs)
    IconButton(onClick = if (state == EpisodePlayState.PLAYING) onPause else onPlay) {
        when (state) {
            EpisodePlayState.PLAYING -> Icon(
                Icons.Default.Pause,
                contentDescription = "Pause",
                tint = MaterialTheme.colorScheme.primary
            )
            EpisodePlayState.PLAYED -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Played",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            EpisodePlayState.IN_PROGRESS -> Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = MaterialTheme.colorScheme.primary
            )
            EpisodePlayState.UNSTARTED -> Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
