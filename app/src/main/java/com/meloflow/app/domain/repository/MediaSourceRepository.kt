package com.meloflow.app.domain.repository

import com.meloflow.app.domain.model.Artist
import com.meloflow.app.domain.model.MediaItem

/**
 * One content provider. Real providers implement this and respect their own terms.
 * TODO(later): an aggregating implementation that merges several providers.
 */
interface MediaSourceRepository {
    val sourceId: String

    /** True for sample data. The UI must disclose this to the user. */
    val isDemo: Boolean

    suspend fun getFeatured(): Result<List<MediaItem>>
    suspend fun search(query: String): Result<List<MediaItem>>
    suspend fun getById(id: String): Result<MediaItem?>
    suspend fun searchArtists(query: String): Result<List<Artist>>
}
