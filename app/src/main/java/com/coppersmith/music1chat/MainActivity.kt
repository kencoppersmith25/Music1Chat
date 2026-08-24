package com.coppersmith.music1chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.coppersmith.music1chat.ui.screens.MainScreen
import com.coppersmith.music1chat.ui.theme.Music1ChatTheme
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import com.coppersmith.music1chat.ads.AdManager
import com.google.android.gms.ads.MobileAds
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        val powerManager =
            getSystemService(PowerManager::class.java)

        AdManager.initialize(
            application = application,
            appInForegroundProvider = {
                lifecycle.currentState.isAtLeast(
                    Lifecycle.State.RESUMED
                )
            },
            screenInteractiveProvider = {
                powerManager.isInteractive
            },
            playbackRequestedProvider = {
                // Temporary until we connect this to PlaybackService.
                true
            }
        )

        setContent {
            Music1ChatTheme {
                MainScreen()
            }
        }
    }
}
