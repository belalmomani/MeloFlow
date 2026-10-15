package com.meloflow.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meloflow.app.R
import com.meloflow.app.player.PlaybackStatus
import com.meloflow.app.player.PlaybackUiState

/**
 * Persistent in-app mini player. Reflects the single shared player session; it never creates a player.
 * [applyNavigationBarInsets] is true only when no bottom NavigationBar is shown below it.
 */
@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    applyNavigationBarInsets: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    val insets = if (applyNavigationBarInsets) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp) {
        Column(insets) {
            if (state.durationMs > 0 && state.error == null) {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = state.error?.let { stringResource(it.messageRes()) } ?: item.artist
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val showPause = state.playWhenReady && state.status != PlaybackStatus.ENDED && state.error == null
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (showPause) R.string.player_pause else R.string.player_play)
                    )
                }
                if (state.hasNext) {
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.player_next))
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.mini_close))
                }
            }
        }
    }
}
