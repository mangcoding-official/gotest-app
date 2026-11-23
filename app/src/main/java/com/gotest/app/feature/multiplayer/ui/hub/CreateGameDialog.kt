package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.OptionCategory
import com.gotest.app.feature.multiplayer.ui.CreateStatus
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.modeLabel
import com.gotest.app.feature.multiplayer.ui.components.problemText

/** "Buat permainan" (web CreateGameModal): mode, category, subtest, topics, question count, duration, summary. */
@Composable
internal fun CreateGameDialog(
    categories: List<OptionCategory>,
    draft: CreateGameDraft,
    status: CreateStatus,
    onDraft: ((CreateGameDraft) -> CreateGameDraft) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GotestCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GotestTheme.spacing.lg, vertical = GotestTheme.spacing.xxl)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.multiplayer_create_screen_title),
                style = MaterialTheme.typography.titleLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Body(stringResource(R.string.multiplayer_create_intro))
            ModeChoice(selected = draft.mode, onSelect = { mode -> onDraft { it.copy(mode = mode) } })
            ChoiceFields(categories = categories, draft = draft, onDraft = onDraft)
            CountAndDuration(categories = categories, draft = draft, onDraft = onDraft)
            status.problem?.let { problem ->
                MbAlert(
                    type = MbAlertType.Error,
                    title =
                        stringResource(
                            if (problem.kind == GameProblem.STOCK) {
                                R.string.multiplayer_create_stock_title
                            } else {
                                R.string.multiplayer_create_failed_title
                            },
                        ),
                    message = problemText(problem),
                )
            }
            Summary(categories = categories, draft = draft)
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.multiplayer_create_cancel),
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    enabled = !status.submitting,
                )
                GotestButton(
                    text =
                        stringResource(
                            if (status.submitting) {
                                R.string.multiplayer_create_submitting
                            } else {
                                R.string.multiplayer_create_submit
                            },
                        ),
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = !status.submitting && draft.maxCount(categories) > 0,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(
    label: String,
    hint: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
        hint?.let { Body(it) }
    }
}

@Composable
private fun ModeChoice(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val colors = GotestTheme.colors
        FieldLabel(stringResource(R.string.multiplayer_create_type))
        listOf(GameState.MODE_DUEL, GameState.MODE_ROOM).forEach { mode ->
            val chosen = mode == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                border =
                    BorderStroke(
                        GotestTheme.sizes.strokeThin,
                        if (chosen) colors.borderFocus else colors.borderSubtle,
                    ),
                color = colors.bgApp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(mode) }),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = chosen, onClick = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = modeLabel(mode),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (chosen) colors.textBrandDark else colors.textPrimary,
                        )
                        Body(
                            stringResource(
                                if (mode == GameState.MODE_DUEL) {
                                    R.string.multiplayer_create_duel_subtitle
                                } else {
                                    R.string.multiplayer_create_room_subtitle
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceFields(
    categories: List<OptionCategory>,
    draft: CreateGameDraft,
    onDraft: ((CreateGameDraft) -> CreateGameDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val category = draft.category(categories)
        val subject = draft.subject(categories)
        SelectField(
            label = stringResource(R.string.multiplayer_create_category),
            value = category?.name.orEmpty(),
            options = categories.map { it.id to it.name },
            onSelect = { id -> onDraft { it.pickCategory(id, categories) } },
        )
        SelectField(
            label = stringResource(R.string.multiplayer_create_subtest),
            value = subject?.name.orEmpty(),
            options = category?.subjects.orEmpty().map { it.id to it.name },
            onSelect = { id -> onDraft { it.pickSubject(id) } },
        )
        if (subject == null || subject.topics.isEmpty()) return@Column
        var picking by remember { mutableStateOf(false) }
        FieldLabel(
            stringResource(R.string.multiplayer_create_topic),
            stringResource(R.string.multiplayer_create_topic_hint),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            draft.selectedTopics(categories).forEach { topic ->
                val description = stringResource(R.string.multiplayer_cd_remove_topic, topic.name)
                Box(
                    modifier =
                        Modifier.clickable(onClickLabel = description) {
                            onDraft {
                                it.copy(
                                    topicIds =
                                        it.topicIds - topic.id,
                                )
                            }
                        },
                ) {
                    GotestChip(
                        text = topic.name,
                        tone = GotestChipTone.Violet,
                        leadingIcon = GotestIcons.Bold.CloseCircle,
                    )
                }
            }
            val others = draft.otherTopics(categories)
            if (others.isNotEmpty()) {
                Box {
                    GotestButton(
                        text = stringResource(R.string.multiplayer_create_add_topic),
                        onClick = { picking = true },
                        variant = GotestButtonVariant.Text,
                        size = GotestButtonSize.Small,
                    )
                    DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                        others.forEach { topic ->
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                                        Text(topic.name, modifier = Modifier.weight(1f, fill = false))
                                        Body(stringResource(R.string.multiplayer_questions, topic.questionCount))
                                    }
                                },
                                onClick = {
                                    picking = false
                                    onDraft { it.copy(topicIds = it.topicIds + topic.id) }
                                },
                            )
                        }
                    }
                }
            }
        }
        Body(stringResource(R.string.multiplayer_create_topic_helper))
    }
}

@Composable
private fun SelectField(
    label: String,
    value: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        GotestTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = label,
            trailingContent = {
                Icon(
                    painter = painterResource(GotestIcons.Linear.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconSm),
                    tint = GotestTheme.colors.iconDefault,
                )
            },
            readOnly = true,
        )
        Box(modifier = Modifier.matchParentSize().clickable(role = Role.DropdownList) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        open = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountAndDuration(
    categories: List<OptionCategory>,
    draft: CreateGameDraft,
    onDraft: ((CreateGameDraft) -> CreateGameDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val colors = GotestTheme.colors
        val maxCount = draft.maxCount(categories)
        val count = draft.count(categories)
        FieldLabel(stringResource(R.string.multiplayer_detail_question_count))
        Row(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = "−",
                onClick = { onDraft { it.copy(questionCount = maxOf(1, count - 1)) } },
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Medium,
                enabled = count > 1,
                modifier = Modifier.described(stringResource(R.string.multiplayer_cd_fewer_questions)),
            )
            Text(
                text = if (maxCount == 0) "" else count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            GotestButton(
                text = "+",
                onClick = { onDraft { it.copy(questionCount = minOf(maxCount, count + 1)) } },
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Medium,
                enabled = count < maxCount,
                modifier = Modifier.described(stringResource(R.string.multiplayer_cd_more_questions)),
            )
        }
        if (maxCount == 0) {
            Text(
                text = stringResource(R.string.multiplayer_create_no_stock),
                style = MaterialTheme.typography.bodySmall,
                color = colors.statusDanger,
            )
        } else {
            Body(stringResource(R.string.multiplayer_create_stock, draft.stock(categories), GameRules.MAX_QUESTIONS))
        }
        val duration = draft.duration(categories)
        val minimum = draft.minimumDuration(categories)
        FieldLabel(stringResource(R.string.multiplayer_detail_duration))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            GameRules.durationChoices.forEach { minutes ->
                DurationChip(
                    text = stringResource(R.string.multiplayer_minutes, minutes),
                    selected = minutes == duration,
                    enabled = minutes >= minimum,
                    onClick = { onDraft { it.copy(durationMinutes = minutes) } },
                )
            }
        }
        val tooShort = draft.tooShort(categories)
        if (tooShort.isNotEmpty()) {
            val list = tooShort.map { stringResource(R.string.multiplayer_minutes, it) }.joinToString(", ")
            Body(
                stringResource(
                    R.string.multiplayer_create_duration_helper,
                    list,
                    GameRules.MIN_SECONDS_PER_QUESTION,
                    minimum,
                ),
            )
        }
    }
}

@Composable
private fun Summary(
    categories: List<OptionCategory>,
    draft: CreateGameDraft,
) {
    val topics = draft.selectedTopics(categories)
    val rows =
        listOf(
            stringResource(R.string.multiplayer_detail_type) to modeLabel(draft.mode),
            stringResource(R.string.multiplayer_detail_subtest) to
                (draft.subject(categories)?.name ?: stringResource(R.string.multiplayer_create_none)),
            stringResource(R.string.multiplayer_detail_topic) to
                (
                    topics.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }
                        ?: stringResource(R.string.multiplayer_create_all_topics)
                ),
            stringResource(R.string.multiplayer_detail_question_count) to
                (
                    if (draft.maxCount(categories) == 0) {
                        stringResource(R.string.multiplayer_create_none)
                    } else {
                        draft.count(categories).toString()
                    }
                ),
            stringResource(R.string.multiplayer_detail_duration) to
                stringResource(R.string.multiplayer_minutes, draft.duration(categories)),
        )
    GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = GotestTheme.colors.bgSubtle, showBorder = false) {
        Text(
            text = stringResource(R.string.multiplayer_create_summary),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        rows.forEach { (term, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
                Body(term)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** A label read out instead of the button's "−" or "+". */
private fun Modifier.described(label: String) = semantics { contentDescription = label }

/** One duration (web radio button); a choice under 30 seconds per question is greyed out and cannot be picked. */
@Composable
private fun DurationChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = GotestTheme.colors
    Surface(
        modifier =
            Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = GotestTheme.shapes.full,
        color =
            when {
                selected -> colors.bgBrandDark
                !enabled -> colors.disabledContainer
                else -> colors.bgApp
            },
        contentColor =
            when {
                selected -> colors.textOnBrand
                !enabled -> colors.disabledContent
                else -> colors.textPrimary
            },
        border = if (selected) null else BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
