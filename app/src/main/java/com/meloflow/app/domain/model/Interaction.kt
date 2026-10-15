package com.meloflow.app.domain.model

enum class InteractionType {
    /** Plain like (heart). */
    LIKE,
    /** "أعجبني هذا، أرني المزيد" */
    MORE_LIKE_THIS,
    /** "لا تقترح هذا النوع" */
    NOT_INTERESTED,
    /** A watch session; [Interaction.watchedMs] says for how long. */
    VIEW
}

/**
 * A user action with a snapshot of the item's metadata, so recommendations do not depend on the
 * catalog still containing the item. Stored locally only.
 */
data class Interaction(
    val mediaId: String,
    val artist: String,
    val genreIds: List<String>,
    val language: MediaLanguage,
    val type: InteractionType,
    val watchedMs: Long = 0L,
    val timestampMs: Long
)
