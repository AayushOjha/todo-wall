package com.mohannic.taskarma

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Centralised user preference store for Taskarma.
 * Persists: user name, dark mode setting, onboarding completion flag,
 * and the last wallpaper hash.
 */
object UserPreferences {

    private const val PREFS_NAME        = "taskarma_prefs"
    private const val KEY_USER_NAME     = "user_name"
    private const val KEY_DARK_MODE     = "dark_mode"
    private const val KEY_ONBOARDED     = "onboarding_done"
    private const val KEY_WALLPAPER_HASH= "last_wallpaper_hash"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Name ────────────────────────────────────────────────────────────────

    fun getUserName(context: Context): String =
        prefs(context).getString(KEY_USER_NAME, "") ?: ""

    fun setUserName(context: Context, name: String) =
        prefs(context).edit { putString(KEY_USER_NAME, name) }

    // ── Dark Mode ────────────────────────────────────────────────────────────

    /** Returns null if user has never chosen (will follow system). */
    fun isDarkMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, true)

    fun setDarkMode(context: Context, dark: Boolean) =
        prefs(context).edit { putBoolean(KEY_DARK_MODE, dark) }

    // ── Onboarding ───────────────────────────────────────────────────────────

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboardingDone(context: Context) =
        prefs(context).edit { putBoolean(KEY_ONBOARDED, true) }

    // ── Wallpaper Hash ───────────────────────────────────────────────────────

    fun getLastWallpaperHash(context: Context): Int =
        prefs(context).getInt(KEY_WALLPAPER_HASH, 0)

    fun setLastWallpaperHash(context: Context, hash: Int) =
        prefs(context).edit { putInt(KEY_WALLPAPER_HASH, hash) }
}
