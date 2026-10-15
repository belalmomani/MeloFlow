package com.meloflow.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions ORDER BY timestampMs DESC, id DESC")
    fun observeAll(): Flow<List<InteractionEntity>>

    @Insert
    suspend fun insert(entity: InteractionEntity)

    @Query("DELETE FROM interactions")
    suspend fun clearAll()

    /** Keeps only the newest [keep] rows so the table cannot grow without bound. */
    @Query(
        "DELETE FROM interactions WHERE id NOT IN " +
            "(SELECT id FROM interactions ORDER BY timestampMs DESC, id DESC LIMIT :keep)"
    )
    suspend fun trimTo(keep: Int)
}
