package com.meloflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// TODO(Phase 8): add playlists, favorites, watch history, search history entities (bump version + migration).
@Database(entities = [InteractionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interactionDao(): InteractionDao
}
