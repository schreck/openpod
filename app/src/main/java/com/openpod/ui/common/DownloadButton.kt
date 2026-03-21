package com.openpod.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.openpod.data.db.Episode

@Composable
fun DownloadButton(episode: Episode, onDownload: () -> Unit, onCancel: () -> Unit) {
    when {
        episode.localFilePath != null -> IconButton(onClick = {}) {
            Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded")
        }
        episode.downloadId != -1L -> IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel download")
        }
        else -> IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Download")
        }
    }
}
