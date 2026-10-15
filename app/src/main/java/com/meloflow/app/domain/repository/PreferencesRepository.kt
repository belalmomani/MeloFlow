package com.meloflow.app.domain.repository

import com.meloflow.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun save(preferences: UserPreferences)
    suspend fun clear()
}
