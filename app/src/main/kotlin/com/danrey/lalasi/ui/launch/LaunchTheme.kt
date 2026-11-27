package com.danrey.lalasi.ui.launch

import android.content.Context
import androidx.core.content.edit

/**
 * The app theme as the launch screens need it. The app's light/dark choice lives in DataStore, which cannot be
 * read synchronously before the first frame, so the choice is mirrored here (SharedPreferences, read once per
 * process). Both the Compose loading overlay and the system splash theme (AppIcons.syncSplash) read this one
 * value, so they always agree: light theme -> light background, dark logo and dark pen; dark theme -> as before.
 */
object LaunchTheme {
    private const val Prefs = "launch_theme"
    private const val KeyLight = "light"

    @Volatile
    private var light: Boolean? = null

    /** true when the app is on the light theme (a fresh install starts dark, like the app's default) */
    fun isLight(context: Context): Boolean {
        light?.let { return it }
        return context.applicationContext
            .getSharedPreferences(Prefs, Context.MODE_PRIVATE)
            .getBoolean(KeyLight, false)
            .also { light = it }
    }

    /** remembers the theme for the next launch; returns true when it changed */
    fun save(context: Context, isLight: Boolean): Boolean {
        val changed = isLight(context) != isLight
        if (changed) {
            light = isLight
            context.applicationContext
                .getSharedPreferences(Prefs, Context.MODE_PRIVATE)
                .edit { putBoolean(KeyLight, isLight) }
        }
        return changed
    }
}
