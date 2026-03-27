package com.example.nimons360.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrientationSensorManager(private val context: Context) {
    val azimuth: StateFlow<Float> = MutableStateFlow(0f)
    fun start() {}
    fun stop() {}
}
