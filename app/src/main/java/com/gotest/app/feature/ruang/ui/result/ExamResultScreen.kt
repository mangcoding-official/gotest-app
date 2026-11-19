package com.gotest.app.feature.ruang.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.data.parseServerTime
import com.gotest.app.feature.ruang.domain.ExamResultItem
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.components.formatGrade
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExamResultRoute(
    onBackToList: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExamResultScreen(
        uiState = uiState,
        onRetry = viewModel::load,
        onBackToList = { onBackToList(viewModel.code) },
        modifier = modifier,
    )
}

@Composable
fun ExamResultScreen(
    uiState: ExamResultUiState,
    onRetry: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    RuangScaffold(
        title = page?.title ?: stringResource(R.string.exam_list_title),
        subtitle = page?.let { stringResource(R.string.exam_detail_back, it.organization.name) },
        onBack = onBackToList,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = uiState.errorMessage ?: stringResource(R.string.state_error_message),
                )
            }

            else -> {
                ResultContent(page = page, onBackToList = onBackToList, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ResultContent(
    page: ExamResultPage,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            page.submittedAt?.let { submittedLine(page.submitReason, it) }?.let { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
            }
            if (page.released) {
                page.grade?.let { grade ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.result_grade),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GotestTheme.colors.textSecondary,
                        )
                        Text(
                            text = formatGrade(grade),
                            style = MaterialTheme.typography.displaySmall,
                            color = GotestTheme.colors.textPrimary,
                        )
                    }
                }
            } else {
                NoticeBox(text = page.message ?: stringResource(R.string.result_not_released), tone = NoticeTone.Info)
            }
        }
        if (page.released && page.items.isNotEmpty()) ResultGrid(page.items)
        if (page.released && page.showAnswerKey) AnswerKeys(page.items)
        GotestButton(
            text = stringResource(R.string.work_back_to_list),
            onClick = onBackToList,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

/** "Hasil per soal": two columns of "Soal n" and its result. */
@Composable
private fun ResultGrid(items: List<ExamResultItem>) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.result_per_item),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                row.forEach { item ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.work_question, item.number),
                            style = MaterialTheme.typography.labelLarge,
                            color = GotestTheme.colors.textPrimary,
                        )
                        ResultChip(item.result)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnswerKeys(items: List<ExamResultItem>) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.result_key_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        items.filter { it.type != null && it.content != null && it.answerKey != null }.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = GotestTheme.colors.borderSubtle)
            Row(
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.work_question, item.number),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                ResultChip(item.result)
            }
            item.prompt?.let {
                RichText(
                    content = it,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textPrimary,
                )
            }
            ExamItemReview(item)
        }
    }
}

@Composable
fun ResultChip(result: String?) {
    val (label, tone) =
        when (result) {
            "correct" -> R.string.result_correct to GotestChipTone.Green
            "partial" -> R.string.result_partial to GotestChipTone.Yellow
            "wrong" -> R.string.result_wrong to GotestChipTone.Red
            else -> R.string.result_empty to GotestChipTone.Grey
        }
    GotestChip(text = stringResource(label), tone = tone)
}

/** "Terkumpul 13 November 2026 pukul 07.41 WIB." and the automatic-submit variants (Result.tsx). */
@Composable
private fun submittedLine(
    reason: String?,
    submittedAt: String,
): String {
    val instant = parseServerTime(submittedAt)
    val at = stringResource(R.string.result_at, dateWib.format(instant), timeWib.format(instant))
    return when (reason) {
        "time_up" -> stringResource(R.string.result_time_up, at)
        "violation" -> stringResource(R.string.result_violation, at)
        "teacher" -> stringResource(R.string.result_teacher, at)
        else -> stringResource(R.string.result_submitted, at)
    }
}

private val jakarta = ZoneId.of("Asia/Jakarta")
private val indonesian = Locale.forLanguageTag("id-ID")
private val dateWib = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian).withZone(jakarta)
private val timeWib = DateTimeFormatter.ofPattern("HH.mm 'WIB'", indonesian).withZone(jakarta)

@Preview(showBackground = true)
@Composable
private fun ExamResultScreenPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState =
                ExamResultUiState(
                    isLoading = false,
                    page =
                        ExamResultPage(
                            organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
                            title = "Ulangan Bahasa Indonesia",
                            submittedAt = "2026-11-10T00:41:00+00:00",
                            submitReason = "manual",
                            released = true,
                            grade = 82.5,
                            items =
                                listOf(
                                    ExamResultItem(1, "correct"),
                                    ExamResultItem(2, "partial"),
                                    ExamResultItem(3, null),
                                ),
                        ),
                ),
            onRetry = {},
            onBackToList = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamResultScreenWaitingPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState =
                ExamResultUiState(
                    isLoading = false,
                    page =
                        ExamResultPage(
                            organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
                            title = "Kuis Sejarah Indonesia",
                            submittedAt = "2026-11-14T01:15:00+00:00",
                            submitReason = "time_up",
                        ),
                ),
            onRetry = {},
            onBackToList = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamResultScreenErrorPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState = ExamResultUiState(isLoading = false, errorMessage = "Ujian ini belum dikumpulkan."),
            onRetry = {},
            onBackToList = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamResultScreenLoadingPreview() {
    GotestTheme { ExamResultScreen(uiState = ExamResultUiState(), onRetry = {}, onBackToList = {}) }
}
