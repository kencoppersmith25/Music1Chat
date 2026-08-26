package com.coppersmith.music1chat.playback

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class MediaButtonCommand {
    TOGGLE_PLAYBACK,
    NEXT_STATION,
    PREVIOUS_STATION,
    NEXT_CATEGORY,
    PREVIOUS_CATEGORY
}

object MediaButtonCommandBus {

    private val mutableCommands =
        MutableSharedFlow<MediaButtonCommand>(
            replay = 0,
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    val commands: SharedFlow<MediaButtonCommand> =
        mutableCommands.asSharedFlow()

    private var lastCommandTimes = mutableMapOf<MediaButtonCommand, Long>()
    private const val DEBOUNCE_MS = 500L // Ignore commands faster than 0.5 seconds

    fun send(command: MediaButtonCommand) {
        val now = System.currentTimeMillis()
        val lastTime = lastCommandTimes[command] ?: 0L

        // Apply "Human Speed Filter" to navigation commands
        if (command != MediaButtonCommand.TOGGLE_PLAYBACK) {
            if (now - lastTime < DEBOUNCE_MS) {
                android.util.Log.w(
                    "CommandBus",
                    "Debounced rapid-fire command: $command (delta: ${now - lastTime}ms)"
                )
                return
            }
        }

        val emitted = mutableCommands.tryEmit(command)
        lastCommandTimes[command] = now

        android.util.Log.d(
            "KenCheck",
            "CommandBus send=$command emitted=$emitted subscribers=${mutableCommands.subscriptionCount.value}"
        )
    }
}