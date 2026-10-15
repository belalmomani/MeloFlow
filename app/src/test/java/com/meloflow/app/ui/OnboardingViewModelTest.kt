package com.meloflow.app.ui

import com.meloflow.app.domain.model.MusicLanguage
import com.meloflow.app.domain.model.UserPreferences
import com.meloflow.app.domain.repository.PreferencesRepository
import com.meloflow.app.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePreferencesRepository(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    val current = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = current
    override suspend fun save(preferences: UserPreferences) { current.value = preferences }
    override suspend fun clear() { current.value = UserPreferences() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun next_stopsAtLastStep_andBackStopsAtZero() {
        val vm = OnboardingViewModel(FakePreferencesRepository())
        repeat(10) { vm.next() }
        assertEquals(OnboardingViewModel.LAST_STEP, vm.state.value.step)
        assertTrue(vm.state.value.isLastStep)
        repeat(10) { vm.back() }
        assertEquals(0, vm.state.value.step)
    }

    @Test
    fun toggleGenre_addsThenRemoves() {
        val vm = OnboardingViewModel(FakePreferencesRepository())
        vm.toggleGenre("oud")
        assertEquals(setOf("oud"), vm.state.value.genreIds)
        vm.toggleGenre("oud")
        assertTrue(vm.state.value.genreIds.isEmpty())
    }

    @Test
    fun addArtist_trimsIgnoresBlankAndDedupes() {
        val vm = OnboardingViewModel(FakePreferencesRepository())
        vm.onArtistInputChange("   "); vm.addArtist()
        vm.onArtistInputChange("  Fairuz "); vm.addArtist()
        vm.onArtistInputChange("fairuz"); vm.addArtist()
        assertEquals(listOf("Fairuz"), vm.state.value.artists)
        assertEquals("", vm.state.value.artistInput)
    }

    @Test
    fun init_prefillsFromSavedPreferences() {
        val repo = FakePreferencesRepository(
            UserPreferences(true, MusicLanguage.FOREIGN, setOf("jazz"), setOf("X"), setOf("work"))
        )
        val s = OnboardingViewModel(repo).state.value
        assertEquals(MusicLanguage.FOREIGN, s.musicLanguage)
        assertEquals(setOf("jazz"), s.genreIds)
        assertEquals(listOf("X"), s.artists)
        assertEquals(setOf("work"), s.contextIds)
    }

    @Test
    fun finish_savesCompletedPreferencesAndCallsBack() {
        val repo = FakePreferencesRepository()
        val vm = OnboardingViewModel(repo)
        vm.setLanguage(MusicLanguage.ARABIC)
        vm.toggleGenre("tarab")
        vm.toggleContext("study")
        var done = false
        vm.finish { done = true }
        val saved = repo.current.value
        assertTrue(done)
        assertTrue(saved.onboardingCompleted)
        assertEquals(MusicLanguage.ARABIC, saved.musicLanguage)
        assertEquals(setOf("tarab"), saved.genreIds)
        assertEquals(setOf("study"), saved.listeningContextIds)
    }
}
