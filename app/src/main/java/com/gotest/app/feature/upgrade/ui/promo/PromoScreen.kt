package com.gotest.app.feature.upgrade.ui.promo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.upgrade.domain.Promo
import com.gotest.app.feature.upgrade.ui.components.UpgradeBottomBar
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData
import com.gotest.app.feature.upgrade.ui.components.UpgradeSectionTitle

@Composable
fun PromoRoute(
    onBack: () -> Unit,
    onClaim: (planId: String, promoId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PromoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PromoScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onClaim = onClaim,
        modifier = modifier,
    )
}

/** "Beranda / Promo Semangat Merdeka" (frame 22863). */
@Composable
fun PromoScreen(
    uiState: PromoUiState,
    onEvent: (PromoUiEvent) -> Unit,
    onBack: () -> Unit,
    onClaim: (planId: String, promoId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.upgrade_promo_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        bottomBar = {
            if (uiState is PromoUiState.Success) {
                val label =
                    if (uiState.isPremium) R.string.upgrade_plan_already_premium else R.string.upgrade_promo_claim
                UpgradeBottomBar(showTopBorder = false) {
                    GotestButton(
                        text = stringResource(label),
                        onClick = { onClaim(uiState.promo.planId, uiState.promo.id) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isPremium,
                    )
                }
            }
        },
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState) {
            PromoUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            PromoUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(PromoUiEvent.Retry) }, modifier = contentModifier)
            }

            PromoUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.Award,
                    title = stringResource(R.string.upgrade_promo_empty_title),
                    message = stringResource(R.string.upgrade_promo_empty_message),
                    modifier = contentModifier,
                )
            }

            is PromoUiState.Success -> {
                PromoContent(promo = uiState.promo, modifier = contentModifier)
            }
        }
    }
}

@Composable
private fun PromoContent(
    promo: Promo,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AsyncImage(
            model = promo.bannerUrl,
            // The headline below says the same thing.
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(UpgradeDimens.PROMO_BANNER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop,
        )
        PromoHeadline(promo = promo)
        PromoPriceCard(promo = promo)
        PromoBenefits(benefits = promo.benefits)
        PromoTerms(terms = promo.terms)
    }
}

@Composable
private fun PromoHeadline(
    promo: Promo,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = UpgradeDimens.compactCardGap) {
        GotestChip(text = promo.label, tone = GotestChipTone.Red)
        Text(
            text = promo.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = promo.description,
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

/** Yellow card: product, promo price, struck-through normal price and the "-50%" badge. */
@Composable
private fun PromoPriceCard(
    promo: Promo,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val mutedContent = colors.bgYellowDark.copy(alpha = UpgradeDimens.PROMO_MUTED_ALPHA)
    val originalPrice = GotestFormat.rupiah(promo.originalPriceRupiah)
    val originalPriceDescription = stringResource(R.string.upgrade_promo_cd_original_price, originalPrice)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgYellow, MaterialTheme.shapes.large)
                .padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(text = promo.productName, style = MaterialTheme.typography.labelSmall, color = mutedContent)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = GotestFormat.rupiah(promo.promoPriceRupiah),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.bgYellowDark,
                )
                Text(
                    text = originalPrice,
                    modifier = Modifier.semantics { contentDescription = originalPriceDescription },
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedContent,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
        Text(
            text = stringResource(R.string.upgrade_promo_discount, promo.discountPercent),
            modifier =
                Modifier
                    .background(colors.bgYellowSoft, MaterialTheme.shapes.small)
                    .padding(
                        PaddingValues(
                            horizontal = UpgradeDimens.discountBadgeHorizontalPadding,
                            vertical = UpgradeDimens.discountBadgeVerticalPadding,
                        ),
                    ),
            style = MaterialTheme.typography.labelLarge,
            color = colors.bgYellowDark,
        )
    }
}

/** "Yang kamu dapat". */
@Composable
private fun PromoBenefits(
    benefits: List<String>,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        UpgradeSectionTitle(text = stringResource(R.string.upgrade_promo_benefits_title))
        benefits.forEach { benefit ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UpgradeDimens.promoBenefitGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(GotestIcons.Bold.TickCircle),
                    contentDescription = null,
                    modifier = Modifier.size(UpgradeDimens.promoBenefitIcon),
                    tint = GotestTheme.colors.iconDefault,
                )
                Text(
                    text = benefit,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GotestTheme.colors.textPrimary,
                )
            }
        }
    }
}

/** "Syarat & Ketentuan", one bullet per term. */
@Composable
private fun PromoTerms(
    terms: List<String>,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = UpgradeDimens.compactCardGap) {
        UpgradeSectionTitle(text = stringResource(R.string.upgrade_promo_terms_title))
        Column {
            terms.forEach { term ->
                Text(
                    text = stringResource(R.string.upgrade_promo_term_bullet, term),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PromoScreenPreview() {
    GotestTheme {
        PromoScreen(
            uiState = PromoUiState.Success(promo = UpgradePreviewData.promo, isPremium = false),
            onEvent = {},
            onBack = {},
            onClaim = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PromoScreenPremiumPreview() {
    GotestTheme {
        PromoScreen(
            uiState = PromoUiState.Success(promo = UpgradePreviewData.promo, isPremium = true),
            onEvent = {},
            onBack = {},
            onClaim = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PromoScreenLoadingPreview() {
    GotestTheme {
        PromoScreen(uiState = PromoUiState.Loading, onEvent = {}, onBack = {}, onClaim = { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PromoScreenEmptyPreview() {
    GotestTheme {
        PromoScreen(uiState = PromoUiState.Empty, onEvent = {}, onBack = {}, onClaim = { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PromoScreenErrorPreview() {
    GotestTheme {
        PromoScreen(uiState = PromoUiState.Error, onEvent = {}, onBack = {}, onClaim = { _, _ -> })
    }
}
