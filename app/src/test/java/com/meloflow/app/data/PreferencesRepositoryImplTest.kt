package com.meloflow.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.meloflow.app.data.repository.PreferencesRepositoryImpl
import com.meloflow.app.domain.model.MusicLanguage
import com.meloflow.app.domain.model.UserPreferences
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newRepository() = PreferencesRepositoryImpl(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "test.preferences_pb")
        }
    )

    @Test
    fun defaults_whenNothingSaved() = runTest {
        val prefs = newRepository().preferences.first()
        assertFalse(prefs.onboardingCompleted)
        assertEquals(MusicLanguage.BOTH, prefs.musicLanguage)
        assertTrue(prefs.genreIds.isEmpty())
    }

    @Test
    fun saveThenRead_roundTrips() = runTest {
        val repo = newRepository()
        val saved = UserPreferences(
            onboardingCompleted = true,
            musicLanguage = MusicLanguage.ARABIC,
            genreIds = setOf("oud", "tarab"),
            favoriteArtists = setOf("Artist A"),
            listeningContextIds = setOf("driving")
        )
        repo.save(saved)
        assertEquals(saved, repo.preferences.first())
    }

    @Test
    fun clear_resetsToDefaults() = runTest {
        val repo = newRepository()
        repo.save(UserPreferences(onboardingCompleted = true, genreIds = setOf("pop")))
        repo.clear()
        assertEquals(UserPreferences(), repo.preferences.first())
    }
}
