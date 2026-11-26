package com.gotest.app.feature.auth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.network.ApiError
import java.io.IOException

/** Why a sign-up page's questions could not be read: no connection, the server's sentence, or neither. */
@Immutable
sealed interface InfoProblem {
    data object Offline : InfoProblem

    data class Server(
        val message: String,
    ) : InfoProblem

    data object Unknown : InfoProblem

    companion object {
        fun of(error: Throwable): InfoProblem =
            when {
                error is IOException -> Offline
                error is ApiError && error.status != 0 && error.message.isNotBlank() -> Server(error.message)
                else -> Unknown
            }
    }
}

/** The sign-up form's place while its questions failed to load: why, and "Coba Lagi". */
@Composable
fun InfoProblemView(
    problem: InfoProblem,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestErrorView(
        onRetry = onRetry,
        modifier = modifier.fillMaxWidth(),
        message =
            when (problem) {
                InfoProblem.Offline -> stringResource(R.string.auth_error_offline)
                is InfoProblem.Server -> problem.message
                InfoProblem.Unknown -> stringResource(R.string.state_error_message)
            },
    )
}
