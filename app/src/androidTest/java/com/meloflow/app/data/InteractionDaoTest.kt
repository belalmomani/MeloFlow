package com.meloflow.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.meloflow.app.data.local.AppDatabase
import com.meloflow.app.data.local.InteractionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented (needs a device/emulator): Room cannot run on the plain JVM without Robolectric. */
@RunWith(AndroidJUnit4::class)
class InteractionDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun row(id: String, ts: Long) = InteractionEntity(
        mediaId = id, artist = "a", genreIds = "g1,g2", language = "ARABIC", type = "LIKE", watchedMs = 0, timestampMs = ts
    )

    @Test
    fun insertAndObserve_newestFirst() = runBlocking {
        db.interactionDao().insert(row("old", 1))
        db.interactionDao().insert(row("new", 2))
        assertEquals(listOf("new", "old"), db.interactionDao().observeAll().first().map { it.mediaId })
    }

    @Test
    fun trimTo_keepsNewestRows() = runBlocking {
        (1..5).forEach { db.interactionDao().insert(row("m$it", it.toLong())) }
        db.interactionDao().trimTo(2)
        assertEquals(listOf("m5", "m4"), db.interactionDao().observeAll().first().map { it.mediaId })
    }

    @Test
    fun clearAll_emptiesTable() = runBlocking {
        db.interactionDao().insert(row("x", 1))
        db.interactionDao().clearAll()
        assertEquals(0, db.interactionDao().observeAll().first().size)
    }
}
