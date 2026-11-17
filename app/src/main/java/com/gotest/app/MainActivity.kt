package com.gotest.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.navigation.AppLink
import com.gotest.app.navigation.AppLinks
import com.gotest.app.navigation.GotestApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** A gotest.id link the app was opened with (T-018), until the app shell has opened its screen. */
    private var appLink by mutableStateOf<AppLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: after a rotation the link was already handled.
        if (savedInstanceState == null) appLink = AppLinks.parse(intent?.dataString)
        setContent {
            GotestTheme {
                GotestApp(appLink = appLink, onAppLinkConsume = { appLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLinks.parse(intent.dataString)?.let { appLink = it }
    }
}
