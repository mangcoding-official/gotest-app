package com.gotest.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Web `StudentStatCards`: Soal Dikerjakan, Akurasi Jawaban, Paket Selesai (Riwayat, Paket Latihan). */
@Composable
fun StudentStatTiles(
    questionsDone: Int,
    accuracy: Int,
    packagesDone: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        StatTile(
            GotestFormat.number(questionsDone),
            stringResource(R.string.history_stat_questions),
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(R.string.history_percent, accuracy),
            stringResource(R.string.history_stat_accuracy),
            Modifier.weight(1f),
        )
        StatTile(GotestFormat.number(packagesDone), stringResource(R.string.history_stat_packages), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        contentPadding = PaddingValues(GotestTheme.spacing.md),
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = GotestTheme.colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/** Web `categoryBadgeClass`: SMA green, UTBK/SNBT blue, others violet. */
fun categoryTone(name: String): GotestChipTone {
    val value = name.lowercase()
    return when {
        "sma" in value -> GotestChipTone.Green
        "utbk" in value || "snbt" in value -> GotestChipTone.Blue
        else -> GotestChipTone.Violet
    }
}
