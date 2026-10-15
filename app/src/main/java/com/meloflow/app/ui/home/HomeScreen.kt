package com.meloflow.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloflow.app.R
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.ui.components.MediaCard

@Composable
fun HomeScreen(onItemClick: (MediaItem, List<MediaItem>) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val s = state) {
        HomeUiState.Loading -> CenteredColumn { CircularProgressIndicator() }
        HomeUiState.Error -> CenteredColumn {
            Text(stringResource(R.string.home_error), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = viewModel::load) { Text(stringResource(R.string.home_retry)) }
        }
        is HomeUiState.Success -> if (s.items.isEmpty()) {
            CenteredColumn { Text(stringResource(R.string.home_empty)) }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item { HomeHeader() }
                if (s.isDemo) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                stringResource(R.string.home_demo_banner),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                items(s.items, key = { it.id }) { media ->
                    MediaCard(
                        media,
                        onClick = { onItemClick(media, s.items) },
                        onMoreLikeThis = {
                            viewModel.onMoreLikeThis(media)
                            Toast.makeText(context, R.string.feedback_more_like_this, Toast.LENGTH_SHORT).show()
                        },
                        onNotInterested = {
                            viewModel.onNotInterested(media)
                            Toast.makeText(context, R.string.feedback_not_interested, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painter = painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
    }
}
