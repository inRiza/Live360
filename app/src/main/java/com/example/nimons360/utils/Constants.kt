package com.example.nimons360.utils

object Constants {
    const val BASE_URL = "https://mad.labpro.hmif.dev"
    const val WS_URL = "wss://mad.labpro.hmif.dev/ws/live"
    val FAMILY_ICONS = (1..8).map { 
        "$BASE_URL/assets/family_icon_$it.png" 
    }
}
