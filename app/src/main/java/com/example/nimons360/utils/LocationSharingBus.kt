package com.example.nimons360.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object LocationSharingBus {
    private val _events = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val events: SharedFlow<Boolean> = _events.asSharedFlow()

    fun notify(enabled: Boolean) {
        _events.tryEmit(enabled)
    }
}
