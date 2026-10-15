package com.meloflow.app.domain.recommendation

import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.MediaLanguage
import com.meloflow.app.domain.model.MusicLanguage
import javax.inject.Inject
import kotlin.math.pow

/**
 * Deterministic, explainable scoring (no randomness): the same inputs always give the same order,
 * and ties keep the provider's order.
 *
 * Signals: music language, preferred genres, favorite artists, past interactions (likes, "show more",
 * watch duration, skips, repeats) with a 14-day half-life, "not interested" (hides the item and
 * penalizes its genres/artist, never decays), and recent searches.
 */
class RuleBasedRecommendationEngine @Inject constructor() : RecommendationEngine {

    override fun rank(candidates: List<MediaItem>, input: RecommendationInput): List<RankedItem> {
        val hiddenIds = input.interactions
            .filter { it.type == InteractionType.NOT_INTERESTED }
            .map { it.mediaId }
            .toSet()

        val genreAffinity = HashMap<String, Double>()
        val artistAffinity = HashMap<String, Double>()
        val repeatPenalty = HashMap<String, Double>()

        for (interaction in input.interactions) {
            val decay = decay(input.nowMs - interaction.timestampMs)
            val (genreWeight, artistWeight) = weights(interaction, decay)
            interaction.genreIds.forEach { genreAffinity[it] = (genreAffinity[it] ?: 0.0) + genreWeight }
            if (artistWeight != 0.0) {
                val key = artistKey(interaction.artist)
                artistAffinity[key] = (artistAffinity[key] ?: 0.0) + artistWeight
            }
            if (interaction.type == InteractionType.VIEW && interaction.watchedMs >= SKIP_MS) {
                repeatPenalty[interaction.mediaId] =
                    (repeatPenalty[interaction.mediaId] ?: 0.0) + REPEAT_PENALTY * decay
            }
        }

        val queries = input.searchQueries.map { it.trim().lowercase() }.filter { it.length >= 2 }.distinct().take(5)
        val prefs = input.preferences

        return candidates
            .filter { it.id !in hiddenIds }
            .map { item ->
                var score = languageScore(item.language, prefs.musicLanguage)
                score += item.genreIds.count { it in prefs.genreIds } * GENRE_PREFERENCE
                if (prefs.favoriteArtists.any { it.equals(item.artist, ignoreCase = true) }) score += ARTIST_PREFERENCE
                score += item.genreIds.sumOf { (genreAffinity[it] ?: 0.0).coerceIn(-GENRE_CAP, GENRE_CAP) }
                score += (artistAffinity[artistKey(item.artist)] ?: 0.0).coerceIn(-ARTIST_CAP, ARTIST_CAP)
                score -= (repeatPenalty[item.id] ?: 0.0).coerceAtMost(REPEAT_CAP)
                score += searchBoost(item, queries)
                RankedItem(item, score)
            }
            .sortedByDescending { it.score } // stable: ties keep provider order
    }

    private fun weights(i: Interaction, decay: Double): Pair<Double, Double> = when (i.type) {
        InteractionType.LIKE -> 2.0 * decay to 3.0 * decay
        InteractionType.MORE_LIKE_THIS -> 3.0 * decay to 4.0 * decay
        InteractionType.NOT_INTERESTED -> -4.0 to -2.0
        InteractionType.VIEW -> when {
            i.watchedMs >= LONG_VIEW_MS -> 1.0 * decay to 1.5 * decay
            i.watchedMs < SKIP_MS -> -0.5 * decay to 0.0
            else -> 0.5 * decay to 0.5 * decay
        }
    }

    private fun languageScore(item: MediaLanguage, preferred: MusicLanguage): Double = when (preferred) {
        MusicLanguage.BOTH -> 0.0
        MusicLanguage.ARABIC -> when (item) {
            MediaLanguage.ARABIC -> LANGUAGE_MATCH
            MediaLanguage.FOREIGN -> -LANGUAGE_MATCH
            MediaLanguage.NONE -> 0.0
        }
        MusicLanguage.FOREIGN -> when (item) {
            MediaLanguage.FOREIGN -> LANGUAGE_MATCH
            MediaLanguage.ARABIC -> -LANGUAGE_MATCH
            MediaLanguage.NONE -> 0.0
        }
    }

    private fun searchBoost(item: MediaItem, queries: List<String>): Double {
        val matches = queries.count { q ->
            item.title.contains(q, ignoreCase = true) || item.artist.contains(q, ignoreCase = true)
        }
        return minOf(matches, 2) * SEARCH_MATCH
    }

    private fun decay(ageMs: Long): Double = 0.5.pow(ageMs.coerceAtLeast(0).toDouble() / HALF_LIFE_MS)

    private fun artistKey(artist: String) = artist.trim().lowercase()

    private companion object {
        const val HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000
        const val SKIP_MS = 10_000L
        const val LONG_VIEW_MS = 60_000L
        const val LANGUAGE_MATCH = 2.0
        const val GENRE_PREFERENCE = 3.0
        const val ARTIST_PREFERENCE = 4.0
        const val GENRE_CAP = 8.0
        const val ARTIST_CAP = 6.0
        const val REPEAT_PENALTY = 1.5
        const val REPEAT_CAP = 3.0
        const val SEARCH_MATCH = 1.5
    }
}
