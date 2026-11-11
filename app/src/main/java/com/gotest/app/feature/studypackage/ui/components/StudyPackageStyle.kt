package com.gotest.app.feature.studypackage.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.CategoryAccent
import com.gotest.app.core.model.Difficulty
import com.gotest.app.core.model.StudyPackage

/** Sizes from the Paket frames that have no design-system token. */
internal object StudyPackageDimens {
    /** Coloured bar on category and subject cards (32×8). */
    val accentBarWidth = 32.dp
    val accentBarHeight = 8.dp

    /** Round number in "Materi yang diujikan" / "Urutan Section". */
    val numberBadge = 24.dp

    /** Icons in the Soal / Durasi / Penilaian tiles. */
    val infoTileIcon = 18.dp

    /** Seasonal Tryout cards scroll horizontally at the design's fixed width. */
    val seasonalCardWidth = 265.dp

    /** Dot between "12 Sub Kategori" and "12 Paket latihan". */
    val metaDot = 4.dp

    /** "Paket disimpan" toast shadow (y 8, blur 24 in the design). */
    val toastElevation = 8.dp
}

/** Colour of the small bar on category cards. */
@Composable
@ReadOnlyComposable
internal fun CategoryAccent.color(): Color =
    when (this) {
        CategoryAccent.Yellow -> GotestTheme.colors.bgYellow

        // The design uses color/illustration/grey (#656565), which has no token yet.
        CategoryAccent.Grey -> GotestTheme.colors.illustrationGrey

        CategoryAccent.Blue -> GotestTheme.colors.bgBlue

        CategoryAccent.Red -> GotestTheme.colors.illustrationRed
    }

@get:StringRes
internal val Difficulty.labelRes: Int
    get() =
        when (this) {
            Difficulty.Easy -> R.string.studypackage_difficulty_easy
            Difficulty.Medium -> R.string.studypackage_difficulty_medium
            Difficulty.Hard -> R.string.studypackage_difficulty_hard
        }

internal val Difficulty.chipTone: GotestChipTone
    get() =
        when (this) {
            Difficulty.Easy -> GotestChipTone.Green
            Difficulty.Medium -> GotestChipTone.Yellow
            Difficulty.Hard -> GotestChipTone.Red
        }

/** Call to action of a practice package card, derived from its progress. */
internal enum class PracticeAction(
    @param:StringRes val labelRes: Int,
) {
    Start(R.string.studypackage_action_start_practice),
    Continue(R.string.studypackage_action_continue),
    Repeat(R.string.studypackage_action_repeat),
}

internal fun StudyPackage.practiceAction(): PracticeAction {
    val progress = progress
    return when {
        progress != null && progress.total > 0 && progress.answered >= progress.total -> PracticeAction.Repeat
        progress != null && progress.answered > 0 -> PracticeAction.Continue
        progress == null && attempts > 0 -> PracticeAction.Repeat
        else -> PracticeAction.Start
    }
}

/** An unfinished attempt the "Detail Latihan" sheet and "Detail Paket" offer to resume. */
internal val StudyPackage.hasResumableProgress: Boolean
    get() = progress?.let { it.answered in 1 until it.total } == true
