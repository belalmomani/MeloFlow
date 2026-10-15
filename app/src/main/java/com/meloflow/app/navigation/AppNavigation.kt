package com.meloflow.app.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meloflow.app.R
import com.meloflow.app.ui.components.PlaceholderScreen
import com.meloflow.app.ui.home.HomeScreen
import com.meloflow.app.ui.library.LibraryScreen
import com.meloflow.app.ui.onboarding.OnboardingScreen
import com.meloflow.app.ui.player.MiniPlayer
import com.meloflow.app.ui.player.PlayerScreen
import com.meloflow.app.ui.player.PlayerViewModel
import com.meloflow.app.ui.settings.SettingsScreen

@Composable
fun AppNavigation(appViewModel: AppViewModel = hiltViewModel()) {
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    val start = startRoute
    if (start == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else {
        MeloFlowNavHost(startRoute = start)
    }
}

@Composable
private fun MeloFlowNavHost(
    startRoute: String,
    playerViewModel: PlayerViewModel = hiltViewModel() // activity-scoped: shared by mini and full player
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val playback by playerViewModel.state.collectAsStateWithLifecycle()

    val currentRoute = destination?.route
    val showBottomBar = bottomTabs.any { tab ->
        destination?.hierarchy?.any { it.route == tab.screen.route } == true
    }
    val showMiniPlayer = playback.currentItem != null &&
        currentRoute != Screen.Player.route &&
        currentRoute != Screen.Onboarding.route

    val openPlayer = {
        navController.navigate(Screen.Player.route) { launchSingleTop = true }
    }

    val context = LocalContext.current
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Playback works either way; only the notification and lock-screen controls depend on it.
        if (!granted) {
            Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
        }
    }
    fun requestNotificationPermissionOnce() {
        if (askedNotificationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        askedNotificationPermission = true
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            Column {
                if (showMiniPlayer) {
                    MiniPlayer(
                        state = playback,
                        applyNavigationBarInsets = !showBottomBar,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::next,
                        onClose = playerViewModel::close,
                        onOpen = { openPlayer() }
                    )
                }
                if (showBottomBar) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            val selected = destination?.hierarchy?.any { it.route == tab.screen.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack() // opened from Settings
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(onItemClick = { item, queue ->
                    requestNotificationPermissionOnce()
                    playerViewModel.play(item, queue)
                    openPlayer()
                })
            }
            composable(Screen.Discover.route) { PlaceholderScreen(stringResource(R.string.nav_discover)) }
            composable(Screen.Search.route) { PlaceholderScreen(stringResource(R.string.nav_search)) }
            composable(Screen.Library.route) {
                LibraryScreen(onOpenDownloads = { navController.navigate(Screen.Downloads.route) })
            }
            composable(Screen.Downloads.route) { PlaceholderScreen(stringResource(R.string.title_downloads)) }
            composable(Screen.Settings.route) {
                SettingsScreen(onEditPreferences = { navController.navigate(Screen.Onboarding.route) })
            }
            composable(Screen.Player.route) {
                PlayerScreen(viewModel = playerViewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
