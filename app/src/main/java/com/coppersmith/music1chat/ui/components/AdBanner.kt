package com.coppersmith.music1chat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.coppersmith.music1chat.ads.AdConfig
import com.coppersmith.music1chat.ads.AdManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdManager.mayShowBanner()) {
        return
    }

    // Wrap in a Box with a minimum height to prevent layout jumps
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 100.dp) 
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    // Changing from BANNER (320x50) to LARGE_BANNER (320x100)
                    setAdSize(AdSize.LARGE_BANNER)

                    adUnitId = AdConfig.ANDROID_BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
