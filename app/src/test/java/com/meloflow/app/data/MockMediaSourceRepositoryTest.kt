package com.meloflow.app.data

import com.meloflow.app.data.repository.MockMediaSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockMediaSourceRepositoryTest {
    private val repo = MockMediaSourceRepository()

    @Test
    fun isFlaggedAsDemo() = assertTrue(repo.isDemo)

    @Test
    fun featured_hasUniqueIds_andEveryItemCarriesSourceId() = runTest {
        val items = repo.getFeatured().getOrThrow()
        assertTrue(items.isNotEmpty())
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertTrue(items.all { it.sourceId == MockMediaSourceRepository.SOURCE_ID })
    }

    @Test
    fun itemsWithoutPlaybackSource_neverAllowDownload() = runTest {
        val noSource = repo.getFeatured().getOrThrow().filter { it.playbackSource == null }
        assertTrue(noSource.isNotEmpty())
        assertTrue(noSource.none { it.isDownloadAllowed })
    }

    @Test
    fun search_matchesTitleOrArtist_caseInsensitive_andBlankIsEmpty() = runTest {
        assertEquals(listOf("demo-sintel"), repo.search("SINTEL").getOrThrow().map { it.id })
        assertTrue(repo.search("blender").getOrThrow().size >= 4)
        assertTrue(repo.search("   ").getOrThrow().isEmpty())
        assertTrue(repo.search("zzz-no-match").getOrThrow().isEmpty())
    }

    @Test
    fun getById_returnsItemOrNull() = runTest {
        assertNotNull(repo.getById("demo-bbb").getOrThrow())
        assertNull(repo.getById("missing").getOrThrow())
    }

    @Test
    fun searchArtists_returnsDistinctMatches() = runTest {
        val artists = repo.searchArtists("blender").getOrThrow()
        assertEquals(1, artists.size)
        assertFalse(repo.searchArtists("").getOrThrow().isNotEmpty())
    }
}
