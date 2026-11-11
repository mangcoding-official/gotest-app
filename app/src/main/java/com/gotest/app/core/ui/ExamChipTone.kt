package com.gotest.app.core.ui

import com.gotest.app.core.designsystem.component.GotestChipTone

/** Chip colour for an exam or level label, as the design colours them (Paket Belajar, frame 1758). */
fun examChipTone(examLabel: String): GotestChipTone =
    when (examLabel) {
        "UTBK-SNBT" -> GotestChipTone.Blue
        "SMA" -> GotestChipTone.Green
        "CPNS", "SMP" -> GotestChipTone.Violet
        else -> GotestChipTone.Grey
    }
