package com.example.nimons360

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Nimons360Application : Application() {

	override fun onCreate() {
		super.onCreate()
		initializeMapSdkCompat()
	}

	private fun initializeMapSdkCompat() {
		// MapLibre 9.x can still expose Mapbox-compatible package names.
		runCatching {
			val mapLibreClass = Class.forName("org.maplibre.android.MapLibre")
			val mapLibreInit = mapLibreClass.methods.firstOrNull {
				it.name == "getInstance" &&
					it.parameterTypes.size == 1 &&
					it.parameterTypes[0] == Application::class.java
			}
			if (mapLibreInit != null) {
				mapLibreInit.invoke(null, this)
				return
			}
		}

		runCatching {
			val mapboxClass = Class.forName("com.mapbox.mapboxsdk.Mapbox")
			val twoArgs = mapboxClass.methods.firstOrNull {
				it.name == "getInstance" &&
					it.parameterTypes.size == 2
			}
			val oneArg = mapboxClass.methods.firstOrNull {
				it.name == "getInstance" &&
					it.parameterTypes.size == 1
			}

			when {
				twoArgs != null -> twoArgs.invoke(null, this, "maplibre-local")
				oneArg != null -> oneArg.invoke(null, this)
				else -> Unit
			}
		}.onFailure {
			Log.w("Nimons360", "Map SDK init skipped: ${it.message}")
		}
	}
}
