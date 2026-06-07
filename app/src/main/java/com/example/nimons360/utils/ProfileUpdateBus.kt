package com.example.nimons360.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ProfileUpdateBus {
    private val _events = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val events: SharedFlow<Long> = _events.asSharedFlow()

    fun notifyProfileUpdated() {
        _events.tryEmit(System.currentTimeMillis())
    }
}
