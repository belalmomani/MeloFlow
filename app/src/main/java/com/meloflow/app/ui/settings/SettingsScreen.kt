package com.meloflow.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meloflow.app.R

@Composable
fun SettingsScreen(onEditPreferences: () -> Unit) {
    // TODO(Phase 7/8): theme, clear watch history, clear search history, privacy options.
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.settings_edit_prefs),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditPreferences)
                .padding(vertical = 16.dp)
        )
    }
}
