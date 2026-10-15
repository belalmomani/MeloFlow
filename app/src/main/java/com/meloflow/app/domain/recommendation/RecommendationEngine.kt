package com.meloflow.app.domain.recommendation

import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.UserPreferences

data class RecommendationInput(
    val preferences: UserPreferences,
    val interactions: List<Interaction>,
    /** Recent search queries. TODO(Phase 8): fed from the search history once it exists. */
    val searchQueries: List<String>,
    val nowMs: Long
)

data class RankedItem(val item: MediaItem, val score: Double)

/** Pure ranking logic; no Android, no I/O. Replaceable by any other implementation. */
interface RecommendationEngine {
    fun rank(candidates: List<MediaItem>, input: RecommendationInput): List<RankedItem>
}
