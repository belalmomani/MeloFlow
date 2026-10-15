package com.meloflow.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.meloflow.app.domain.model.MusicLanguage
import com.meloflow.app.domain.model.UserPreferences
import com.meloflow.app.domain.repository.PreferencesRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MUSIC_LANGUAGE = stringPreferencesKey("music_language")
        val GENRES = stringSetPreferencesKey("genres")
        val ARTISTS = stringSetPreferencesKey("favorite_artists")
        val CONTEXTS = stringSetPreferencesKey("listening_contexts")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserPreferences(
                onboardingCompleted = p[Keys.ONBOARDING_COMPLETED] ?: false,
                musicLanguage = p[Keys.MUSIC_LANGUAGE]
                    ?.let { runCatching { MusicLanguage.valueOf(it) }.getOrNull() }
                    ?: MusicLanguage.BOTH,
                genreIds = p[Keys.GENRES] ?: emptySet(),
                favoriteArtists = p[Keys.ARTISTS] ?: emptySet(),
                listeningContextIds = p[Keys.CONTEXTS] ?: emptySet()
            )
        }

    override suspend fun save(preferences: UserPreferences) {
        dataStore.edit { p ->
            p[Keys.ONBOARDING_COMPLETED] = preferences.onboardingCompleted
            p[Keys.MUSIC_LANGUAGE] = preferences.musicLanguage.name
            p[Keys.GENRES] = preferences.genreIds
            p[Keys.ARTISTS] = preferences.favoriteArtists
            p[Keys.CONTEXTS] = preferences.listeningContextIds
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
