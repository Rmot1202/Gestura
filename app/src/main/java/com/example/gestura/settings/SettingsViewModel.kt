package com.example.gestura.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Safe placeholders until your DB is connected.
 * Later, call setStats(contrib, acc) with real values from Firestore/API.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("gestura_settings", Context.MODE_PRIVATE)

    private val _language = MutableLiveData(prefs.getString("language", "en") ?: "en")
    val language: LiveData<String> = _language

    private val _theme = MutableLiveData(prefs.getString("theme", "auto") ?: "auto") // light/dark/auto
    val theme: LiveData<String> = _theme

    private val _notifications = MutableLiveData(prefs.getBoolean("notifications", true))
    val notifications: LiveData<Boolean> = _notifications

    private val _autoUpdate = MutableLiveData(prefs.getBoolean("autoUpdate", true))
    val autoUpdate: LiveData<Boolean> = _autoUpdate

    private val _maskSync = MutableLiveData(prefs.getBoolean("maskSync", true))
    val maskSync: LiveData<Boolean> = _maskSync

    // ---- Stats (null => no DB yet) ----
    private val _totalContrib = MutableLiveData<Int?>(null)
    val totalContrib: LiveData<Int?> = _totalContrib

    private val _accuracy = MutableLiveData<Int?>(null) // %
    val accuracy: LiveData<Int?> = _accuracy

    private val _devMode = MutableLiveData(false)
    val devMode: LiveData<Boolean> = _devMode

    fun setLanguage(v: String) { _language.value = v; prefs.edit().putString("language", v).apply() }
    fun setTheme(v: String) { _theme.value = v; prefs.edit().putString("theme", v).apply() }
    fun setNotifications(v: Boolean) { _notifications.value = v; prefs.edit().putBoolean("notifications", v).apply() }
    fun setAutoUpdate(v: Boolean) { _autoUpdate.value = v; prefs.edit().putBoolean("autoUpdate", v).apply() }
    fun setMaskSync(v: Boolean) { _maskSync.value = v; prefs.edit().putBoolean("maskSync", v).apply() }

    /**
     * Toggle dev mode only if we have real stats and user qualifies.
     * Qualify rule: ≥47 contributions and ≥90% accuracy.
     */
    fun tryToggleDevMode() {
        val c = _totalContrib.value
        val a = _accuracy.value
        val hasData = (c != null && a != null)
        val qualifies = hasData && c!! >= 47 && a!! >= 90
        if (qualifies) _devMode.value = !(_devMode.value ?: false)
    }

    // When you connect DB later, populate real values here.
    fun setStats(contrib: Int?, acc: Int?) {
        _totalContrib.value = contrib
        _accuracy.value = acc
    }
}
