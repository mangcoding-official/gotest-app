package com.gotest.app.feature.account.domain

import javax.inject.Inject

/** Why a profile field is rejected; the UI maps each to a message. */
enum class ProfileFieldError { Required, InvalidEmail }

/** The live checklist under "Ganti Password". */
enum class PasswordRule { MinLength, LettersAndDigits, DifferentFromCurrent }

/** Client-side checks for the account forms. */
class AccountValidator
    @Inject
    constructor() {
        fun validateName(name: String): ProfileFieldError? = if (name.isBlank()) ProfileFieldError.Required else null

        fun validateEmail(email: String): ProfileFieldError? =
            when {
                email.isBlank() -> ProfileFieldError.Required
                !EMAIL_PATTERN.matches(email.trim()) -> ProfileFieldError.InvalidEmail
                else -> null
            }

        fun satisfiedPasswordRules(
            currentPassword: String,
            newPassword: String,
        ): Set<PasswordRule> =
            buildSet {
                if (newPassword.length >= MIN_PASSWORD_LENGTH) add(PasswordRule.MinLength)
                if (newPassword.any(Char::isLetter) && newPassword.any(Char::isDigit)) {
                    add(PasswordRule.LettersAndDigits)
                }
                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && newPassword != currentPassword) {
                    add(PasswordRule.DifferentFromCurrent)
                }
            }

        /** "Hapus Akun Permanen" unlocks only once the student retypes [DELETE_CONFIRMATION]. */
        fun isDeleteConfirmed(input: String): Boolean = input.trim() == DELETE_CONFIRMATION

        companion object {
            const val DELETE_CONFIRMATION = "gotest.id"
            private const val MIN_PASSWORD_LENGTH = 8
            private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }
