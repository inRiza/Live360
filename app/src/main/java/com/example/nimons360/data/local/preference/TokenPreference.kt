package com.example.nimons360.data.local.preference

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TokenPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveToken(token: String) {}

    fun getToken(): String? = null

    fun clear() {}
}
