package com.gotest.app.feature.support.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.HelpAccordionItem
import com.gotest.app.feature.support.domain.SupportPage
import com.gotest.app.feature.support.domain.SupportTicket
import com.gotest.app.feature.support.domain.TicketReply
import com.gotest.app.feature.support.domain.TicketStatus
import kotlinx.coroutines.launch

/** A topic card of the web's Support/data.ts; [slug] opens `GET /bantuan/{slug}`. */
private data class TopicCard(
    val slug: String,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:DrawableRes val icon: Int,
)

private val topicCards =
    listOf(
        TopicCard(
            "akun-profil",
            R.string.support_topic_account_title,
            R.string.support_topic_account_body,
            GotestIcons.Linear.User,
        ),
        TopicCard(
            "paket-latihan-tryout",
            R.string.support_topic_practice_title,
            R.string.support_topic_practice_body,
            GotestIcons.Linear.TaskSquare,
        ),
        TopicCard(
            "pembayaran-berlangganan",
            R.string.support_topic_payment_title,
            R.string.support_topic_payment_body,
            GotestIcons.Linear.TickCircle,
        ),
    )

private val faqs =
    listOf(
        R.string.support_faq_1_q to R.string.support_faq_1_a,
        R.string.support_faq_2_q to R.string.support_faq_2_a,
        R.string.support_faq_3_q to R.string.support_faq_3_a,
        R.string.support_faq_4_q to R.string.support_faq_4_a,
        R.string.support_faq_5_q to R.string.support_faq_5_a,
        R.string.support_faq_6_q to R.string.support_faq_6_a,
    )

private const val SUPPORT_MAIL = "mailto:support@gotest.id"
private const val SUPPORT_WHATSAPP = "https://wa.me/6231422453"
private const val CONTACT_ALPHA = 0.8f

@Composable
fun SupportRoute(
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    ObserveAsEvents(viewModel.effects) { failed ->
        scope.launch { snackbar.showSnackbar(failed.message ?: resources.getString(R.string.state_error_message)) }
    }
    SupportScreen(
        uiState = uiState,
        actions =
            SupportActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onOpenTopic = onOpenTopic,
                onToggleFaq = viewModel::toggleFaq,
                onFormChange = viewModel::onFormChange,
                onSendTicket = viewModel::sendTicket,
                onToggleTicket = viewModel::toggleTicket,
                onReplyChange = viewModel::onReplyChange,
                onSendReply = viewModel::sendReply,
                onOpenLink = { link -> context.openSupportLink(link) },
            ),
        modifier = modifier,
        snackbarHostState = snackbar,
    )
}

private fun Context.openSupportLink(link: String) {
    val action = if (link.startsWith("mailto:")) Intent.ACTION_SENDTO else Intent.ACTION_VIEW
    runCatching { startActivity(Intent(action, link.toUri())) }
}

data class SupportActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenTopic: (String) -> Unit,
    val onToggleFaq: (Int) -> Unit,
    val onFormChange: (TicketForm) -> Unit,
    val onSendTicket: () -> Unit,
    val onToggleTicket: (Long) -> Unit,
    val onReplyChange: (Long, String) -> Unit,
    val onSendReply: (Long) -> Unit,
    val onOpenLink: (String) -> Unit,
)

@Composable
fun SupportScreen(
    uiState: SupportUiState,
    actions: SupportActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(
                title = stringResource(R.string.support_title),
                navigationIcon = { GotestBackButton(onClick = actions.onBack) },
            )
            SupportList(uiState = uiState, actions = actions)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        )
    }
}

@Composable
private fun SupportList(
    uiState: SupportUiState,
    actions: SupportActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.sm,
                bottom = spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "lede") { SectionHead(R.string.support_title, R.string.support_lede) }
        items(
            topicCards,
            key = TopicCard::slug,
        ) { card -> TopicCardItem(card, onOpen = { actions.onOpenTopic(card.slug) }) }
        item(key = "faq") { FaqSection(openFaq = uiState.openFaq, onToggle = actions.onToggleFaq) }
        val page = uiState.page
        when {
            page != null -> tickets(page, uiState, actions)
            uiState.loadFailed -> item(key = "error") { GotestErrorView(onRetry = actions.onRetry) }
            else -> item(key = "loading") { GotestLoadingView() }
        }
        item(key = "contacts") { ContactCards(onOpenLink = actions.onOpenLink) }
    }
}

@Composable
internal fun SectionHead(
    @StringRes title: Int,
    @StringRes lede: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(lede),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun TopicCardItem(
    card: TopicCard,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(card.icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(card.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(card.body),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(
                    GotestTheme.sizes.avatarXs,
                ).background(GotestTheme.colors.bgBrandSoft, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = GotestTheme.colors.iconBrand,
        )
    }
}

@Composable
private fun FaqSection(
    openFaq: Int?,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        SectionHead(R.string.support_faq_title, R.string.support_faq_lede)
        faqs.forEachIndexed { index, (question, answer) ->
            HelpAccordionItem(
                question = stringResource(question),
                answer = stringResource(answer),
                expanded = openFaq == index,
                onToggle = { onToggle(index) },
            )
        }
    }
}

private fun LazyListScope.tickets(
    page: SupportPage,
    uiState: SupportUiState,
    actions: SupportActions,
) {
    item(key = "form") {
        TicketFormCard(
            categories = page.ticketCategories,
            form = uiState.form,
            onChange = actions.onFormChange,
            onSend = actions.onSendTicket,
        )
    }
    if (page.tickets.isNotEmpty()) {
        item(key = "tickets-head") { SectionHead(R.string.support_tickets_title, R.string.support_tickets_lede) }
        itemsIndexed(page.tickets, key = { _, ticket -> "ticket-${ticket.id}" }) { _, ticket ->
            TicketCard(
                ticket = ticket,
                open = ticket.id in uiState.openTickets,
                reply = uiState.replies[ticket.id].orEmpty(),
                error = uiState.replyErrors[ticket.id],
                sending = ticket.id in uiState.replying,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ContactCards(
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The web shows the contacts a little faded (opacity-80).
    Column(
        modifier = modifier.fillMaxWidth().alpha(CONTACT_ALPHA),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        ContactCard(
            GotestIcons.Linear.Directbox,
            R.string.support_contact_email_title,
            R.string.support_contact_email_value,
        ) {
            onOpenLink(SUPPORT_MAIL)
        }
        ContactCard(
            GotestIcons.Linear.Message,
            R.string.support_contact_whatsapp_title,
            R.string.support_contact_whatsapp_value,
        ) {
            onOpenLink(SUPPORT_WHATSAPP)
        }
        ContactCard(
            GotestIcons.Linear.Calendar,
            R.string.support_contact_hours_title,
            R.string.support_contact_hours_value,
            onOpen = null,
        )
    }
}

@Composable
private fun ContactCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes value: Int,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
    }
}

private val previewPage =
    SupportPage(
        tickets =
            listOf(
                SupportTicket(
                    id = 31,
                    subject = "Tidak bisa membuka hasil Try Out",
                    category = "Latihan, Try Out, atau hasil",
                    message = "Halaman hasil tidak terbuka.",
                    status = TicketStatus.InProgress,
                    statusLabel = "Ditangani",
                    createdAt = "2026-11-16T02:10:00+00:00",
                    replies =
                        listOf(
                            TicketReply(
                                90,
                                true,
                                "Tim Gotest",
                                "Kami sedang memeriksanya.",
                                "2026-11-17T03:00:00+00:00",
                            ),
                        ),
                ),
            ),
        ticketCategories = mapOf("akun" to "Akun dan masuk", "teknis" to "Kendala teknis"),
    )

private val previewActions = SupportActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})

@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun SupportScreenPreview() {
    GotestTheme {
        SupportScreen(
            uiState = SupportUiState(isLoading = false, page = previewPage, openTickets = setOf(31)),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportScreenLoadingPreview() {
    GotestTheme { SupportScreen(uiState = SupportUiState(), actions = previewActions) }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SupportScreenErrorPreview() {
    GotestTheme {
        SupportScreen(
            uiState = SupportUiState(isLoading = false, loadFailed = true),
            actions = previewActions,
        )
    }
}
