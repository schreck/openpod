package com.openpod

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openpod.ui.episodes.EpisodeListScreen
import com.openpod.ui.podcasts.PodcastListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
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
                            onPlayEpisode = { /* step 4 */ }
                        )
                    }
                }
            }
        }
    }
}
