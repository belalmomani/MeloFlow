package com.meloflow.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.meloflow.app.R
import com.meloflow.app.player.PlaybackStatus
import com.meloflow.app.player.PlaybackUiState
import com.meloflow.app.ui.components.formatDuration

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    FullscreenEffect(fullscreen)
    BackHandler(enabled = fullscreen) { fullscreen = false }
    val context = LocalContext.current
    val isInPip = rememberIsInPip()
    PipEffect(
        canEnterAutomatically = canEnterPipAutomatically(state),
        isPlaying = state.playWhenReady,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPipDismissed = viewModel::pause
    )

    val item = state.currentItem
    if (item == null) {
        // Player was closed; nothing to show.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val player = viewModel.player

    if (isInPip) {
        // Floating window: video only, system draws the play/pause action.
        VideoSurface(state, player, viewModel::retry, Modifier.fillMaxSize())
    } else if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(state, player, viewModel::retry, Modifier.fillMaxSize())
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PlayerControls(state, viewModel, fullscreen = true, onToggleFullscreen = { fullscreen = false })
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
                }
                Spacer(Modifier.weight(1f))
                if (isPipSupported(context)) {
                    IconButton(
                        onClick = { enterPip(context, state.playWhenReady) },
                        enabled = isPipAvailable(state)
                    ) {
                        Icon(Icons.Filled.PictureInPictureAlt, contentDescription = stringResource(R.string.player_pip))
                    }
                }
            }
            VideoSurface(state, player, viewModel::retry, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    item.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        viewModel.moreLikeThis()
                        Toast.makeText(context, R.string.feedback_more_like_this, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.menu_more_like_this)) }
                    TextButton(onClick = {
                        viewModel.notInterested()
                        Toast.makeText(context, R.string.feedback_not_interested, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.menu_not_interested)) }
                }
                Spacer(Modifier.padding(8.dp))
                PlayerControls(state, viewModel, fullscreen = false, onToggleFullscreen = { fullscreen = true })
                item.licenseInformation?.let { license ->
                    Spacer(Modifier.padding(8.dp))
                    Text(
                        listOfNotNull(license.name, license.attribution).joinToString(" — "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSurface(
    state: PlaybackUiState,
    player: Player?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context -> PlayerView(context).apply { useController = false } },
            update = { it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize()
        )
        val error = state.error
        when {
            error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    stringResource(error.messageRes()),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (error != com.meloflow.app.player.PlaybackError.NO_SOURCE) {
                    Spacer(Modifier.padding(4.dp))
                    Button(onClick = onRetry) { Text(stringResource(R.string.player_retry)) }
                }
            }
            state.status == PlaybackStatus.LOADING -> CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlaybackUiState,
    viewModel: PlayerViewModel,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    val enabled = state.error == null
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Slider(
            value = dragFraction ?: state.progress,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { viewModel.seekTo((it * state.durationMs).toLong()) }
                dragFraction = null
            },
            enabled = enabled && state.durationMs > 0
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            if (state.durationMs > 0) {
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::previous, enabled = enabled) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.player_previous))
            }
            IconButton(onClick = { viewModel.seekBy(-SEEK_STEP_MS) }, enabled = enabled) {
                Icon(Icons.Filled.Replay10, contentDescription = stringResource(R.string.player_rewind))
            }
            val showPause = state.playWhenReady && state.status != PlaybackStatus.ENDED && enabled
            IconButton(onClick = viewModel::togglePlayPause) {
                Icon(
                    if (showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (showPause) R.string.player_pause else R.string.player_play)
                )
            }
            IconButton(onClick = { viewModel.seekBy(SEEK_STEP_MS) }, enabled = enabled) {
                Icon(Icons.Filled.Forward10, contentDescription = stringResource(R.string.player_forward))
            }
            IconButton(onClick = viewModel::next, enabled = enabled && state.hasNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.player_next))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = stringResource(
                        if (fullscreen) R.string.player_exit_fullscreen else R.string.player_fullscreen
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = stringResource(R.string.player_volume))
            Slider(
                value = state.volume,
                onValueChange = viewModel::setVolume,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun FullscreenEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(enabled) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (enabled) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private const val SEEK_STEP_MS = 10_000L
