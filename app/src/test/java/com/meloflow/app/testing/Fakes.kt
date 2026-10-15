package com.meloflow.app.testing

import com.meloflow.app.domain.Clock
import com.meloflow.app.domain.model.Interaction
import com.meloflow.app.domain.model.UserPreferences
import com.meloflow.app.domain.repository.InteractionRepository
import com.meloflow.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeClock(var now: Long = 1_000_000_000L) : Clock {
    override fun nowMs() = now
}

class FakeInteractionRepository : InteractionRepository {
    private val items = MutableStateFlow<List<Interaction>>(emptyList())
    override fun observeAll(): Flow<List<Interaction>> = items.map { list -> list.sortedByDescending { it.timestampMs } }
    override suspend fun record(interaction: Interaction) { items.value = items.value + interaction }
    override suspend fun clearAll() { items.value = emptyList() }
}

class FakePreferences(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    val current = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = current
    override suspend fun save(preferences: UserPreferences) { current.value = preferences }
    override suspend fun clear() { current.value = UserPreferences() }
}
