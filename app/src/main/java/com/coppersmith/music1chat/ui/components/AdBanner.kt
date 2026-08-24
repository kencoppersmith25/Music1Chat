package com.coppersmith.music1chat.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Use the standard Google Test Banner ID
                setAdSize(AdSize.BANNER)

                adUnitId = "ca-app-pub-6232643827829257/2486278674"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
