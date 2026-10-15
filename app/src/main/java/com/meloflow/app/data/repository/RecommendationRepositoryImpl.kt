package com.meloflow.app.data.repository

import com.meloflow.app.domain.Clock
import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.recommendation.RecommendationEngine
import com.meloflow.app.domain.recommendation.RecommendationInput
import com.meloflow.app.domain.repository.InteractionRepository
import com.meloflow.app.domain.repository.PreferencesRepository
import com.meloflow.app.domain.repository.RecommendationRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Local-only: preferences and interactions never leave the device. */
class RecommendationRepositoryImpl @Inject constructor(
    private val preferences: PreferencesRepository,
    private val interactions: InteractionRepository,
    private val engine: RecommendationEngine,
    private val clock: Clock
) : RecommendationRepository {

    override fun ranked(candidates: List<MediaItem>): Flow<List<MediaItem>> =
        combine(preferences.preferences, interactions.observeAll()) { prefs, history ->
            // TODO(Phase 8): pass real search history instead of an empty list.
            engine.rank(candidates, RecommendationInput(prefs, history, emptyList(), clock.nowMs()))
        }.map { ranked -> ranked.map { it.item } }

    override suspend fun record(item: MediaItem, type: InteractionType, watchedMs: Long) {
        interactions.record(
            Interaction(
                mediaId = item.id,
                artist = item.artist,
                genreIds = item.genreIds,
                language = item.language,
                type = type,
                watchedMs = watchedMs,
                timestampMs = clock.nowMs()
            )
        )
    }

    override suspend fun clearHistory() = interactions.clearAll()
}
