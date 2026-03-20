package com.openpod

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openpod.player.PlayerController
import com.openpod.ui.episodes.EpisodeListScreen
import com.openpod.ui.player.MiniPlayerBar
import com.openpod.ui.player.PlayerViewModel
import com.openpod.ui.podcasts.PodcastListScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onStart() {
        super.onStart()
        playerController.connect()
    }

    override fun onStop() {
        super.onStop()
        playerController.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val playerViewModel: PlayerViewModel = hiltViewModel()
                val playerState by playerViewModel.state.collectAsStateWithLifecycle()

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(navController, startDestination = "podcasts") {
                            composable("podcasts") {
                                PodcastListScreen(
                                    onPodcastClick = { feedUrl ->
                                        navController.navigate("episodes/${Uri.encode(feedUrl)}")
                                    }
                                )
                            }
                            composable(
                                "episodes/{feedUrl}",
                                arguments = listOf(navArgument("feedUrl") { type = NavType.StringType })
                            ) {
                                EpisodeListScreen(
                                    onBack = { navController.popBackStack() },
                                    onPlayEpisode = { episode -> playerController.playEpisode(episode) }
                                )
                            }
                        }
                    }
                    if (playerState.hasMedia) {
                        MiniPlayerBar(
                            state = playerState,
                            onPlayPause = playerViewModel::playPause,
                            onSeekForward = playerViewModel::seekForward
                        )
                    }
                }
            }
        }
    }
}
