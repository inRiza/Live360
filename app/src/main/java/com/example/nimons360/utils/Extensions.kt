package com.example.nimons360.utils

fun String.toInitial(): String = firstOrNull()?.uppercaseChar()?.toString() ?: "?"
