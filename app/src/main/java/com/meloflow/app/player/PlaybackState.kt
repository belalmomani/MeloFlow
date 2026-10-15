package com.meloflow.app.player

import com.meloflow.app.domain.model.MediaItem

enum class PlaybackStatus { IDLE, LOADING, READY, ENDED, ERROR }

enum class PlaybackError { NO_SOURCE, NETWORK, INVALID_SOURCE, UNKNOWN }

data class PlaybackUiState(
    val currentItem: MediaItem? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    /** The user's intent (play vs pause); stays true while buffering so the UI shows "pause". */
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val error: PlaybackError? = null,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
