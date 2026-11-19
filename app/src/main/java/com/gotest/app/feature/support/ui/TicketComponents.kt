package com.gotest.app.feature.support.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.support.domain.SupportTicket
import com.gotest.app.feature.support.domain.TicketReply
import com.gotest.app.feature.support.domain.TicketStatus
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** The web's client limits, so the server's English-attribute messages do not show (spec Q14). */
private const val SUBJECT_MAX = 120
private const val MESSAGE_MAX = 3000

/** Web `formatWindowInstant`: "18 Nov 2026, 09.30 WIB"; empty when missing. */
internal fun wibLabel(value: String?): String =
    value
        ?.let {
            try {
                GotestFormat.windowInstant(OffsetDateTime.parse(it).toInstant())
            } catch (_: DateTimeParseException) {
                null
            }
        }.orEmpty()

/** "Hubungi Tim Gotest": title, kind of problem, message, "Kirim Tiket", and the green note once sent. */
@Composable
internal fun TicketFormCard(
    categories: Map<String, String>,
    form: TicketForm,
    onChange: (TicketForm) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.md) {
        SectionHead(R.string.support_form_title, R.string.support_form_lede)
        form.sent?.let { SentNote(it) }
        GotestTextField(
            value = form.subject,
            onValueChange = { onChange(form.copy(subject = it.take(SUBJECT_MAX))) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.support_form_subject),
            placeholder = stringResource(R.string.support_form_subject_hint),
            supportingText = form.errors["subject"],
            isError = "subject" in form.errors,
        )
        CategoryPicker(
            categories = categories,
            selected = form.category,
            error = form.errors["category"],
            onSelect = { onChange(form.copy(category = it)) },
        )
        GotestTextField(
            value = form.message,
            onValueChange = { onChange(form.copy(message = it.take(MESSAGE_MAX))) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.support_form_message),
            placeholder = stringResource(R.string.support_form_message_hint),
            supportingText = form.errors["message"],
            isError = "message" in form.errors,
            singleLine = false,
        )
        GotestButton(
            text = stringResource(if (form.sending) R.string.support_sending else R.string.support_form_send),
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
            size = GotestButtonSize.Medium,
            enabled = !form.sending,
        )
    }
}

@Composable
private fun SentNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.statusSuccessSoft, MaterialTheme.shapes.small)
                .padding(GotestTheme.spacing.sm),
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.statusSuccess,
    )
}

/** The web's select: "Pilih jenis kendala" until a kind is chosen, kinds in the server's order. */
@Composable
private fun CategoryPicker(
    categories: Map<String, String>,
    selected: String,
    error: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        GotestTextField(
            value = categories[selected].orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.support_form_category),
            placeholder = stringResource(R.string.support_form_category_hint),
            trailingContent = {
                Icon(
                    painter = painterResource(GotestIcons.Linear.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconSm),
                    tint = GotestTheme.colors.iconDefault,
                )
            },
            supportingText = error,
            isError = error != null,
            readOnly = true,
        )
        Box(modifier = Modifier.matchParentSize().clickable(role = Role.DropdownList) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            categories.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(key)
                        open = false
                    },
                )
            }
        }
    }
}

/** One of "Tiket Saya": the head opens and closes it; the body shows the message, replies, and a reply box. */
@Composable
internal fun TicketCard(
    ticket: SupportTicket,
    open: Boolean,
    reply: String,
    error: String?,
    sending: Boolean,
    actions: SupportActions,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.sm) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { actions.onToggleTicket(ticket.id) },
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ticket.subject, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(
                    text = stringResource(R.string.support_ticket_meta, ticket.category, wibLabel(ticket.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            GotestChip(text = ticket.statusLabel, tone = statusTone(ticket.status))
        }
        if (open) {
            GotestDivider()
            Text(text = ticket.message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            ticket.replies.forEach { ReplyBubble(it) }
            GotestTextField(
                value = reply,
                onValueChange = { actions.onReplyChange(ticket.id, it.take(MESSAGE_MAX)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder =
                    stringResource(
                        if (ticket.status ==
                            TicketStatus.Resolved
                        ) {
                            R.string.support_reply_hint_resolved
                        } else {
                            R.string.support_reply_hint
                        },
                    ),
                supportingText = error,
                isError = error != null,
                singleLine = false,
            )
            GotestButton(
                text = stringResource(if (sending) R.string.support_sending else R.string.support_reply_send),
                onClick = { actions.onSendReply(ticket.id) },
                modifier = Modifier.align(Alignment.End),
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
                enabled = !sending,
            )
        }
    }
}

/** Team replies on light violet, the account's own on light grey, as on the web. */
@Composable
private fun ReplyBubble(
    reply: TicketReply,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(if (reply.fromTeam) colors.bgBrandSoft else colors.bgSubtle, MaterialTheme.shapes.small)
                .padding(GotestTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.support_ticket_meta, reply.author, wibLabel(reply.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Text(text = reply.body, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
    }
}

private fun statusTone(status: TicketStatus): GotestChipTone =
    when (status) {
        TicketStatus.Open -> GotestChipTone.Yellow
        TicketStatus.InProgress -> GotestChipTone.Blue
        TicketStatus.Resolved -> GotestChipTone.Green
    }

@Preview(showBackground = true)
@Composable
private fun TicketFormCardPreview() {
    GotestTheme {
        TicketFormCard(
            categories = mapOf("akun" to "Akun dan masuk"),
            form =
                TicketForm(
                    errors = mapOf("message" to "Tulis kendalamu."),
                    sent = "Tiket terkirim. Tim Gotest membalas di halaman ini.",
                ),
            onChange = {},
            onSend = {},
        )
    }
}
