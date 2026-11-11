package com.gotest.app.feature.assistant.ui.components

import androidx.compose.ui.unit.dp

/** Sizes from the AI chat frames that the shared tokens do not cover. */
internal object AssistantDimens {
    /** "ai-orb" on the welcome layout: halo, brand ring, avatar and its glyph. */
    val welcomeOrb = 112.dp
    val welcomeOrbRing = 84.dp
    val welcomeOrbAvatar = 64.dp
    val welcomeOrbIcon = 32.dp

    /** "voice-orb" on the listening overlay: halo, clipping ring, dark circle, glyph and shadow. */
    val voiceOrb = 232.dp
    val voiceOrbRing = 176.dp
    val voiceOrbCore = 120.dp
    val voiceOrbIcon = 44.dp
    val voiceOrbShadowBlur = 32.dp
    val voiceOrbShadowOffsetY = 12.dp

    val suggestionIconBox = 36.dp
    val suggestionIcon = 18.dp

    val chatInputMinHeight = 58.dp
    val chatInputIcon = 22.dp
    val chatInputFocusedBorder = 1.5.dp
    val sendButton = 44.dp

    /** Space before AI attachments, lining them up with the bubble after the avatar. */
    val attachmentIndent = 40.dp

    /** Gap that keeps the student's bubbles away from the screen edges. */
    val userBubbleInset = 48.dp

    val typingDot = 8.dp
    val typingBubblePaddingVertical = 14.dp

    /** "Riwayat Percakapan" panel (frame 21622). */
    val historyPanelWidth = 340.dp
    val historySearchHeight = 40.dp

    /** Corner radius, vertical padding and icon gap of a history row and "Hapus semua riwayat". */
    val historyRowRadius = 10.dp
    val historyRowSpacing = 10.dp

    /** The 18dp glyphs of the history panel and the attachment hint. */
    val smallIcon = 18.dp

    /** "Tambah Lampiran" tiles (frame 22009). */
    val attachmentTileIconBox = 48.dp
    val attachmentTileIconRadius = 14.dp
    val attachmentTileIcon = 22.dp

    /** Pending attachments above the input and sent ones above the student's bubble (not in the design). */
    val pendingAttachmentThumbnail = 32.dp
    val pendingAttachmentNameMaxWidth = 120.dp
    val sentAttachmentImage = 96.dp
    val sentAttachmentNameMaxWidth = 160.dp

    val voiceButton = 56.dp
    val voiceDoneButton = 72.dp
    val voiceButtonIcon = 22.dp
    val voiceStopSquare = 22.dp
    val voiceContentSpacing = 28.dp
    val voiceControlsSpacing = 40.dp

    val waveformBarWidth = 5.dp
    val waveformBarGap = 5.dp
    val waveformHeight = 40.dp

    /** Bar heights of the design's "waveform", left to right. */
    val waveformBars = listOf(10.dp, 18.dp, 30.dp, 22.dp, 40.dp, 28.dp, 16.dp, 34.dp, 24.dp, 12.dp, 20.dp, 8.dp)

    const val VOICE_ORB_SHADOW_ALPHA = 0.3f

    /** Shortest bar, as a fraction of its design height, while the waveform animates. */
    const val WAVEFORM_MIN_SCALE = 0.35f
    const val WAVEFORM_PERIOD_MS = 900
    const val WAVEFORM_PHASE_STEP = 0.55f
    const val TYPING_PERIOD_MS = 1_200
    const val TYPING_MIN_ALPHA = 0.3f
}
