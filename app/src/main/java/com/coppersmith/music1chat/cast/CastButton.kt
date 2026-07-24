package com.coppersmith.music1chat.cast

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.coppersmith.music1chat.R
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import androidx.mediarouter.media.MediaRouteSelector

@Composable
fun Music1CastButton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(48.dp), // Match standard IconButton size
        contentAlignment = Alignment.Center
    ) {
        // 1. The visual icon (Matches Settings/Power icons)
        Icon(
            imageVector = Icons.Default.Cast,
            contentDescription = "Cast",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(30.dp)
        )

        // 2. The invisible interactive layer
        AndroidView(
            factory = { context ->
                // Ensure CastContext is available
                try { CastContext.getSharedInstance(context) } catch (e: Exception) {}

                val themedContext = ContextThemeWrapper(context, R.style.Theme_Music1Chat)
                MediaRouteButton(themedContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    val selector = MediaRouteSelector.Builder()
                        .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                        .build()
                    setRouteSelector(selector)
                    setAlwaysVisible(true)
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                }
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(0.01f) // Nearly invisible but clickable
        )
    }
}
