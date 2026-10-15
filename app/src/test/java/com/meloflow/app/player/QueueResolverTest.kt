package com.meloflow.app.player

import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueResolverTest {

    private fun playable(id: String) =
        MediaItem(id = id, title = id, artist = "a", sourceId = "s", playbackSource = PlaybackSource.Url("https://x/$id.mp4"))

    private fun unplayable(id: String) = MediaItem(id = id, title = id, artist = "a", sourceId = "s")

    @Test
    fun dropsUnplayableItems_andKeepsOrder() {
        val sel = resolveQueue(playable("b"), listOf(playable("a"), unplayable("x"), playable("b"), playable("c")))
        assertEquals(listOf("a", "b", "c"), sel.items.map { it.id })
        assertEquals(1, sel.startIndex)
    }

    @Test
    fun startIndexPointsAtRequestedItem() {
        val sel = resolveQueue(playable("c"), listOf(playable("a"), playable("b"), playable("c")))
        assertEquals(2, sel.startIndex)
    }

    @Test
    fun requestedNotInQueue_collapsesToSingleItem() {
        val sel = resolveQueue(playable("z"), listOf(playable("a")))
        assertEquals(listOf("z"), sel.items.map { it.id })
        assertEquals(0, sel.startIndex)
    }

    @Test
    fun emptyQueue_collapsesToSingleItem() {
        val sel = resolveQueue(playable("a"), emptyList())
        assertEquals(1, sel.items.size)
    }
}
