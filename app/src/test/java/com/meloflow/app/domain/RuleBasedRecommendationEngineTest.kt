package com.meloflow.app.domain

import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.MediaLanguage
import com.meloflow.app.domain.model.MusicLanguage
import com.meloflow.app.domain.model.UserPreferences
import com.meloflow.app.domain.recommendation.RecommendationInput
import com.meloflow.app.domain.recommendation.RuleBasedRecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedRecommendationEngineTest {

    private val engine = RuleBasedRecommendationEngine()
    private val now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun item(
        id: String,
        artist: String = "artist-$id",
        genres: List<String> = emptyList(),
        language: MediaLanguage = MediaLanguage.NONE,
        title: String = "title-$id"
    ) = MediaItem(id = id, title = title, artist = artist, genreIds = genres, language = language, sourceId = "s")

    private fun interaction(
        of: MediaItem,
        type: InteractionType,
        watchedMs: Long = 0,
        ageMs: Long = 0
    ) = Interaction(of.id, of.artist, of.genreIds, of.language, type, watchedMs, now - ageMs)

    private fun rank(
        candidates: List<MediaItem>,
        prefs: UserPreferences = UserPreferences(),
        interactions: List<Interaction> = emptyList(),
        queries: List<String> = emptyList()
    ) = engine.rank(candidates, RecommendationInput(prefs, interactions, queries, now))

    private fun ids(r: List<com.meloflow.app.domain.recommendation.RankedItem>) = r.map { it.item.id }

    @Test
    fun noSignals_keepsProviderOrder() {
        assertEquals(listOf("a", "b", "c"), ids(rank(listOf(item("a"), item("b"), item("c")))))
    }

    @Test
    fun preferredGenre_ranksHigher() {
        val r = rank(listOf(item("a", genres = listOf("rock")), item("b", genres = listOf("jazz"))),
            prefs = UserPreferences(genreIds = setOf("jazz")))
        assertEquals(listOf("b", "a"), ids(r))
    }

    @Test
    fun languagePreference_isRespected() {
        val ar = item("ar", language = MediaLanguage.ARABIC)
        val en = item("en", language = MediaLanguage.FOREIGN)
        assertEquals(listOf("ar", "en"), ids(rank(listOf(en, ar), prefs = UserPreferences(musicLanguage = MusicLanguage.ARABIC))))
        assertEquals(listOf("en", "ar"), ids(rank(listOf(ar, en), prefs = UserPreferences(musicLanguage = MusicLanguage.FOREIGN))))
    }

    @Test
    fun favoriteArtist_isBoosted_caseInsensitive() {
        val r = rank(listOf(item("a", artist = "Someone"), item("b", artist = "FAIRUZ")),
            prefs = UserPreferences(favoriteArtists = setOf("fairuz")))
        assertEquals("b", ids(r).first())
    }

    @Test
    fun moreLikeThis_boostsSimilarGenreAndArtist() {
        val liked = item("liked", artist = "X", genres = listOf("tarab"))
        val similar = item("similar", artist = "X", genres = listOf("tarab"))
        val other = item("other", genres = listOf("rock"))
        val r = rank(listOf(other, similar), interactions = listOf(interaction(liked, InteractionType.MORE_LIKE_THIS)))
        assertEquals(listOf("similar", "other"), ids(r))
    }

    @Test
    fun notInterested_hidesItem_andPenalizesItsGenres() {
        val bad = item("bad", genres = listOf("pop"))
        val samePop = item("samePop", genres = listOf("pop"))
        val neutral = item("neutral", genres = listOf("jazz"))
        val r = rank(listOf(bad, samePop, neutral), interactions = listOf(interaction(bad, InteractionType.NOT_INTERESTED)))
        assertEquals(listOf("neutral", "samePop"), ids(r))
    }

    @Test
    fun notInterested_doesNotDecayOverTime() {
        val bad = item("bad", genres = listOf("pop"))
        val samePop = item("samePop", genres = listOf("pop"))
        val neutral = item("neutral", genres = listOf("jazz"))
        val r = rank(listOf(samePop, neutral), interactions = listOf(interaction(bad, InteractionType.NOT_INTERESTED, ageMs = 365 * day)))
        assertEquals("neutral", ids(r).first())
    }

    @Test
    fun likes_decayWithAge() {
        val liked = item("liked", genres = listOf("g"))
        val cand = listOf(item("c", genres = listOf("g")))
        val recent = rank(cand, interactions = listOf(interaction(liked, InteractionType.LIKE, ageMs = 0))).first().score
        val old = rank(cand, interactions = listOf(interaction(liked, InteractionType.LIKE, ageMs = 60 * day))).first().score
        assertTrue(recent > old && old > 0)
    }

    @Test
    fun longView_boosts_shortSkip_penalizes() {
        val watched = item("w", genres = listOf("g1"))
        val skipped = item("s", genres = listOf("g2"))
        val r = rank(
            listOf(item("c2", genres = listOf("g2")), item("c1", genres = listOf("g1"))),
            interactions = listOf(
                interaction(watched, InteractionType.VIEW, watchedMs = 120_000),
                interaction(skipped, InteractionType.VIEW, watchedMs = 3_000)
            )
        )
        assertEquals(listOf("c1", "c2"), ids(r))
    }

    @Test
    fun recentlyWatchedItem_isDemotedToAvoidRepeats() {
        val a = item("a", artist = "p")
        val b = item("b", artist = "q")
        val r = rank(listOf(a, b), interactions = listOf(interaction(a, InteractionType.VIEW, watchedMs = 30_000)))
        assertEquals("b", ids(r).first())
    }

    @Test
    fun searchQueries_boostMatchingItems() {
        val r = rank(listOf(item("a", title = "Moonlight"), item("b", title = "Sunrise")), queries = listOf("sunr"))
        assertEquals("b", ids(r).first())
    }

    @Test
    fun ranking_isDeterministic() {
        val c = listOf(item("a", genres = listOf("x")), item("b", genres = listOf("y")), item("c"))
        val prefs = UserPreferences(genreIds = setOf("y"))
        assertEquals(ids(rank(c, prefs)), ids(rank(c, prefs)))
    }
}
