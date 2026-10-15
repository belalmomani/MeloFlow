package com.meloflow.app.ui

import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.PlaybackSource
import com.meloflow.app.player.PlaybackError
import com.meloflow.app.player.PlaybackUiState
import com.meloflow.app.ui.player.canEnterPipAutomatically
import com.meloflow.app.ui.player.isPipAvailable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureInPictureTest {

    private val playable = MediaItem(
        id = "1", title = "t", artist = "a", sourceId = "s",
        playbackSource = PlaybackSource.Url("https://x/1.mp4")
    )
    private val unplayable = playable.copy(playbackSource = null)

    @Test
    fun noItem_orNoSource_isNotAvailable() {
        assertFalse(isPipAvailable(PlaybackUiState()))
        assertFalse(isPipAvailable(PlaybackUiState(currentItem = unplayable)))
    }

    @Test
    fun errorState_isNotAvailable() {
        assertFalse(isPipAvailable(PlaybackUiState(currentItem = playable, error = PlaybackError.NETWORK)))
    }

    @Test
    fun playableItem_isAvailable_butAutoEntryNeedsPlaying() {
        val paused = PlaybackUiState(currentItem = playable, playWhenReady = false)
        val playing = paused.copy(playWhenReady = true)
        assertTrue(isPipAvailable(paused))
        assertFalse(canEnterPipAutomatically(paused))
        assertTrue(canEnterPipAutomatically(playing))
    }
}
