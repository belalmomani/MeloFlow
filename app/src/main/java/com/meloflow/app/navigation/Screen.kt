package com.meloflow.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.meloflow.app.R

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Discover : Screen("discover")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Downloads : Screen("downloads")
    data object Settings : Screen("settings")
    data object Player : Screen("player")
}

data class BottomTab(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

/**
 * Downloads is intentionally not a tab: a 6-item bottom bar is cramped (Material recommends 3-5).
 * It is reached from Library.
 */
val bottomTabs: List<BottomTab> = listOf(
    BottomTab(Screen.Home, R.string.nav_home, Icons.Filled.Home),
    BottomTab(Screen.Discover, R.string.nav_discover, Icons.Filled.Explore),
    BottomTab(Screen.Search, R.string.nav_search, Icons.Filled.Search),
    BottomTab(Screen.Library, R.string.nav_library, Icons.Filled.VideoLibrary),
    BottomTab(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings)
)
