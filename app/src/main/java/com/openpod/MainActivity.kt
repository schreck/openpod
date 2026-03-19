package com.openpod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                                navController.navigate("episodes/${feedUrl}")
                            }
                        )
                    }
                    composable("episodes/{feedUrl}") {
                        // Step 3
                    }
                }
            }
        }
    }
}
