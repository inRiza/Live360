package com.example.nimons360

import android.app.Application
import org.maplibre.android.MapLibre
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Nimons360Application : Application() {

	override fun onCreate() {
		super.onCreate()
		MapLibre.getInstance(this)
	}
}
