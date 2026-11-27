package com.danrey.lalasi.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** A refresh done on one page (Library) tells Home to reload its songs too. */
object AppRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun request() {
        _events.tryEmit(Unit)
    }
}
