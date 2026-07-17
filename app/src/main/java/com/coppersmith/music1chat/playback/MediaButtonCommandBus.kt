package com.coppersmith.music1chat.playback

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class MediaButtonCommand {
    TOGGLE_PLAYBACK,
    NEXT_STATION,
    NEXT_CATEGORY
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

    fun send(command: MediaButtonCommand) {
        mutableCommands.tryEmit(command)
    }
}