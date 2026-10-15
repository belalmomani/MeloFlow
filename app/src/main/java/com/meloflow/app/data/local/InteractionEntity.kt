package com.meloflow.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactions",
    indices = [Index("mediaId"), Index("timestampMs")]
)
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: String,
    val artist: String,
    /** Comma-joined genre ids (ids never contain commas). */
    val genreIds: String,
    val language: String,
    val type: String,
    val watchedMs: Long,
    val timestampMs: Long
)
