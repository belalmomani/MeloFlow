package com.meloflow.app.player

import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.PlaybackSource

data class QueueSelection(val items: List<MediaItem>, val startIndex: Int)

/**
 * Keeps only items with an authorized playback source and finds where [requested] sits in them.
 * If [requested] is not part of the playable queue, the queue collapses to just that item.
 */
fun resolveQueue(requested: MediaItem, queue: List<MediaItem>): QueueSelection {
    val playable = queue.filter { it.playbackSource is PlaybackSource.Url }
    val index = playable.indexOfFirst { it.id == requested.id }
    return if (index >= 0) QueueSelection(playable, index) else QueueSelection(listOf(requested), 0)
}
