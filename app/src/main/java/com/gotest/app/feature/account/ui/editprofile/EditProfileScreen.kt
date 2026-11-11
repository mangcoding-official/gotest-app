package com.gotest.app.feature.account.ui.editprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountNote
import com.gotest.app.feature.account.ui.components.PhotoMessageHost
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState
import com.gotest.app.feature.account.ui.components.ProfilePhotoCaptureDialog
import com.gotest.app.feature.account.ui.components.ProfilePhotoSheet
import com.gotest.app.feature.account.ui.components.ProfilePhotoSheetContent
import com.gotest.app.feature.account.ui.components.rememberGalleryPhotoPicker
import com.gotest.app.feature.account.ui.components.rememberTakePhotoAction
import kotlinx.coroutines.launch

@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetSnackbarHostState = remember { SnackbarHostState() }
    val captureSnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            EditProfileEffect.Saved -> {
                onSaveSuccess()
            }

            is EditProfileEffect.ShowMessage -> {
                val host =
                    when (effect.host) {
                        PhotoMessageHost.Screen -> snackbarHostState
                        PhotoMessageHost.Sheet -> sheetSnackbarHostState
                        PhotoMessageHost.Capture -> captureSnackbarHostState
                    }
                scope.launch { host.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    val pickPhoto = rememberGalleryPhotoPicker { viewModel.onEvent(EditProfileUiEvent.PhotoPick(it)) }
    val takePhoto = rememberTakePhotoAction(sheetSnackbarHostState) { viewModel.onEvent(EditProfileUiEvent.TakePhoto) }
    EditProfileScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onTakePhoto = takePhoto,
        onPickPhoto = pickPhoto,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        sheetSnackbarHostState = sheetSnackbarHostState,
        captureSnackbarHostState = captureSnackbarHostState,
    )
}

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    sheetSnackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    captureSnackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_edit_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (uiState is EditProfileUiState.Success) {
                AccountBottomActions {
                    GotestButton(
                        text = stringResource(R.string.account_edit_submit),
                        onClick = { onEvent(EditProfileUiEvent.Submit) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                    )
                }
            }
        },
    ) { padding ->
        when (uiState) {
            EditProfileUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            EditProfileUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(EditProfileUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is EditProfileUiState.Success -> {
                EditProfileForm(state = uiState, onEvent = onEvent, contentPadding = padding)
                uiState.photoSheet?.let { sheet ->
                    ProfilePhotoSheet(
                        currentPhoto = uiState.avatarUrl,
                        state = sheet,
                        snackbarHostState = sheetSnackbarHostState,
                        onTakePhoto = onTakePhoto,
                        onPickFromGallery = onPickPhoto,
                        onSave = { onEvent(EditProfileUiEvent.SavePhoto) },
                        onDismiss = { onEvent(EditProfileUiEvent.DismissPhotoSheet) },
                    )
                    sheet.capture?.let { capture ->
                        ProfilePhotoCaptureDialog(
                            state = capture,
                            onEvent = { onEvent(EditProfileUiEvent.PhotoCapture(it)) },
                            onPickFromGallery = onPickPhoto,
                            snackbarHostState = captureSnackbarHostState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileForm(
    state: EditProfileUiState.Success,
    onEvent: (EditProfileUiEvent) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
    ) {
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.lg + spacing.xs) {
            ChangePhotoButton(
                photo = state.avatarUrl,
                onClick = { onEvent(EditProfileUiEvent.ChangePhotoClick) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = stringResource(R.string.account_edit_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                GotestTextField(
                    value = state.name,
                    onValueChange = { onEvent(EditProfileUiEvent.NameChange(it)) },
                    label = stringResource(R.string.account_edit_name_label),
                    placeholder = stringResource(R.string.account_edit_name_hint),
                    leadingIcon = GotestIcons.Linear.User,
                    supportingText = state.nameError?.let { stringResource(it) },
                    isError = state.nameError != null,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                GotestTextField(
                    value = state.email,
                    onValueChange = { onEvent(EditProfileUiEvent.EmailChange(it)) },
                    label = stringResource(R.string.account_edit_email_label),
                    placeholder = stringResource(R.string.account_edit_email_hint),
                    leadingIcon = GotestIcons.Linear.Directbox,
                    supportingText = state.emailError?.let { stringResource(it) },
                    isError = state.emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onEvent(EditProfileUiEvent.Submit) }),
                )
            }
            AccountNote(
                text = stringResource(R.string.account_edit_info),
                icon = GotestIcons.Linear.InfoCircle,
                containerColor = GotestTheme.colors.bgBlueSoft,
                iconColor = GotestTheme.colors.bgBlue,
                textColor = GotestTheme.colors.bgBlueDark,
            )
        }
    }
}

/** 88dp avatar with the "Ubah Foto" label; the whole block opens the photo sheet. */
@Composable
private fun ChangePhotoButton(
    photo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(GotestTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GotestAvatar(model = photo, contentDescription = null, size = GotestTheme.sizes.avatarXl)
        Text(
            text = stringResource(R.string.account_edit_change_photo),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textBrandDark,
        )
    }
}

private val previewSuccess =
    EditProfileUiState.Success(avatarUrl = null, name = "Gilang Permana", email = "gilangpermana@mail.com")

@Composable
private fun EditProfilePreviewScreen(uiState: EditProfileUiState) {
    EditProfileScreen(uiState = uiState, onEvent = {}, onBack = {}, onTakePhoto = {}, onPickPhoto = {})
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun EditProfileScreenPreview() {
    GotestTheme { EditProfilePreviewScreen(previewSuccess) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun EditProfileScreenValidationPreview() {
    GotestTheme {
        EditProfilePreviewScreen(
            previewSuccess.copy(
                name = "",
                email = "gilang@",
                nameError = R.string.account_error_name_required,
                emailError = R.string.account_error_email_invalid,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun EditProfileScreenLoadingPreview() {
    GotestTheme { EditProfilePreviewScreen(EditProfileUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun EditProfileScreenErrorPreview() {
    GotestTheme { EditProfilePreviewScreen(EditProfileUiState.Error) }
}

/** "Ubah Foto" opens the same sheet as Profil; drawn in place because the modal does not preview. */
@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun EditProfileScreenPhotoSheetPreview() {
    GotestTheme {
        Box {
            EditProfilePreviewScreen(previewSuccess)
            Box(modifier = Modifier.fillMaxSize().background(GotestTheme.colors.scrim))
            ProfilePhotoSheetContent(
                currentPhoto = null,
                state = PhotoSheetUiState(selectedPhotoUri = "content://media/picker/0/1"),
                onTakePhoto = {},
                onPickFromGallery = {},
                onSave = {},
                onClose = {},
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(GotestTheme.colors.bgApp, GotestTheme.shapes.bottomSheet)
                        .padding(top = GotestTheme.spacing.xl),
            )
        }
    }
}
