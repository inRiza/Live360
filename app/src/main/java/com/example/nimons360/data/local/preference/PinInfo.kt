package com.example.nimons360.data.local.preference

data class PinInfo(
    val id: String,
    val name: String,
    val url: String,
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0
)
