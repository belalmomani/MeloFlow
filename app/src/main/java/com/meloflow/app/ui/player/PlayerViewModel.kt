package com.meloflow.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.player.PlaybackUiState
import com.meloflow.app.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Thin UI facade. Playback lives in the singleton [PlayerManager], so nothing is released in onCleared. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val manager: PlayerManager,
    private val recommendations: RecommendationRepository
) : ViewModel() {

    val state: StateFlow<PlaybackUiState> = manager.state
    val player: Player? get() = manager.player

    fun play(item: MediaItem, queue: List<MediaItem> = listOf(item)) = manager.play(item, queue)
    fun pause() = manager.pause()
    fun next() = manager.next()
    fun previous() = manager.previous()
    fun togglePlayPause() = manager.togglePlayPause()
    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = manager.seekBy(deltaMs)
    fun setVolume(volume: Float) = manager.setVolume(volume)
    fun retry() = manager.retry()
    fun close() = manager.close()

    fun moreLikeThis() = recordCurrent(InteractionType.MORE_LIKE_THIS)
    fun notInterested() = recordCurrent(InteractionType.NOT_INTERESTED)

    private fun recordCurrent(type: InteractionType) {
        val item = state.value.currentItem ?: return
        viewModelScope.launch { recommendations.record(item, type) }
    }
}
