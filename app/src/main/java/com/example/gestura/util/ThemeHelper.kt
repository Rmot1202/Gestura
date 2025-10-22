package com.example.gestura.util

import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    /** mode: "light" | "dark" | "auto" */
    fun apply(mode: String) {
        val nightMode = when (mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
            else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        // AppCompat will recreate activities as needed to apply the change.
    }
}
