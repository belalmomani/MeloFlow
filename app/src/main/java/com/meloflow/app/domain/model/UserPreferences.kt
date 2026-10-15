package com.meloflow.app.domain.model

enum class MusicLanguage { ARABIC, FOREIGN, BOTH }

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val musicLanguage: MusicLanguage = MusicLanguage.BOTH,
    val genreIds: Set<String> = emptySet(),
    val favoriteArtists: Set<String> = emptySet(),
    val listeningContextIds: Set<String> = emptySet()
)
