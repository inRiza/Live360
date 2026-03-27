package com.example.nimons360.platform

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
