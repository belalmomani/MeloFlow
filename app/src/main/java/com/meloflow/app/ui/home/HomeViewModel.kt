package com.meloflow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meloflow.app.domain.model.InteractionType
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.repository.MediaSourceRepository
import com.meloflow.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val items: List<MediaItem>, val isDemo: Boolean) : HomeUiState
    data object Error : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaSourceRepository,
    private val recommendations: RecommendationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var rankingJob: Job? = null

    init {
        load()
    }

    /** Loads candidates once, then keeps the list ranked as preferences/interactions change. */
    fun load() {
        rankingJob?.cancel()
        _state.value = HomeUiState.Loading
        rankingJob = viewModelScope.launch {
            repository.getFeatured().fold(
                onSuccess = { candidates ->
                    recommendations.ranked(candidates).collect { ranked ->
                        _state.value = HomeUiState.Success(ranked, repository.isDemo)
                    }
                },
                onFailure = { _state.value = HomeUiState.Error }
            )
        }
    }

    fun onMoreLikeThis(item: MediaItem) = record(item, InteractionType.MORE_LIKE_THIS)

    fun onNotInterested(item: MediaItem) = record(item, InteractionType.NOT_INTERESTED)

    private fun record(item: MediaItem, type: InteractionType) {
        viewModelScope.launch { recommendations.record(item, type) }
    }
}
