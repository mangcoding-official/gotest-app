package com.gotest.app.feature.account.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpContactType

/**
 * Opens the mail app (`ACTION_SENDTO mailto:`) or WhatsApp (`ACTION_VIEW https://wa.me/…`) for
 * [contact]. Returns false when no installed app can handle it.
 */
fun Context.openHelpContact(contact: HelpContact): Boolean {
    val action =
        when (contact.type) {
            HelpContactType.Email -> Intent.ACTION_SENDTO
            HelpContactType.WhatsApp -> Intent.ACTION_VIEW
        }
    return runCatching { startActivity(Intent(action, contact.uri.toUri())) }.isSuccess
}

/** Opens [url] (e.g. the privacy policy) in a browser. Returns false when no installed app can handle it. */
fun Context.openWebLink(url: String): Boolean =
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }.isSuccess
