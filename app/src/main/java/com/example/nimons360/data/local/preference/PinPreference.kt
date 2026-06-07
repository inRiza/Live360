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
        private const val PREFIX_DOWNLOADED = "downloaded_filename_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Helper untuk mengonstruksi File
    private fun getSafeFile(filename: String?): File? {
        if (filename.isNullOrBlank()) return null
        val safeFile = File(context.filesDir, filename)
        return if (safeFile.exists()) safeFile else null
    }

    fun saveCustomPinBiasaFilename(filename: String?) {
        prefs.edit { putString(KEY_PIN_BIASA, filename) }
    }

    fun getCustomPinBiasaPath(): String? {
        val filename = prefs.getString(KEY_PIN_BIASA, null)
        return getSafeFile(filename)?.absolutePath
    }

    fun saveCustomPinFavoriteFilename(filename: String?) {
        prefs.edit { putString(KEY_PIN_FAVORITE, filename) }
    }

    fun getCustomPinFavoritePath(): String? {
        val filename = prefs.getString(KEY_PIN_FAVORITE, null)
        return getSafeFile(filename)?.absolutePath
    }

    fun saveDownloadedPinFilename(id: String, filename: String) {
        prefs.edit { putString(PREFIX_DOWNLOADED + id, filename) }
    }

    fun getDownloadedPinPath(id: String): String? {
        val filename = prefs.getString(PREFIX_DOWNLOADED + id, null)
        return getSafeFile(filename)?.absolutePath
    }

    fun isPinDownloaded(id: String): Boolean {
        return getDownloadedPinPath(id) != null
    }
}