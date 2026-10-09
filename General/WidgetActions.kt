package com.coppersmith.music1chat.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.core.content.ContextCompat

class TogglePlayAction : ActionCardCallback { // or ActionCallback
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, com.coppersmith.music1chat.playback.PlaybackService::class.java).apply {
            action = "com.coppersmith.music1chat.ACTION_WIDGET_TOGGLE"
        }
        ContextCompat.startForegroundService(context, intent)
    }
}

class PrevCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, com.coppersmith.music1chat.playback.PlaybackService::class.java).apply {
            action = "com.coppersmith.music1chat.ACTION_WIDGET_PREV_CAT"
        }
        context.startForegroundService(intent)
    }
}

class PrevStationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, com.coppersmith.music1chat.playback.PlaybackService::class.java).apply {
            action = "com.coppersmith.music1chat.ACTION_WIDGET_PREV_STATION"
        }
        context.startForegroundService(intent)
    }
}

class NextStationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, com.coppersmith.music1chat.playback.PlaybackService::class.java).apply {
            action = "com.coppersmith.music1chat.ACTION_WIDGET_NEXT_STATION"
        }
        context.startForegroundService(intent)
    }
}

class NextCategoryAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, com.coppersmith.music1chat.playback.PlaybackService::class.java).apply {
            action = "com.coppersmith.music1chat.ACTION_WIDGET_NEXT_CAT"
        }
        context.startForegroundService(intent)
    }
}