package com.coppersmith.music1chat.cast

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
@Composable
fun Music1CastButton(
    modifier: Modifier = Modifier
) {
    MediaRouteButton(
        modifier = modifier
    )
}