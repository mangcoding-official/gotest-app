package com.gotest.app.feature.assistant.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.assistant.ui.assistant.VoiceInputState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * "AI Chat / Voice Mendengarkan" (frame 12266), drawn over the chat. Speech recognition is
 * simulated by the repository; this only shows the live transcript it emits.
 */
@Composable
internal fun AssistantVoiceOverlay(
    voice: VoiceInputState,
    onMenuClick: () -> Unit,
    onTypeClick: () -> Unit,
    onDoneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A plain Surface swallows touches, so the chat underneath stays inert.
    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent) {
        Column(modifier = Modifier.fillMaxSize().background(assistantBackgroundBrush())) {
            AssistantTopBar(
                navigationIcon = GotestIcons.Linear.CloseCircle,
                navigationContentDescription = stringResource(R.string.assistant_voice_close),
                onNavigationClick = onCancelClick,
                onMenuClick = onMenuClick,
            )
            ListeningContent(
                transcript = voice.transcript,
                modifier = Modifier.weight(1f),
            )
            VoiceControls(onTypeClick = onTypeClick, onDoneClick = onDoneClick, onCancelClick = onCancelClick)
        }
    }
}

@Composable
private fun ListeningContent(
    transcript: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    // Centred while it fits, scrollable on short screens.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ListeningBody(
            transcript = transcript,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.screenHorizontal, top = spacing.xl, end = spacing.screenHorizontal),
        )
    }
}

@Composable
private fun ListeningBody(
    transcript: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AssistantDimens.voiceContentSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ListeningPill()
        VoiceOrb()
        VoiceWaveform()
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    if (transcript.isEmpty()) {
                        stringResource(R.string.assistant_voice_waiting)
                    } else {
                        stringResource(R.string.assistant_voice_transcript, transcript)
                    },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (transcript.isEmpty()) GotestTheme.colors.textSecondary else GotestTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.assistant_voice_hint),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ListeningPill(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier,
        shape = GotestTheme.shapes.full,
        color = colors.bgSubtle,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(GotestTheme.sizes.badgeDot)
                        .background(colors.statusDanger, GotestTheme.shapes.full),
            )
            Text(
                text = stringResource(R.string.assistant_voice_listening),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textBrandDark,
            )
        }
    }
}

/**
 * "voice-orb": a dark brand circle with the microphone, in a soft halo. The middle ring clips
 * the circle's shadow, which is what draws the second, slightly darker ring.
 */
@Composable
private fun VoiceOrb(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    val shape = GotestTheme.shapes.full
    Box(
        modifier = modifier.size(AssistantDimens.voiceOrb).background(colors.bgBrandSoft, shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AssistantDimens.voiceOrbRing)
                    .clip(shape)
                    .background(colors.bgBrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(AssistantDimens.voiceOrbCore)
                        .dropShadow(
                            shape = shape,
                            shadow =
                                Shadow(
                                    radius = AssistantDimens.voiceOrbShadowBlur,
                                    color = colors.bgBrandDark,
                                    offset = DpOffset(x = 0.dp, y = AssistantDimens.voiceOrbShadowOffsetY),
                                    alpha = AssistantDimens.VOICE_ORB_SHADOW_ALPHA,
                                ),
                        ).background(colors.bgBrandDark, shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(GotestIcons.Linear.Microphone),
                    contentDescription = null,
                    modifier = Modifier.size(AssistantDimens.voiceOrbIcon),
                    tint = colors.iconOnBrand,
                )
            }
        }
    }
}

/** The design's twelve bars, bouncing between a third of and their full height. */
@Composable
private fun VoiceWaveform(modifier: Modifier = Modifier) {
    val bars = AssistantDimens.waveformBars
    val barWidth = AssistantDimens.waveformBarWidth
    val gap = AssistantDimens.waveformBarGap
    val color = GotestTheme.colors.bgBrand
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = AssistantDimens.WAVEFORM_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "waveformPhase",
    )
    Canvas(
        modifier =
            modifier
                .width(barWidth * bars.size + gap * (bars.size - 1))
                .height(AssistantDimens.waveformHeight),
    ) {
        val barWidthPx = barWidth.toPx()
        val step = barWidthPx + gap.toPx()
        val radius = CornerRadius(barWidthPx / 2)
        bars.forEachIndexed { index, barHeight ->
            val wave = abs(sin(phase + index * AssistantDimens.WAVEFORM_PHASE_STEP))
            val scale = AssistantDimens.WAVEFORM_MIN_SCALE + (1f - AssistantDimens.WAVEFORM_MIN_SCALE) * wave
            val heightPx = barHeight.toPx() * scale
            drawRoundRect(
                color = color,
                topLeft = Offset(index * step, (size.height - heightPx) / 2),
                size = Size(barWidthPx, heightPx),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun VoiceControls(
    onTypeClick: () -> Unit,
    onDoneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        horizontalArrangement =
            Arrangement.spacedBy(
                AssistantDimens.voiceControlsSpacing,
                Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceSecondaryButton(
            icon = GotestIcons.Linear.Message,
            label = stringResource(R.string.assistant_voice_type),
            onClick = onTypeClick,
        )
        VoiceDoneButton(label = stringResource(R.string.assistant_voice_done), onClick = onDoneClick)
        VoiceSecondaryButton(
            icon = GotestIcons.Linear.CloseCircle,
            label = stringResource(R.string.assistant_voice_cancel),
            onClick = onCancelClick,
        )
    }
}

@Composable
private fun VoiceSecondaryButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = label },
            shape = GotestTheme.shapes.full,
            color = colors.bgSubtle,
            contentColor = colors.iconDefault,
            border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
        ) {
            Box(modifier = Modifier.size(AssistantDimens.voiceButton), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(AssistantDimens.voiceButtonIcon),
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )
    }
}

/** Red "Selesai" button with the white stop square. */
@Composable
private fun VoiceDoneButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = label },
            shape = GotestTheme.shapes.full,
            color = colors.statusDanger,
        ) {
            Box(modifier = Modifier.size(AssistantDimens.voiceDoneButton), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(AssistantDimens.voiceStopSquare)
                            .background(colors.bgApp, MaterialTheme.shapes.extraSmall),
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantVoiceOverlayPreview() {
    GotestTheme {
        AssistantVoiceOverlay(
            voice = VoiceInputState(transcript = "Aku mau latihan matematika buat persiapan UTBK", elapsedSeconds = 4),
            onMenuClick = {},
            onTypeClick = {},
            onDoneClick = {},
            onCancelClick = {},
        )
    }
}
