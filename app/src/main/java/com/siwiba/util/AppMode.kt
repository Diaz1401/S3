package com.siwiba.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

/**
 * A utility class for managing the application mode (theme) settings.
 *
 * @property context The context of the current activity.
 */
class AppMode(private val context: AppCompatActivity) {

    /**
     * Retrieves the current application mode from shared preferences.
     *
     * @return `true` if the app mode is WBA, `false` if the app mode is KWI.
     */
    fun getAppMode(): Boolean {
        val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("appMode", true)
    }

    /**
     * Sets the application mode and saves it to shared preferences.
     *
     * @param appMode The new application mode to set. `true` for WBA, `false` for KWI.
     */
    fun setAppMode(appMode: Boolean) {
        val sharedPref: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("appMode", appMode)
            apply()
        }
        context.recreate()
    }
}