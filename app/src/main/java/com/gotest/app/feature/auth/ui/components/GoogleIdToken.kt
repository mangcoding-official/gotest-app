package com.gotest.app.feature.auth.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.gotest.app.BuildConfig
import kotlinx.coroutines.launch

/** What Google's account picker gave: the ID token for `POST /auth/google`, nothing (closed), or a failure. */
sealed interface GoogleIdTokenResult {
    data class Token(
        val value: String,
    ) : GoogleIdTokenResult

    data object Cancelled : GoogleIdTokenResult

    data object Failed : GoogleIdTokenResult
}

/**
 * Opens Google's account picker (Credential Manager, "Sign in with Google") from the button of Masuk or Daftar
 * and hands back the ID token, meant for the server's Web client ID (T-026 A2a). Debug without the API skips
 * the picker: the fake repositories take any token.
 */
@Composable
fun rememberGoogleIdToken(onResult: (GoogleIdTokenResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    return {
        scope.launch { currentOnResult(if (BuildConfig.USE_API) requestGoogleIdToken(context) else FAKE_TOKEN) }
    }
}

private suspend fun requestGoogleIdToken(context: Context): GoogleIdTokenResult {
    val request =
        GetCredentialRequest
            .Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID).build())
            .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleIdTokenResult.Failed
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleIdTokenResult.Cancelled
    } catch (_: NoCredentialException) {
        // No Google account the picker could offer on this phone.
        GoogleIdTokenResult.Failed
    } catch (_: GetCredentialException) {
        GoogleIdTokenResult.Failed
    } catch (_: GoogleIdTokenParsingException) {
        GoogleIdTokenResult.Failed
    }
}

private val FAKE_TOKEN = GoogleIdTokenResult.Token("fake-google-id-token")
