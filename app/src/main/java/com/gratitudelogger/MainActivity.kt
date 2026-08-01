package com.gratitudelogger

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.gratitudelogger.data.backup.OAuthRedirectRelay
import com.gratitudelogger.theme.AppTheme
import com.gratitudelogger.theme.ThemePreferences
import com.gratitudelogger.ui.AppRoot
import com.gratitudelogger.ui.theme.GratitudeLoggerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var oauthRedirectRelay: OAuthRedirectRelay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by themePreferences.selectedTheme.collectAsState(initial = AppTheme.SUNSET_GOLD)
            GratitudeLoggerTheme(appTheme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    // Dropbox's OAuth redirect (gratitudelogger://oauth2redirect) arrives here, not as an
    // ActivityResult - MainActivity is launchMode="singleTop" so this fires on the existing
    // instance instead of creating a second one.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            if (uri.scheme == "gratitudelogger" && uri.host == "oauth2redirect") {
                oauthRedirectRelay.emit(uri)
            }
        }
    }
}
