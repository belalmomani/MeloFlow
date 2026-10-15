package com.meloflow.app.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.meloflow.app.R
import com.meloflow.app.player.PlaybackUiState

/*
 * Picture-in-Picture (system floating window). This is separate from the in-app MiniPlayer:
 * PiP shows the video itself above other apps; the mini player is a bar inside this app.
 * TODO: use the real video aspect ratio from the player's VideoSize instead of a fixed 16:9.
 */

private const val ACTION_PIP_TOGGLE = "com.meloflow.app.PIP_TOGGLE"

/** PiP can be offered manually: there is a playable item and no error. */
fun isPipAvailable(state: PlaybackUiState): Boolean =
    state.currentItem?.playbackSource != null && state.error == null

/** PiP is entered automatically (Home gesture) only while the video is actually meant to be playing. */
fun canEnterPipAutomatically(state: PlaybackUiState): Boolean =
    isPipAvailable(state) && state.playWhenReady

fun isPipSupported(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun buildPipParams(context: Context, isPlaying: Boolean, autoEnter: Boolean): PictureInPictureParams {
    val label = context.getString(if (isPlaying) R.string.player_pause else R.string.player_play)
    val toggle = RemoteAction(
        Icon.createWithResource(context, if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play),
        label,
        label,
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_PIP_TOGGLE).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    )
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .setActions(listOf(toggle))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(autoEnter)
    return builder.build()
}

fun enterPip(context: Context, isPlaying: Boolean) {
    val activity = context.findActivity() ?: return
    if (!isPipSupported(activity)) return
    activity.enterPictureInPictureMode(buildPipParams(activity, isPlaying, autoEnter = false))
}

/** True while the activity is shown in the PiP window. */
@Composable
fun rememberIsInPip(): Boolean {
    val activity = LocalContext.current.findActivity() as? ComponentActivity
    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

/**
 * Keeps PiP wired while the player screen is visible:
 * - Android 12+: auto-enters PiP on Home gesture; Android 8-11: enters from onUserLeaveHint.
 * - Play/pause button inside the PiP window.
 * - Closing the PiP window (activity stopped while in PiP) pauses playback.
 * - Leaving the player screen turns auto-enter off again.
 */
@Composable
fun PipEffect(
    canEnterAutomatically: Boolean,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onPipDismissed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? ComponentActivity ?: return
    if (!isPipSupported(context)) return

    val currentToggle by rememberUpdatedState(onTogglePlayPause)
    val currentDismissed by rememberUpdatedState(onPipDismissed)

    LaunchedEffect(canEnterAutomatically, isPlaying) {
        activity.setPictureInPictureParams(buildPipParams(activity, isPlaying, canEnterAutomatically))
    }

    DisposableEffect(activity, canEnterAutomatically, isPlaying) {
        val onLeave = Runnable {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && canEnterAutomatically) {
                activity.enterPictureInPictureMode(buildPipParams(activity, isPlaying, autoEnter = false))
            }
        }
        activity.addOnUserLeaveHintListener(onLeave)
        onDispose { activity.removeOnUserLeaveHintListener(onLeave) }
    }

    DisposableEffect(activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_PIP_TOGGLE) currentToggle()
            }
        }
        ContextCompat.registerReceiver(
            activity, receiver, IntentFilter(ACTION_PIP_TOGGLE), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { activity.unregisterReceiver(receiver) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity.isInPictureInPictureMode) currentDismissed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(activity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
            }
        }
    }
}
