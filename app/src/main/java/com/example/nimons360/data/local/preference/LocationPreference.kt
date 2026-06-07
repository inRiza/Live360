package com.example.nimons360.data.local.preference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocationPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    fun setLocationSharingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_LOCATION, enabled).apply()
    fun isLocationSharingEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION, true)

    companion object {
        private const val KEY_LOCATION = "location_sharing_enabled"
    }
}