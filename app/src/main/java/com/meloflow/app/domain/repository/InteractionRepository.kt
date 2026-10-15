package com.meloflow.app.domain.repository

import com.meloflow.app.domain.model.Interaction
import kotlinx.coroutines.flow.Flow

interface InteractionRepository {
    /** Newest first. */
    fun observeAll(): Flow<List<Interaction>>
    suspend fun record(interaction: Interaction)
    /** Privacy: wipes everything the recommendation engine learned from behavior. */
    suspend fun clearAll()
}
