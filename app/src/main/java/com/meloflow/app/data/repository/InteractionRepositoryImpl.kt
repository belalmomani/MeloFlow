package com.meloflow.app.data.repository

import com.meloflow.app.data.local.InteractionDao
import com.meloflow.app.data.local.InteractionEntity
import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaLanguage
import com.meloflow.app.domain.repository.InteractionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InteractionRepositoryImpl @Inject constructor(
    private val dao: InteractionDao
) : InteractionRepository {

    override fun observeAll(): Flow<List<Interaction>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun record(interaction: Interaction) {
        dao.insert(interaction.toEntity())
        dao.trimTo(MAX_ROWS)
    }

    override suspend fun clearAll() = dao.clearAll()

    private fun Interaction.toEntity() = InteractionEntity(
        mediaId = mediaId,
        artist = artist,
        genreIds = genreIds.joinToString(","),
        language = language.name,
        type = type.name,
        watchedMs = watchedMs,
        timestampMs = timestampMs
    )

    /** Rows with an unknown enum value (e.g. written by a newer app version) are skipped, not fatal. */
    private fun InteractionEntity.toDomainOrNull(): Interaction? {
        val parsedType = runCatching { InteractionType.valueOf(type) }.getOrNull() ?: return null
        val parsedLanguage = runCatching { MediaLanguage.valueOf(language) }.getOrDefault(MediaLanguage.NONE)
        return Interaction(
            mediaId = mediaId,
            artist = artist,
            genreIds = genreIds.split(",").filter { it.isNotEmpty() },
            language = parsedLanguage,
            type = parsedType,
            watchedMs = watchedMs,
            timestampMs = timestampMs
        )
    }

    private companion object {
        const val MAX_ROWS = 2000
    }
}
