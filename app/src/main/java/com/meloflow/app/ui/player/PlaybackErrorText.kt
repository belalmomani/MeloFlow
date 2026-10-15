package com.meloflow.app.ui.player

import androidx.annotation.StringRes
import com.meloflow.app.R
import com.meloflow.app.player.PlaybackError

@StringRes
fun PlaybackError.messageRes(): Int = when (this) {
    PlaybackError.NO_SOURCE -> R.string.error_no_source
    PlaybackError.NETWORK -> R.string.error_network
    PlaybackError.INVALID_SOURCE -> R.string.error_invalid
    PlaybackError.UNKNOWN -> R.string.error_unknown
}
