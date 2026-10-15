package com.meloflow.app.ui

import com.meloflow.app.domain.model.Artist
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.repository.MediaSourceRepository
import com.meloflow.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.meloflow.app.ui.components.formatDuration
import com.meloflow.app.ui.home.HomeUiState
import com.meloflow.app.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeMediaSource(var result: Result<List<MediaItem>>, override val isDemo: Boolean = false) :
    MediaSourceRepository {
    override val sourceId = "fake"
    override suspend fun getFeatured() = result
    override suspend fun search(query: String) = Result.success(emptyList<MediaItem>())
    override suspend fun getById(id: String) = Result.success<MediaItem?>(null)
    override suspend fun searchArtists(query: String) = Result.success(emptyList<Artist>())
}

private class FakeRecommendations : RecommendationRepository {
    val recorded = mutableListOf<Pair<String, InteractionType>>()
    override fun ranked(candidates: List<MediaItem>): Flow<List<MediaItem>> = flowOf(candidates)
    override suspend fun record(item: MediaItem, type: InteractionType, watchedMs: Long) {
        recorded += item.id to type
    }
    override suspend fun clearHistory() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String) = MediaItem(id = id, title = id, artist = "a", sourceId = "fake")

    @Test
    fun success_exposesItemsAndDemoFlag() {
        val vm = HomeViewModel(FakeMediaSource(Result.success(listOf(item("1"))), isDemo = true), FakeRecommendations())
        val s = vm.state.value as HomeUiState.Success
        assertEquals(1, s.items.size)
        assertTrue(s.isDemo)
    }

    @Test
    fun failure_showsError_thenRetryRecovers() {
        val source = FakeMediaSource(Result.failure(RuntimeException("offline")))
        val vm = HomeViewModel(source, FakeRecommendations())
        assertEquals(HomeUiState.Error, vm.state.value)
        source.result = Result.success(listOf(item("1")))
        vm.load()
        assertTrue(vm.state.value is HomeUiState.Success)
    }

    @Test
    fun menuActions_recordInteractions() {
        val recs = FakeRecommendations()
        val vm = HomeViewModel(FakeMediaSource(Result.success(listOf(item("1")))), recs)
        vm.onMoreLikeThis(item("1"))
        vm.onNotInterested(item("1"))
        assertEquals(
            listOf("1" to InteractionType.MORE_LIKE_THIS, "1" to InteractionType.NOT_INTERESTED),
            recs.recorded
        )
    }

    @Test
    fun formatDuration_handlesMinutesAndHours() {
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("10:00", formatDuration(600_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
    }
}
