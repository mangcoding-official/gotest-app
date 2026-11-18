package com.gotest.app.feature.account.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Opens [url] (e.g. the privacy policy) in a browser. Returns false when no installed app can handle it. */
fun Context.openWebLink(url: String): Boolean =
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }.isSuccess
