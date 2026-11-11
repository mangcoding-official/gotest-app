package com.gotest.app.feature.assistant.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** The four "saran" cards; each sends its text as the first question. */
private enum class WelcomeSuggestion(
    @param:StringRes val textRes: Int,
    @param:DrawableRes val icon: Int,
) {
    Recommend(R.string.assistant_suggestion_recommend, GotestIcons.Linear.Note2),
    FindTryout(R.string.assistant_suggestion_tryout, GotestIcons.Linear.SearchNormal),
    ContinueLast(R.string.assistant_suggestion_continue, GotestIcons.Linear.TaskSquare),
    WeakTopics(R.string.assistant_suggestion_topics, GotestIcons.Linear.Award),
}

private data class SuggestionColors(
    val container: Color,
    val icon: Color,
)

/** "AI Chat / Sambutan" body (frame 11855): orb, greeting and the 2×2 suggestion grid. */
@Composable
internal fun AssistantWelcome(
    firstName: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AssistantOrb()
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.assistant_welcome_title, firstName),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = GotestTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.assistant_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            WelcomeSuggestion.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    row.forEach { suggestion ->
                        val text = stringResource(suggestion.textRes)
                        SuggestionCard(
                            suggestion = suggestion,
                            text = text,
                            onClick = { onSuggestionClick(text) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/** "ai-orb": the AI avatar inside a brand ring and a soft halo. */
@Composable
private fun AssistantOrb(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    val shape = GotestTheme.shapes.full
    Box(
        modifier = modifier.size(AssistantDimens.welcomeOrb).background(colors.bgBrandSoft, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(AssistantDimens.welcomeOrbRing).background(colors.bgBrand, shape),
            contentAlignment = Alignment.Center,
        ) {
            // The design enlarges the 32dp avatar to 64dp but leaves its 16dp glyph in the top-left
            // corner. The glyph is centred and scaled with the circle, as in the base component.
            AssistantAvatar(size = AssistantDimens.welcomeOrbAvatar, iconSize = AssistantDimens.welcomeOrbIcon)
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: WelcomeSuggestion,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = suggestionColors(suggestion)
    GotestCard(modifier = modifier, onClick = onClick) {
        Box(
            modifier =
                Modifier
                    .size(AssistantDimens.suggestionIconBox)
                    .background(colors.container, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(suggestion.icon),
                contentDescription = null,
                modifier = Modifier.size(AssistantDimens.suggestionIcon),
                tint = colors.icon,
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
    }
}

@Composable
private fun suggestionColors(suggestion: WelcomeSuggestion): SuggestionColors {
    val colors = GotestTheme.colors
    return when (suggestion) {
        WelcomeSuggestion.Recommend -> SuggestionColors(colors.bgBrandSoft, colors.iconBrand)
        WelcomeSuggestion.FindTryout -> SuggestionColors(colors.bgBlueSoft, colors.bgBlue)
        WelcomeSuggestion.ContinueLast -> SuggestionColors(colors.bgGreenSoft, colors.bgGreen)
        WelcomeSuggestion.WeakTopics -> SuggestionColors(colors.statusWarningSoft, colors.bgYellow)
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun AssistantWelcomePreview() {
    GotestTheme {
        AssistantWelcome(firstName = "Gilang", onSuggestionClick = {})
    }
}
