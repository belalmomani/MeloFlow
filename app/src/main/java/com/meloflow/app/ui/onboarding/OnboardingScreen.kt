package com.meloflow.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloflow.app.R
import com.meloflow.app.domain.model.MusicCatalog
import com.meloflow.app.domain.model.MusicLanguage

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isArabic = LocalConfiguration.current.locales[0].language == "ar"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        LinearProgressIndicator(
            progress = { (state.step + 1f) / (OnboardingViewModel.LAST_STEP + 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (state.step) {
                0 -> LanguageStep(state.musicLanguage, viewModel::setLanguage)
                1 -> GenresStep(state, isArabic, viewModel)
                2 -> ArtistsStep(state, viewModel)
                3 -> ContextsStep(state, isArabic, viewModel)
                else -> SummaryStep(state, isArabic)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.step > 0) {
                TextButton(onClick = viewModel::back) { Text(stringResource(R.string.onb_back)) }
            }
            Spacer(Modifier.weight(1f))
            if (state.isLastStep) {
                Button(onClick = { viewModel.finish(onFinished) }) {
                    Text(stringResource(R.string.onb_start))
                }
            } else {
                TextButton(onClick = viewModel::next) { Text(stringResource(R.string.onb_skip)) }
                Button(onClick = viewModel::next) { Text(stringResource(R.string.onb_next)) }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun LanguageStep(selected: MusicLanguage, onSelect: (MusicLanguage) -> Unit) {
    StepTitle(stringResource(R.string.onb_step1_title))
    val options = listOf(
        MusicLanguage.ARABIC to R.string.lang_arabic,
        MusicLanguage.FOREIGN to R.string.lang_foreign,
        MusicLanguage.BOTH to R.string.lang_both
    )
    options.forEach { (language, labelRes) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected == language,
                    onClick = { onSelect(language) },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected == language, onClick = null)
            Spacer(Modifier.padding(horizontal = 8.dp))
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresStep(state: OnboardingUiState, isArabic: Boolean, vm: OnboardingViewModel) {
    StepTitle(stringResource(R.string.onb_step2_title))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MusicCatalog.genresFor(state.musicLanguage).forEach { genre ->
            FilterChip(
                selected = genre.id in state.genreIds,
                onClick = { vm.toggleGenre(genre.id) },
                label = { Text(if (isArabic) genre.labelAr else genre.labelEn) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistsStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepTitle(stringResource(R.string.onb_step3_title))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.artistInput,
            onValueChange = vm::onArtistInputChange,
            label = { Text(stringResource(R.string.onb_artist_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.padding(horizontal = 4.dp))
        Button(onClick = vm::addArtist, enabled = state.artistInput.isNotBlank()) {
            Text(stringResource(R.string.onb_artist_add))
        }
    }
    Spacer(Modifier.height(8.dp))
    // TODO(Phase 3): replace manual entry with catalog search via the MediaSourceRepository artist provider.
    Text(
        stringResource(R.string.onb_artist_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.artists.forEach { name ->
            InputChip(
                selected = true,
                onClick = { vm.removeArtist(name) },
                label = { Text(name) },
                trailingIcon = {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove))
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextsStep(state: OnboardingUiState, isArabic: Boolean, vm: OnboardingViewModel) {
    StepTitle(stringResource(R.string.onb_step4_title))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MusicCatalog.listeningContexts.forEach { ctx ->
            FilterChip(
                selected = ctx.id in state.contextIds,
                onClick = { vm.toggleContext(ctx.id) },
                label = { Text(if (isArabic) ctx.labelAr else ctx.labelEn) }
            )
        }
    }
}

@Composable
private fun SummaryStep(state: OnboardingUiState, isArabic: Boolean) {
    StepTitle(stringResource(R.string.onb_step5_title))
    val none = stringResource(R.string.onb_summary_none)
    val languageLabel = stringResource(
        when (state.musicLanguage) {
            MusicLanguage.ARABIC -> R.string.lang_arabic
            MusicLanguage.FOREIGN -> R.string.lang_foreign
            MusicLanguage.BOTH -> R.string.lang_both
        }
    )
    val genres = MusicCatalog.genres
        .filter { it.id in state.genreIds }
        .joinToString("، ") { if (isArabic) it.labelAr else it.labelEn }
        .ifEmpty { none }
    val artists = state.artists.joinToString("، ").ifEmpty { none }
    val contexts = MusicCatalog.listeningContexts
        .filter { it.id in state.contextIds }
        .joinToString("، ") { if (isArabic) it.labelAr else it.labelEn }
        .ifEmpty { none }

    SummaryRow(stringResource(R.string.onb_summary_language), languageLabel)
    SummaryRow(stringResource(R.string.onb_summary_genres), genres)
    SummaryRow(stringResource(R.string.onb_summary_artists), artists)
    SummaryRow(stringResource(R.string.onb_summary_contexts), contexts)
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
