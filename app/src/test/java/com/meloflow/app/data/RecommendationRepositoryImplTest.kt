package com.meloflow.app.data

import com.meloflow.app.data.repository.RecommendationRepositoryImpl
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.recommendation.RuleBasedRecommendationEngine
import com.meloflow.app.testing.FakeClock
import com.meloflow.app.testing.FakeInteractionRepository
import com.meloflow.app.testing.FakePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationRepositoryImplTest {

    private val interactions = FakeInteractionRepository()
    private val repo = RecommendationRepositoryImpl(FakePreferences(), interactions, RuleBasedRecommendationEngine(), FakeClock())

    private fun item(id: String, genre: String) =
        MediaItem(id = id, title = id, artist = "a-$id", genreIds = listOf(genre), sourceId = "s")

    private val a = item("a", "rock")
    private val b = item("b", "jazz")

    @Test
    fun withoutInteractions_keepsProviderOrder() = runTest {
        assertEquals(listOf("a", "b"), repo.ranked(listOf(a, b)).first().map { it.id })
    }

    @Test
    fun moreLikeThis_changesRankingOnNextEmission() = runTest {
        repo.record(b, InteractionType.MORE_LIKE_THIS)
        val c = item("c", "jazz") // shares the genre of the item the user asked more of
        assertEquals(listOf("c", "a"), repo.ranked(listOf(a, c)).first().map { it.id })
    }

    @Test
    fun notInterested_removesItem() = runTest {
        repo.record(a, InteractionType.NOT_INTERESTED)
        assertEquals(listOf("b"), repo.ranked(listOf(a, b)).first().map { it.id })
    }

    @Test
    fun clearHistory_restoresOriginalOrder() = runTest {
        repo.record(a, InteractionType.NOT_INTERESTED)
        repo.clearHistory()
        assertEquals(listOf("a", "b"), repo.ranked(listOf(a, b)).first().map { it.id })
    }
}
