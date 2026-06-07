package com.example.nimons360.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "nimons360_pin_prefs"
        private const val KEY_PIN_BIASA = "custom_pin_biasa"
        private const val KEY_PIN_FAVORITE = "custom_pin_favorite"
        private const val PREFIX_DOWNLOADED = "downloaded_path_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCustomPinBiasaPath(path: String?) {
        prefs.edit { putString(KEY_PIN_BIASA, path) }
    }

    fun getCustomPinBiasaPath(): String? {
        val path = prefs.getString(KEY_PIN_BIASA, null)
        return if (path != null && File(path).exists()) path else null
    }

    fun saveCustomPinFavoritePath(path: String?) {
        prefs.edit { putString(KEY_PIN_FAVORITE, path) }
    }

    fun getCustomPinFavoritePath(): String? {
        val path = prefs.getString(KEY_PIN_FAVORITE, null)
        return if (path != null && File(path).exists()) path else null
    }

    fun saveDownloadedPin(id: String, path: String) {
        prefs.edit { putString(PREFIX_DOWNLOADED + id, path) }
    }

    fun getDownloadedPinPath(id: String): String? {
        val path = prefs.getString(PREFIX_DOWNLOADED + id, null)
        return if (path != null && File(path).exists()) path else null
    }

    fun isPinDownloaded(id: String): Boolean {
        return getDownloadedPinPath(id) != null
    }
}
