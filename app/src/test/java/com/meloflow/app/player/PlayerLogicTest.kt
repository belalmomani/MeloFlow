package com.meloflow.app.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLogicTest {

    @Test
    fun networkCodes_mapToNetwork() {
        assertEquals(PlaybackError.NETWORK, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(PlaybackError.NETWORK, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
    }

    @Test
    fun badLinkCodes_mapToInvalidSource() {
        assertEquals(PlaybackError.INVALID_SOURCE, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
        assertEquals(PlaybackError.INVALID_SOURCE, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
        assertEquals(PlaybackError.INVALID_SOURCE, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED))
    }

    @Test
    fun otherCodes_mapToUnknown() {
        assertEquals(PlaybackError.UNKNOWN, mapPlaybackErrorCode(PlaybackException.ERROR_CODE_UNSPECIFIED))
    }

    @Test
    fun progress_isClampedAndZeroWithoutDuration() {
        assertEquals(0f, PlaybackUiState(positionMs = 500, durationMs = 0).progress, 0f)
        assertEquals(0.5f, PlaybackUiState(positionMs = 500, durationMs = 1000).progress, 0f)
        assertEquals(1f, PlaybackUiState(positionMs = 5000, durationMs = 1000).progress, 0f)
    }
}
