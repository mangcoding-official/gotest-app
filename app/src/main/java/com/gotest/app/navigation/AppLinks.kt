package com.gotest.app.navigation

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/** A gotest.id link the app opens itself (T-018), with the same addresses as the web. */
sealed interface AppLink {
    /** `https://gotest.id/reset-password/{token}?email=…`, from the reset email. */
    data class ResetPassword(
        val token: String,
        val email: String?,
    ) : AppLink

    /** `https://gotest.id/gabung/{kode}`: a class code or an event code (Gabung ke kelas). */
    data class JoinCode(
        val code: String,
    ) : AppLink

    /** `https://gotest.id/undangan/{token}`: a Mitra invitation (T-026 A2b). */
    data class Invitation(
        val token: String,
    ) : AppLink

    /** `https://gotest.id/main-bareng?kode=XXXXXX`: a Main Bareng game code. */
    data class MainBarengCode(
        val code: String,
    ) : AppLink
}

object AppLinks {
    private const val HOST = "gotest.id"
    private val gameCode = Regex("^[A-Z0-9]{6}$")

    /** Null for anything that is not one of the app's links, so the app just opens normally. */
    fun parse(link: String?): AppLink? =
        link
            ?.let(::toUri)
            ?.takeIf { it.scheme == "https" && it.host?.lowercase() == HOST }
            ?.let(::fromUri)

    private fun toUri(link: String): URI? =
        try {
            URI(link)
        } catch (_: URISyntaxException) {
            null
        }

    private fun fromUri(uri: URI): AppLink? {
        val segments =
            uri.rawPath
                .orEmpty()
                .split('/')
                .filter { it.isNotEmpty() }
                .map(::decode)
        val query = query(uri.rawQuery)
        return when {
            segments.size == 2 && segments[0] == "reset-password" && segments[1].isNotBlank() -> {
                AppLink.ResetPassword(token = segments[1], email = query["email"]?.takeIf { it.isNotBlank() })
            }

            segments.size == 2 && segments[0] == "gabung" && segments[1].isNotBlank() -> {
                AppLink.JoinCode(segments[1].trim().uppercase())
            }

            segments.size == 2 && segments[0] == "undangan" && segments[1].isNotBlank() -> {
                AppLink.Invitation(segments[1])
            }

            segments == listOf("main-bareng") -> {
                // The web opens the code only when it has the right shape (MainBarengController::hubProps).
                query["kode"]
                    ?.trim()
                    ?.uppercase()
                    ?.takeIf(gameCode::matches)
                    ?.let(AppLink::MainBarengCode)
            }

            else -> {
                null
            }
        }
    }

    private fun query(raw: String?): Map<String, String> =
        raw
            .orEmpty()
            .split('&')
            .filter { it.isNotEmpty() }
            .associate { part ->
                val name = part.substringBefore('=')
                decode(name) to decode(part.substringAfter('=', ""))
            }

    private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
}
