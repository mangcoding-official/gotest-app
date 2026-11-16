package com.gotest.app.feature.auth.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EducationLevel

@get:StringRes
val EducationLevel.labelRes: Int
    get() =
        when (this) {
            EducationLevel.Sd -> R.string.education_sd
            EducationLevel.Smp -> R.string.education_smp
            EducationLevel.SmaSmk -> R.string.education_sma
            EducationLevel.Kuliah -> R.string.education_kuliah
            EducationLevel.Graduated -> R.string.education_graduated
        }

/** "Jenjang pendidikan": every self sign-up picks one (Aturan 2.13), in the web's order. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EducationLevelPicker(
    selected: EducationLevel?,
    onSelect: (EducationLevel) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.education_label),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            EducationLevel.entries.forEach { level ->
                GotestFilterPill(
                    text = stringResource(level.labelRes),
                    selected = level == selected,
                    onClick = { onSelect(level) },
                )
            }
        }
        if (error != null) {
            Text(text = error, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.statusDanger)
        }
    }
}
