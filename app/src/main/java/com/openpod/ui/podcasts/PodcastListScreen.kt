package com.openpod.ui.podcasts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openpod.data.db.Podcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastListContent(
    onPodcastClick: (String) -> Unit,
    viewModel: PodcastListViewModel = hiltViewModel()
) {
    val podcasts by viewModel.podcasts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AddFeedRow(
            url = viewModel.feedUrlInput,
            isLoading = viewModel.isLoading,
            onUrlChange = viewModel::onFeedUrlChange,
            onAdd = viewModel::addPodcast
        )
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { scope.launch { viewModel.refresh() } },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(podcasts, key = { it.feedUrl }) { podcast ->
                    PodcastItem(
                        podcast = podcast,
                        onClick = { onPodcastClick(podcast.feedUrl) },
                        onDelete = { viewModel.deletePodcast(podcast) }
                    )
                }
            }
        }
    }

    viewModel.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun AddFeedRow(
    url: String,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("RSS feed URL") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add podcast")
            }
        }
    }
}

@Composable
private fun PodcastItem(
    podcast: Podcast,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(podcast.title) },
        supportingContent = { Text(podcast.feedUrl) },
        leadingContent = {
            AsyncImage(
                model = podcast.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
