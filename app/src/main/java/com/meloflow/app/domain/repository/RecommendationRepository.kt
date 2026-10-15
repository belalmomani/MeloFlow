package com.meloflow.app.domain.repository

import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/** The only entry point the UI uses; the engine behind it can be swapped (AI service, ML model, ...). */
interface RecommendationRepository {
    /** Re-emits whenever preferences or interactions change. Never sends data off the device. */
    fun ranked(candidates: List<MediaItem>): Flow<List<MediaItem>>
    suspend fun record(item: MediaItem, type: InteractionType, watchedMs: Long = 0L)
    suspend fun clearHistory()
}
