package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gotest.app.core.designsystem.theme.GotestColors

/** Main Bareng sizes the shared tokens do not have (measured from the "MB/…" components). */
internal object MultiplayerDimens {
    /** "MB/Code Box" height; the width follows the row. */
    val codeBoxHeight = 64.dp

    /** Focus stroke of "MB/Code Box". */
    val codeBoxFocusStroke = 2.dp

    /** Error stroke of "MB/Code Box" and the over-stock question field. */
    val errorStroke = 1.5.dp

    /** Round status badge of Main Bareng dialogs and "Jawabanmu sudah terkirim". */
    val statusBadge = 72.dp
    val statusBadgeIcon = 36.dp

    /** Icon tile of the "Buat Permainan" and "Gabung dengan Kode" cards. */
    val featureTile = 44.dp
    val featureTileIcon = 22.dp

    /** Icon tile at the top of the join sheet. */
    val sheetTile = 48.dp

    /** Width of the question-count field between the stepper buttons. */
    val stepperValueWidth = 72.dp

    /** Dot inside the "Menunggu pemain" / "Selesai" chips. */
    val statusDot = 6.dp

    /** Legend swatch of the question navigator. */
    val legendSwatch = 10.dp

    /** Champion's avatar on "Hasil Room". */
    val championAvatar = 72.dp

    /** Dash and gap of the empty seat's avatar outline. */
    val dash = 4.dp

    /** Radio indicator of the "Jenis permainan" cards: selected ring and idle stroke. */
    val radioSelectedStroke = 6.dp
    val radioStroke = 1.5.dp

    /** Label column and player avatar of "Detail Permainan". */
    val detailLabelWidth = 120.dp
    val detailAvatar = 36.dp

    /** Cells per row of "Benar atau salah per soal" and of the question navigator. */
    const val OUTCOME_GRID_COLUMNS = 6
    const val NAVIGATOR_GRID_COLUMNS = 5
}

/** The design's outer "spread" ring (focus, error, badges): drawn outside the bounds, no layout space. */
internal fun Modifier.outerRing(
    color: Color,
    width: Dp,
    shape: Shape,
): Modifier =
    drawBehind {
        val ring = width.toPx()
        val outline = shape.createOutline(Size(size.width + ring * 2, size.height + ring * 2), layoutDirection, this)
        translate(-ring, -ring) { drawOutline(outline, color) }
    }
