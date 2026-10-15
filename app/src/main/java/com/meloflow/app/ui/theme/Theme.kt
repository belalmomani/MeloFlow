package com.meloflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFF00B8D4),
    background = Color(0xFFFBFAFF),
    surface = Color(0xFFFBFAFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    secondary = Color(0xFF5CE1F5),
    background = Color(0xFF121016),
    surface = Color(0xFF121016)
)

@Composable
fun MeloFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // TODO(Phase 2): read theme override from DataStore settings.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
