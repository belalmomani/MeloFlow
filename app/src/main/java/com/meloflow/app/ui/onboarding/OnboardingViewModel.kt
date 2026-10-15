package com.meloflow.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meloflow.app.domain.model.MusicLanguage
import com.meloflow.app.domain.model.UserPreferences
import com.meloflow.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val musicLanguage: MusicLanguage = MusicLanguage.BOTH,
    val genreIds: Set<String> = emptySet(),
    val artists: List<String> = emptyList(),
    val artistInput: String = "",
    val contextIds: Set<String> = emptySet()
) {
    val isLastStep: Boolean get() = step == OnboardingViewModel.LAST_STEP
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        // Pre-fill with saved values so the same flow works for "edit preferences" from Settings.
        viewModelScope.launch {
            val saved = repository.preferences.first()
            _state.update {
                it.copy(
                    musicLanguage = saved.musicLanguage,
                    genreIds = saved.genreIds,
                    artists = saved.favoriteArtists.toList(),
                    contextIds = saved.listeningContextIds
                )
            }
        }
    }

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun setLanguage(language: MusicLanguage) = _state.update { it.copy(musicLanguage = language) }

    fun toggleGenre(id: String) = _state.update { it.copy(genreIds = it.genreIds.toggle(id)) }

    fun toggleContext(id: String) = _state.update { it.copy(contextIds = it.contextIds.toggle(id)) }

    fun onArtistInputChange(value: String) = _state.update { it.copy(artistInput = value) }

    fun addArtist() = _state.update { s ->
        val name = s.artistInput.trim()
        if (name.isEmpty() || s.artists.any { it.equals(name, ignoreCase = true) }) {
            s.copy(artistInput = "")
        } else {
            s.copy(artists = s.artists + name, artistInput = "")
        }
    }

    fun removeArtist(name: String) = _state.update { it.copy(artists = it.artists - name) }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            repository.save(
                UserPreferences(
                    onboardingCompleted = true,
                    musicLanguage = s.musicLanguage,
                    genreIds = s.genreIds,
                    favoriteArtists = s.artists.toSet(),
                    listeningContextIds = s.contextIds
                )
            )
            onDone()
        }
    }

    private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

    companion object {
        const val LAST_STEP = 4
    }
}
