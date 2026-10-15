package com.meloflow.app.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.PlaybackSource
import com.meloflow.app.domain.repository.RecommendationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem as ExoMediaItem

/**
 * Owns the single app-wide ExoPlayer, independent of screen lifecycles.
 * All methods must be called on the main thread (ExoPlayer's application thread).
 *
 * The player is created lazily ([ensurePlayer]). [PlaybackService] wraps this same player in a MediaSession
 * for the notification, lock screen, Bluetooth and headset controls. A MediaController is connected on creation
 * purely to bind/start that service.
 * TODO(Phase 8): persist last item/position (Room) to restore playback.
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recommendations: RecommendationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var exoPlayer: ExoPlayer? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var queue: List<MediaItem> = emptyList()
    private var serviceRunning = false

    // Watch-time tracking for recommendations (local only)
    private var watchItem: MediaItem? = null
    private var watchedMs = 0L
    private var playingSince: Long? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    /** Null until something has been played (and after the player is released). */
    val player: Player? get() = exoPlayer

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val p = exoPlayer ?: return
            val status = when (playbackState) {
                Player.STATE_BUFFERING -> PlaybackStatus.LOADING
                Player.STATE_READY -> PlaybackStatus.READY
                Player.STATE_ENDED -> PlaybackStatus.ENDED
                else -> PlaybackStatus.IDLE
            }
            _state.update {
                if (it.error != null) it
                else it.copy(status = status, durationMs = p.safeDuration(), positionMs = p.currentPosition)
            }
        }

        override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
            val p = exoPlayer ?: return
            val domain = queue.firstOrNull { it.id == mediaItem?.mediaId } ?: return
            if (domain.id != watchItem?.id) beginWatch(domain)
            _state.update {
                it.copy(
                    currentItem = domain,
                    positionMs = 0L,
                    durationMs = p.safeDuration(),
                    error = null,
                    hasNext = p.hasNextMediaItem(),
                    hasPrevious = p.hasPreviousMediaItem()
                )
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _state.update { it.copy(playWhenReady = playWhenReady) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            trackPlayState(isPlaying)
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onPlayerError(error: PlaybackException) {
            stopTicker()
            _state.update {
                it.copy(status = PlaybackStatus.ERROR, error = mapPlaybackErrorCode(error.errorCode), playWhenReady = false)
            }
        }
    }

    /**
     * Plays [item] within [queue] (playable items only are queued). Resumes without restarting when [item]
     * is already the current one. Items with no authorized source show an error instead of playing.
     */
    fun play(item: MediaItem, queue: List<MediaItem> = listOf(item), force: Boolean = false) {
        val current = _state.value
        if (!force && current.currentItem?.id == item.id && current.error == null) {
            exoPlayer?.play()
            return
        }
        if (item.playbackSource !is PlaybackSource.Url) {
            flushWatch()
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            stopTicker()
            this.queue = emptyList()
            _state.value = PlaybackUiState(
                currentItem = item,
                status = PlaybackStatus.ERROR,
                error = PlaybackError.NO_SOURCE
            )
            return
        }
        val selection = resolveQueue(item, queue)
        this.queue = selection.items
        val exo = ensurePlayer()
        beginWatch(item)
        _state.value = PlaybackUiState(
            currentItem = item,
            status = PlaybackStatus.LOADING,
            playWhenReady = true,
            volume = exo.volume,
            hasNext = selection.startIndex < selection.items.lastIndex,
            hasPrevious = selection.startIndex > 0
        )
        exo.setMediaItems(selection.items.map(::buildExoItem), selection.startIndex, C.TIME_UNSET)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun retry() {
        _state.value.currentItem?.let { play(it, queue.ifEmpty { listOf(it) }, force = true) }
    }

    fun togglePlayPause() {
        val s = _state.value
        val exo = exoPlayer
        when {
            s.error != null -> retry()
            exo == null -> Unit
            s.status == PlaybackStatus.ENDED -> { exo.seekTo(0); exo.play() }
            exo.playWhenReady -> exo.pause()
            else -> exo.play()
        }
    }

    fun pause() { exoPlayer?.pause() }

    fun next() { exoPlayer?.seekToNext() }

    fun previous() { exoPlayer?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            it.seekTo(positionMs.coerceAtLeast(0))
            publishPosition()
        }
    }

    fun seekBy(deltaMs: Long) {
        val exo = exoPlayer ?: return
        val duration = exo.safeDuration()
        val target = exo.currentPosition + deltaMs
        seekTo(if (duration > 0) target.coerceIn(0, duration) else target)
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = v
        _state.update { it.copy(volume = v) }
    }

    /** Stops playback, drops the notification/service and releases the player (now, or when the service is gone). */
    fun close() {
        flushWatch()
        stopTicker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        queue = emptyList()
        _state.value = PlaybackUiState()
        context.stopService(Intent(context, PlaybackService::class.java))
        if (!serviceRunning) releasePlayer()
    }

    /** Used by [PlaybackService] to wrap the shared player; also creates it if the system restarts the service. */
    fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val exo = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exo.addListener(listener)
        exoPlayer = exo
        connectController()
        return exo
    }

    fun onServiceCreated() { serviceRunning = true }

    fun onServiceDestroyed() {
        serviceRunning = false
        if (_state.value.currentItem == null) releasePlayer()
    }

    private fun connectController() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
    }

    private fun releasePlayer() {
        exoPlayer?.removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun buildExoItem(item: MediaItem): ExoMediaItem {
        val source = item.playbackSource as PlaybackSource.Url
        return ExoMediaItem.Builder()
            .setMediaId(item.id)
            .setUri(source.uri)
            .apply { source.mimeType?.let { setMimeType(it) } }
            .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist).build())
            .build()
    }

    private fun trackPlayState(isPlaying: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (isPlaying) {
            if (playingSince == null) playingSince = now
        } else {
            playingSince?.let { watchedMs += now - it }
            playingSince = null
        }
    }

    private fun beginWatch(item: MediaItem) {
        flushWatch()
        watchItem = item
    }

    /** Records how long the previous item was actually watched; ignores accidental sub-2s plays. */
    private fun flushWatch() {
        trackPlayState(false)
        val item = watchItem
        val ms = watchedMs
        watchItem = null
        watchedMs = 0L
        if (item != null && ms >= MIN_WATCH_RECORD_MS) {
            scope.launch { recommendations.record(item, InteractionType.VIEW, ms) }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                publishPosition()
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        publishPosition()
    }

    private fun publishPosition() {
        val exo = exoPlayer ?: return
        _state.update { it.copy(positionMs = exo.currentPosition, durationMs = exo.safeDuration()) }
    }

    private fun Player.safeDuration(): Long = duration.takeIf { it != C.TIME_UNSET } ?: 0L

    private companion object {
        const val POSITION_POLL_MS = 500L
        const val MIN_WATCH_RECORD_MS = 2_000L
    }
}

/** Pure mapping so it can be unit-tested without a device. */
fun mapPlaybackErrorCode(errorCode: Int): PlaybackError = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackError.NETWORK

    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> PlaybackError.INVALID_SOURCE

    else -> PlaybackError.UNKNOWN
}
