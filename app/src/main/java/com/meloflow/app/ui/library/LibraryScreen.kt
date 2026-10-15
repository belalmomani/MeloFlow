package com.meloflow.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meloflow.app.R

@Composable
fun LibraryScreen(onOpenDownloads: () -> Unit) {
    // TODO(Phase 8): favorites, history, playlists, followed artists, continue watching.
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.nav_library), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.placeholder_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onOpenDownloads) {
            Text(stringResource(R.string.title_downloads))
        }
    }
}
