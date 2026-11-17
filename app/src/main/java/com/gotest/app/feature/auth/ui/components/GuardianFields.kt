package com.gotest.app.feature.auth.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion

/** The age question and the parent's contact on Daftar and "Satu langkah lagi" (Aturan 2.14, 2.15). */
@Immutable
data class GuardianFormState(
    val adult: Boolean? = null,
    val channel: GuardianChannel? = null,
    val contact: String = "",
    @param:StringRes val answerErrorRes: Int? = null,
    @param:StringRes val contactErrorRes: Int? = null,
    /** The server's own text, such as "Tulis nomor orang tua atau wali, bukan nomormu sendiri." */
    val contactMessage: String? = null,
) {
    fun channelFor(question: GuardianQuestion): GuardianChannel = channel ?: question.channels.first()
}

/** The question shows only for the levels the server names, while consent is on. */
fun GuardianQuestion?.asksFor(level: EducationLevel?): Boolean = this != null && level != null && level in levels

/** The checked answer, or the state with its errors when something is missing (the web's messages). */
fun GuardianFormState.check(question: GuardianQuestion): Pair<GuardianAnswer?, GuardianFormState> {
    val channel = channelFor(question)
    val contactError = if (adult == false) contactError(channel, contact.trim()) else null
    val answerError = if (adult == null) R.string.guardian_answer_required else null
    val answer =
        when {
            answerError != null || contactError != null -> null
            adult == true -> GuardianAnswer(adult = true, channel = null, contact = null)
            else -> GuardianAnswer(adult = false, channel = channel, contact = contact.trim())
        }
    return answer to copy(answerErrorRes = answerError, contactErrorRes = contactError)
}

@StringRes
private fun contactError(
    channel: GuardianChannel,
    contact: String,
): Int? =
    when (channel) {
        GuardianChannel.WhatsApp -> {
            when {
                contact.isEmpty() -> R.string.guardian_phone_required
                PhoneNumber.normalize(contact) == null -> R.string.phone_invalid
                else -> null
            }
        }

        GuardianChannel.Email -> {
            when {
                contact.isEmpty() -> R.string.guardian_email_required
                !EMAIL.matches(contact) -> R.string.guardian_email_invalid
                else -> null
            }
        }
    }

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Composable
fun GuardianFields(
    question: GuardianQuestion,
    state: GuardianFormState,
    onChange: (GuardianFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Legend(stringResource(R.string.guardian_age_question, question.age))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestFilterPill(
                    text = stringResource(R.string.guardian_yes),
                    selected = state.adult == true,
                    onClick = { onChange(state.copy(adult = true, answerErrorRes = null)) },
                )
                GotestFilterPill(
                    text = stringResource(R.string.guardian_no),
                    selected = state.adult == false,
                    onClick = { onChange(state.copy(adult = false, answerErrorRes = null)) },
                )
            }
            state.answerErrorRes?.let { ErrorText(stringResource(it)) }
        }
        if (state.adult == false) {
            val channel = state.channelFor(question)
            if (question.channels.size > 1) {
                GuardianChannelChoice(
                    channels = question.channels,
                    selected = channel,
                    onSelect = {
                        onChange(
                            state.copy(channel = it, contact = "", contactErrorRes = null, contactMessage = null),
                        )
                    },
                )
            }
            GuardianContactField(
                channel = channel,
                value = state.contact,
                onValueChange = { onChange(state.copy(contact = it, contactErrorRes = null, contactMessage = null)) },
                error = state.contactMessage ?: state.contactErrorRes?.let { stringResource(it) },
            )
        }
    }
}

/** "Kirim tautan persetujuan ke": WhatsApp orang tua | Email orang tua, when both are ready. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardianChannelChoice(
    channels: List<GuardianChannel>,
    selected: GuardianChannel,
    onSelect: (GuardianChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Legend(stringResource(R.string.guardian_channel_legend))
        // Pills wrap on a narrow screen instead of cutting "WhatsApp orang tua" short.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            channels.forEach { channel ->
                GotestFilterPill(
                    text =
                        stringResource(
                            if (channel ==
                                GuardianChannel.WhatsApp
                            ) {
                                R.string.guardian_channel_whatsapp
                            } else {
                                R.string.guardian_channel_email
                            },
                        ),
                    selected = channel == selected,
                    onClick = { onSelect(channel) },
                )
            }
        }
    }
}

/** The parent's WhatsApp number or email, with the web's helper under it when [showHelper]. */
@Composable
fun GuardianContactField(
    channel: GuardianChannel,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    showHelper: Boolean = true,
) {
    val whatsapp = channel == GuardianChannel.WhatsApp
    val helper = stringResource(if (whatsapp) R.string.guardian_phone_helper else R.string.guardian_email_helper)
    GotestTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = stringResource(if (whatsapp) R.string.guardian_phone_label else R.string.guardian_email_label),
        placeholder = stringResource(if (whatsapp) R.string.guardian_phone_hint else R.string.guardian_email_hint),
        leadingIcon = GotestIcons.Linear.DirectNotification.takeUnless { whatsapp },
        supportingText = error ?: helper.takeIf { showHelper },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = if (whatsapp) KeyboardType.Phone else KeyboardType.Email),
    )
}

@Composable
private fun Legend(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.statusDanger)
}
