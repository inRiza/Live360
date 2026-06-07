package com.example.nimons360.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "nimons360_prefs"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATION_ENABLED, enabled) }
    }
}
