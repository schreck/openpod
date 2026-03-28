package com.openpod.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodePlayButtonTest {

    @Test
    fun `playing takes priority over all other states`() {
        assertEquals(EpisodePlayState.PLAYING, episodePlayState(isPlaying = true, isPlayed = false, playPositionMs = 0))
        assertEquals(EpisodePlayState.PLAYING, episodePlayState(isPlaying = true, isPlayed = true, playPositionMs = 0))
        assertEquals(EpisodePlayState.PLAYING, episodePlayState(isPlaying = true, isPlayed = false, playPositionMs = 5000))
        assertEquals(EpisodePlayState.PLAYING, episodePlayState(isPlaying = true, isPlayed = true, playPositionMs = 5000))
    }

    @Test
    fun `played when not playing and isPlayed flag set`() {
        assertEquals(EpisodePlayState.PLAYED, episodePlayState(isPlaying = false, isPlayed = true, playPositionMs = 0))
    }

    @Test
    fun `played takes priority over in-progress position`() {
        assertEquals(EpisodePlayState.PLAYED, episodePlayState(isPlaying = false, isPlayed = true, playPositionMs = 5000))
    }

    @Test
    fun `in-progress when position is non-zero and not played`() {
        assertEquals(EpisodePlayState.IN_PROGRESS, episodePlayState(isPlaying = false, isPlayed = false, playPositionMs = 1))
        assertEquals(EpisodePlayState.IN_PROGRESS, episodePlayState(isPlaying = false, isPlayed = false, playPositionMs = 60_000))
    }

    @Test
    fun `unstarted when not playing, not played, and no position`() {
        assertEquals(EpisodePlayState.UNSTARTED, episodePlayState(isPlaying = false, isPlayed = false, playPositionMs = 0))
    }
}
