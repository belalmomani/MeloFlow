package com.meloflow.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meloflow.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(
    repository: PreferencesRepository
) : ViewModel() {

    /** Resolved once at launch (null while loading) so later preference changes never reset the NavHost. */
    val startRoute: StateFlow<String?> = flow {
        val completed = repository.preferences.first().onboardingCompleted
        emit(if (completed) Screen.Home.route else Screen.Onboarding.route)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
