package com.gotest.app.feature.account.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

private const val CHEVRON_EXPANDED_DEGREES = 180f

/**
 * One "faq" row of the help screens: question with a chevron, and the answer when [expanded].
 * The open chevron points up and turns brand-dark, as in the design.
 */
@Composable
fun HelpAccordionItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val rotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_EXPANDED_DEGREES else 0f,
        label = "accordionChevron",
    )
    val actionLabel =
        stringResource(if (expanded) R.string.account_accordion_collapse else R.string.account_accordion_expand)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onToggle)
                .padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            Icon(
                painter = painterResource(GotestIcons.Linear.ChevronDown),
                contentDescription = null,
                modifier = Modifier.size(AccountDimens.iconSmall).rotate(rotation),
                tint = if (expanded) colors.iconBrandDark else colors.iconMuted,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(text = answer, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpAccordionItemPreview() {
    GotestTheme {
        AccountListCard(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
            HelpAccordionItem(
                question = "Bagaimana cara melihat report latihan?",
                answer = "Buka menu Paket, pilih paket latihan yang sudah dikerjakan, lalu ketuk \"Lihat Hasil\".",
                expanded = true,
                onToggle = {},
            )
            HelpAccordionItem(
                question = "Bagaimana cara mengikuti tryout?",
                answer = "Buka menu Paket lalu pilih Paket Tryout.",
                expanded = false,
                onToggle = {},
            )
        }
    }
}
