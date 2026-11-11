package com.gotest.app.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestBottomBar
import com.gotest.app.core.designsystem.component.GotestBottomBarItem
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDialogContent
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.component.GotestToggle
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * Debug-only style guide that renders every design-system component on one screen, to compare
 * against the design's component page. Not part of the release build. Sample copy is literal on
 * purpose: it is fixture data, not app UI.
 */
class DesignSystemCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GotestTheme { Catalog() } }
    }
}

@Composable
private fun Catalog(modifier: Modifier = Modifier) {
    val spacing = GotestTheme.spacing
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            GotestTopAppBar(
                title = "Detail Paket",
                navigationIcon = { GotestBackButton(onClick = {}) },
                actions = {
                    GotestIconButton(icon = GotestIcons.Linear.ArchiveAdd, contentDescription = null, onClick = {})
                },
            )
            GotestTopAppBar(
                title = "Paket Belajar",
                subtitle = "Pilih latihan atau tryout sesuai jenjangmu.",
                style = GotestTopAppBarStyle.Leading,
                actions = {
                    GotestIconButton(icon = GotestIcons.Linear.SearchNormal, contentDescription = null, onClick = {})
                    GotestIconButton(
                        icon = GotestIcons.Linear.Notification,
                        contentDescription = null,
                        onClick = {},
                        showBadge = true,
                    )
                },
            )
            Column(
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                CatalogInputs()
                CatalogButtons()
                CatalogSelection()
                CatalogCard()
                GotestDialogContent(
                    title = "Selesaikan latihan?",
                    message = "Cek semua soal sebelum menyelesaikan latihan. Kamu sudah menjawab 4 dari 20 soal.",
                    confirmText = "Selesaikan",
                    onConfirm = {},
                    icon = GotestIcons.Bold.TaskSquare,
                    dismissText = "Batal",
                )
                GotestEmptyView(
                    icon = GotestIcons.Bold.Notification,
                    title = "Belum ada notifikasi",
                    message = "Semua notifikasi tentang latihan, tryout, dan peringkatmu akan muncul di sini.",
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.padding(bottom = GotestTheme.sizes.bottomBarHeight * 2))
            }
        }
        var tab by remember { mutableIntStateOf(0) }
        GotestBottomBar(
            items =
                listOf(
                    GotestBottomBarItem("Dashboard", GotestIcons.Bold.Home2, GotestIcons.Linear.Home2),
                    GotestBottomBarItem("Paket", GotestIcons.Bold.TaskSquare, GotestIcons.Linear.TaskSquare),
                    GotestBottomBarItem("Riwayat", GotestIcons.Bold.Directbox, GotestIcons.Linear.Directbox),
                    GotestBottomBarItem("Peringkat", GotestIcons.Bold.Leaderboard, GotestIcons.Linear.Leaderboard),
                    GotestBottomBarItem("Profil", GotestIcons.Bold.User, GotestIcons.Linear.User),
                ),
            selectedIndex = tab,
            onSelect = { tab = it },
            onAssistantClick = {},
            assistantContentDescription = "Asisten Gotest",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CatalogInputs(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("rahasia123") }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        GotestTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            placeholder = "nama@email.com",
            leadingIcon = GotestIcons.Linear.User,
        )
        GotestPasswordField(value = password, onValueChange = { password = it }, label = "Password")
        GotestTextField(
            value = "KTR4QX",
            onValueChange = {},
            label = "Kode",
            supportingText = "Kode tidak ditemukan",
            isError = true,
        )
    }
}

@Composable
private fun CatalogButtons(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(text = "Masuk", onClick = {}, modifier = Modifier.fillMaxWidth())
        GotestButton(
            text = "Masuk dengan Google",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
        GotestButton(text = "Mulai Permainan", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text = "Lanjutkan",
                onClick = {},
                size = GotestButtonSize.Small,
                trailingIcon = GotestIcons.Linear.ArrowRight,
            )
            GotestButton(
                text = "Latihan Lagi",
                onClick = {},
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
            GotestButton(
                text = "Lihat Semua",
                onClick = {},
                variant = GotestButtonVariant.Text,
                size = GotestButtonSize.Small,
            )
        }
    }
}

@Composable
private fun CatalogSelection(modifier: Modifier = Modifier) {
    var segment by remember { mutableIntStateOf(0) }
    var pill by remember { mutableIntStateOf(0) }
    var toggle by remember { mutableStateOf(true) }
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        GotestSegmentedControl(options = listOf("Paket Latihan", "Paket Tryout"), selectedIndex = segment, onSelect = {
            segment = it
        })
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf("Semua", "Latihan", "Tryout").forEachIndexed { index, label ->
                GotestFilterPill(text = label, selected = index == pill, onClick = { pill = index })
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GotestChipTone.entries.forEach { GotestChip(text = it.name, tone = it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestToggle(checked = toggle, onCheckedChange = { toggle = it })
            GotestToggle(checked = !toggle, onCheckedChange = { toggle = !it })
        }
    }
}

@Composable
private fun CatalogCard(modifier: Modifier = Modifier) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Text(text = "Latihan September session 1", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "UTBK-SNBT • 20 Soal • 45 Menit",
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        GotestProgressBar(progress = 0.25f)
    }
}
