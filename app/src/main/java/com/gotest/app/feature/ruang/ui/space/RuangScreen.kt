package com.gotest.app.feature.ruang.ui.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.ui.components.RuangScaffold

@Composable
fun RuangRoute(
    onBack: () -> Unit,
    onOpenExams: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuangViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RuangScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBack = onBack,
        onOpenExams = { onOpenExams(viewModel.code) },
        modifier = modifier,
    )
}

@Composable
fun RuangScreen(
    uiState: RuangUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onOpenExams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    RuangScaffold(
        title = page?.organization?.name ?: stringResource(R.string.ruang_kicker),
        subtitle = stringResource(R.string.ruang_kicker).takeIf { page != null },
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize().padding(padding))
            }

            else -> {
                RuangContent(
                    page = page,
                    hasExams = uiState.hasExams,
                    onOpenExams = onOpenExams,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun RuangContent(
    page: RuangPage,
    hasExams: Boolean,
    onOpenExams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val name = page.organization.name
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text =
                if (page.former) {
                    stringResource(R.string.ruang_lede_former, name)
                } else {
                    stringResource(R.string.ruang_lede)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        if (hasExams) {
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.ruang_exams_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text =
                        if (page.former) {
                            stringResource(R.string.ruang_exams_text_former, name)
                        } else {
                            stringResource(R.string.ruang_exams_text, name)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
                GotestButton(
                    text = stringResource(R.string.ruang_exams_open),
                    onClick = onOpenExams,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenPreview() {
    GotestTheme {
        RuangScreen(
            uiState =
                RuangUiState(
                    isLoading = false,
                    page = RuangPage(MitraRef("SMP 1 Bantargedung", "SMP1BTG")),
                    hasExams = true,
                ),
            onRetry = {},
            onBack = {},
            onOpenExams = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenFormerPreview() {
    GotestTheme {
        RuangScreen(
            uiState =
                RuangUiState(
                    isLoading = false,
                    page = RuangPage(MitraRef("Bimbel Cerdas", "BCERDAS"), former = true),
                    hasExams = true,
                ),
            onRetry = {},
            onBack = {},
            onOpenExams = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenLoadingPreview() {
    GotestTheme {
        RuangScreen(uiState = RuangUiState(), onRetry = {}, onBack = {}, onOpenExams = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenErrorPreview() {
    GotestTheme {
        RuangScreen(
            uiState = RuangUiState(isLoading = false, loadFailed = true),
            onRetry = {},
            onBack = {},
            onOpenExams = {},
        )
    }
}
