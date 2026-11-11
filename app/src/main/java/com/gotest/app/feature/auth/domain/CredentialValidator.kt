package com.gotest.app.feature.auth.domain

import javax.inject.Inject

/** Why a credential field is rejected; the UI maps each to a message. */
enum class CredentialError { Required, InvalidEmail, PasswordTooShort, PasswordMismatch }

/** Client-side checks shared by the sign-in, register and reset forms. */
class CredentialValidator
    @Inject
    constructor() {
        fun validateEmail(email: String): CredentialError? =
            when {
                email.isBlank() -> CredentialError.Required
                !EMAIL_PATTERN.matches(email.trim()) -> CredentialError.InvalidEmail
                else -> null
            }

        fun validatePassword(password: String): CredentialError? =
            when {
                password.isEmpty() -> CredentialError.Required
                password.length < MIN_PASSWORD_LENGTH -> CredentialError.PasswordTooShort
                else -> null
            }

        fun validateRequired(value: String): CredentialError? = if (value.isBlank()) CredentialError.Required else null

        fun validateConfirmation(
            password: String,
            confirmation: String,
        ): CredentialError? =
            when {
                confirmation.isEmpty() -> CredentialError.Required
                confirmation != password -> CredentialError.PasswordMismatch
                else -> null
            }

        private companion object {
            const val MIN_PASSWORD_LENGTH = 8
            val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }
