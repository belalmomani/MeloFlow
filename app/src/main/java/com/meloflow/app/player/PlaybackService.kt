package com.meloflow.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.meloflow.app.MainActivity
import com.meloflow.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Exposes the app's single player as a MediaSession: media notification with play/pause/next/previous,
 * lock-screen controls, Bluetooth and headset buttons. Media3 promotes this service to the foreground
 * while playing, and handles the Android 14 foreground-service type declared in the manifest.
 * The player itself is owned by [PlayerManager]; this service only wraps it.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playerManager: PlayerManager

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate() // Hilt injects here
        playerManager.onServiceCreated()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, playerManager.ensurePlayer())
            .setId("meloflow_session")
            .setSessionActivity(openApp)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_notification)
            }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Swiping the app away stops the service unless something is actively playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release() // does not release the player; PlayerManager owns it
        mediaSession = null
        playerManager.onServiceDestroyed()
        super.onDestroy()
    }
}
