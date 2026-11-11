package com.gotest.app.feature.auth.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthLogo

/** Onboarding has no state, so the route only forwards navigation. */
@Composable
fun OnboardingRoute(
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingScreen(onSignIn = onSignIn, onRegister = onRegister, modifier = modifier)
}

@Composable
fun OnboardingScreen(
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthLogo(modifier = Modifier.align(Alignment.Start))
        Image(
            painter = painterResource(R.drawable.illus_onboarding),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
        )
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.xl) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    text = stringResource(R.string.onboarding_headline).uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    color = GotestTheme.colors.textBrandDark,
                )
                Text(
                    text = stringResource(R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = GotestTheme.colors.textSecondary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.onboarding_sign_in),
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
                GotestButton(
                    text = stringResource(R.string.onboarding_register),
                    onClick = onRegister,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                )
            }
            AuthCopyright()
        }
    }
}

@Preview(showBackground = true, heightDp = 956, widthDp = 440)
@Composable
private fun OnboardingScreenPreview() {
    GotestTheme {
        OnboardingScreen(onSignIn = {}, onRegister = {})
    }
}
